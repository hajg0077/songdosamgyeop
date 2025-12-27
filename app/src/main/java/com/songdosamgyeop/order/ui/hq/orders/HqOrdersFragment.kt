package com.songdosamgyeop.order.ui.hq.orders

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrdersBinding
import com.songdosamgyeop.order.ui.common.NavKeys
import dagger.hilt.android.AndroidEntryPoint
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HqOrdersFragment : Fragment(R.layout.fragment_hq_orders) {

    companion object {
        private const val KEY_ORDER_UPDATED = "KEY_ORDER_UPDATED"
        private const val KEY_ORDERS_FILTER_STATUS = "KEY_ORDERS_FILTER_STATUS"
    }

    private val vm: HqOrdersViewModel by viewModels()
    private lateinit var b: FragmentHqOrdersBinding
    private lateinit var adapter: HqOrdersAdapter

    // SAF - CSV 생성기
    private val createCsv = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        exportCsv(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentHqOrdersBinding.bind(view)
        val currentHandle  = findNavController().currentBackStackEntry?.savedStateHandle

        currentHandle?.getLiveData<String>(KEY_ORDERS_FILTER_STATUS)
            ?.observe(viewLifecycleOwner) { statusStr ->
                val s = statusStr ?: return@observe

                // ✅ 여기서 VM에 필터 적용 (네 VM 함수명에 맞게)
                vm.setSingleStatusFilter(s)   // <- 너 프로젝트에 맞게 바꿔줘

                // ✅ 1회 적용 후 제거 (중복 적용/재진입 방지)
                currentHandle.remove<String>(KEY_ORDERS_FILTER_STATUS)
            }

        // RecyclerView
        adapter = HqOrdersAdapter { orderId ->
            findNavController().navigate(
                R.id.action_hqOrders_to_hqOrderDetail,
                bundleOf("orderId" to orderId)
            )
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        // ── 탭: 진행/완료 ──
        b.tabLayout.apply {
            if (tabCount == 0) {
                addTab(newTab().setText("진행 중").setTag("inProgress"))
                addTab(newTab().setText("완료").setTag("completed"))
            }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    (tab.tag as? String)?.let(vm::setTab)
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {
                    // ✅ refresh 대체: 현재 필터로 재조회 트리거
                    refreshList()
                }
            })
        }

        // VM의 현재 탭 → UI 반영
        vm.tab.observe(viewLifecycleOwner) { t ->
            val idx = if (t == "completed") 1 else 0
            if (b.tabLayout.selectedTabPosition != idx) {
                b.tabLayout.getTabAt(idx)?.select()
            }
        }

        // ── 지사명 검색 ──
        b.etBranchName.doOnTextChanged { text, _, _, _ ->
            vm.setBranchQuery(text.toString())
        }

        // ── 기간 선택(endExclusive: 다음날 00:00) ──
        b.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("조회 기간 선택")
                .build()
            picker.addOnPositiveButtonClickListener { range ->
                val start = range.first ?: return@addOnPositiveButtonClickListener
                val end   = range.second ?: return@addOnPositiveButtonClickListener

                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = end
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                vm.setDateRange(
                    Timestamp(Date(start)),
                    Timestamp(Date(cal.timeInMillis))
                )
                b.btnPickDate.text = picker.headerText
            }
            picker.show(parentFragmentManager, "hqDateRange")
        }
        b.btnClearDate.setOnClickListener {
            vm.setDateRange(null, null)
            // ✅ 존재하는 문자열로 대체(리소스 없어서 터지던 부분)
            b.btnPickDate.setText(R.string.select_requested_date)
        }

        // ── 메뉴(CSV 내보내기) ──
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_hq_orders, menu)
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_export_csv -> {
                        val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA)
                            .apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }
                            .format(System.currentTimeMillis())
                        createCsv.launch("hq_orders_$ts.csv")
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.STARTED)

        // ── 리스트 구독 + 합계 푸터 ──
        vm.displayList.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            b.tvEmpty.visibility = if (rows.isNullOrEmpty()) View.VISIBLE else View.GONE

            if (rows.isNullOrEmpty()) {
                b.tvFooter.visibility = View.GONE
            } else {
                val totalCount = rows.size
                val totalAmount = rows.sumOf { it.totalAmount ?: 0L }
                val nf = NumberFormat.getNumberInstance(Locale.KOREA)
                b.tvFooter.text = "합계: ${totalCount}건 / ${nf.format(totalAmount)}원"
                b.tvFooter.visibility = View.VISIBLE
            }
        }

        // 상세→목록 변경 신호 수신 → 재조회
        currentHandle?.getLiveData<Boolean>(KEY_ORDER_UPDATED)
            ?.observe(viewLifecycleOwner) { changed ->
                if (changed == true) {
                    refreshList()                   // ✅ refresh 대체
                    currentHandle.remove<Boolean>(KEY_ORDER_UPDATED)
                }
            }

        // 당겨서 새로고침
        b.swipe.setOnRefreshListener {
            refreshList()                           // ✅ refresh 대체
            b.swipe.isRefreshing = false
        }

        val fromHome = findNavController().previousBackStackEntry?.savedStateHandle
        fromHome?.getLiveData<Bundle>(NavKeys.INIT_FILTER)
            ?.observe(viewLifecycleOwner) { payload: Bundle ->
                if (payload.getString("screen") == "orders") {

                    // 🔹 상태 필터 (PENDING / APPROVED 등)
                    payload.getString("status")?.let { status ->
                        vm.setSingleStatusFilter(status)
                    }
                }

                // ✅ 반드시 제거 (재진입/뒤로가기 중복 방지)
                fromHome.remove<Bundle>(NavKeys.INIT_FILTER)
            }
    }

    /** 현재 필터값을 그대로 다시 적용하여 재조회 트리거 */
    private fun refreshList() {
        // setDateRange는 내부에서 resetAndLoad()를 호출하므로 재조회에 적합
        vm.setDateRange(vm.dateStart.value, vm.dateEnd.value)
    }

    /** 현재 어댑터 리스트를 CSV로 저장 */
    private fun exportCsv(uri: Uri) {
        val rows = adapter.currentList.toList()
        val resolver = requireContext().contentResolver

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                resolver.openOutputStream(uri)?.use { os ->
                    // UTF-8 BOM
                    os.write(0xEF); os.write(0xBB); os.write(0xBF)

                    OutputStreamWriter(os, Charsets.UTF_8).use { w ->
                        // 헤더
                        w.appendLine("주문일시,브랜드,지사,상태,상품개수,합계,주문ID")

                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
                            .apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }

                        for (r in rows) {
                            val whenStr = r.placedAt?.let(sdf::format)
                                ?: r.createdAt?.let(sdf::format) ?: "-"

                            val brand  = r.brandId ?: "-"
                            val branch = r.branchName ?: r.branchId ?: "-"
                            val status = r.status ?: "-"
                            val count  = r.itemsCount?.toString() ?: "-"
                            val total  = r.totalAmount?.toString() ?: "-"
                            val id     = r.id ?: "-"

                            w.appendLine(
                                listOf(whenStr, brand, branch, status, count, total, id)
                                    .joinToCsv()
                            )
                        }
                    }
                } ?: error("OutputStream is null")

                withContext(Dispatchers.Main) {
                    Snackbar.make(requireView(), getString(R.string.msg_export_success), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.msg_export_failed, e.message ?: ""),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // --- CSV 유틸 ---
    private fun List<String>.joinToCsv(): String = buildString {
        this@joinToCsv.forEachIndexed { i, raw ->
            if (i > 0) append(',')
            append(raw.escapeCsv())
        }
    }
    private fun String.escapeCsv(): String {
        val needsQuote = contains(',') || contains('"') || contains('\n') || contains('\r')
        val s = replace("\"", "\"\"")
        return if (needsQuote) "\"$s\"" else s
    }
}