package com.songdosamgyeop.order.ui.hq.orders

import android.net.Uri
import android.os.Bundle
import android.view.*
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
        private const val KEY_ORDER_UPDATED = "KEY_ORDER_UPDATED" // 상세→목록 변경 신호 키
        private const val KEY_INIT_FILTER   = "KEY_INIT_FILTER"   // 홈→초기 필터 전달 키
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

        // RecyclerView
        adapter = HqOrdersAdapter { orderId ->
            findNavController().navigate(
                R.id.action_hqOrders_to_hqOrderDetail,
                bundleOf("orderId" to orderId)
            )
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        // ── 탭 분리: 진행 중 / 완료 ───────────────────────────────────────
        // TabLayout이 레이아웃에 존재한다고 가정 (없다면 TabLayout 추가 필요)
        b.tabLayout.apply {
            if (tabCount == 0) {
                addTab(newTab().setText("진행 중").setTag("inProgress"))
                addTab(newTab().setText("완료").setTag("completed"))
            }
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    (tab.tag as? String)?.let(vm::setTab) // "inProgress" | "completed"
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {
                    // 탭 재선택 시 새로고침 UX
                    vm.refresh()
                }
            })
        }

        // ViewModel의 현재 탭 상태를 UI에 반영 (SavedStateHandle로 초기화될 수 있음)
        vm.tab.observe(viewLifecycleOwner) { t ->
            val idx = if (t == "completed") 1 else 0
            if (b.tabLayout.selectedTabPosition != idx) {
                b.tabLayout.getTabAt(idx)?.select()
            }
        }

        // ── 지사명 검색 ─────────────────────────────────────────────────
        b.etBranchName.doOnTextChanged { text, _, _, _ ->
            vm.setBranchNameQuery(text?.toString())
        }

        // ── 기간 선택 (endExclusive: 다음날 00:00) ──────────────────────
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
            b.btnPickDate.setText(R.string.common_select_date_range)
        }

        // ── 메뉴 (CSV 내보내기) ────────────────────────────────────────
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

        // ── 리스트 구독 + 합계 푸터 ─────────────────────────────────────
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

        // ✅ 상세→목록 복귀 시 변경 신호 수신 → 재조회
        val handle = findNavController().currentBackStackEntry?.savedStateHandle
        handle?.getLiveData<Boolean>(KEY_ORDER_UPDATED)
            ?.observe(viewLifecycleOwner) { changed ->
                if (changed == true) {
                    vm.refresh()
                    handle.remove<Boolean>(KEY_ORDER_UPDATED)
                }
            }

        // ✅ 홈→초기 필터(탭/상태/기간 등) 수신
        val fromHome = findNavController().previousBackStackEntry?.savedStateHandle
        fromHome?.getLiveData<Bundle>(KEY_INIT_FILTER)
            ?.observe(viewLifecycleOwner) { payload ->
                if (payload.getString("screen") == "orders") {
                    payload.getString("tab")?.let(vm::setTab) // "inProgress"|"completed"
                    payload.getString("branchQuery")?.let(vm::setBranchNameQuery)
                    @Suppress("DEPRECATION")
                    vm.setDateRange(
                        payload.getParcelable("dateStart"),
                        payload.getParcelable("dateEnd")
                    )
                }
                fromHome.remove<Bundle>(KEY_INIT_FILTER)
            }

        // 당겨서 새로고침
        b.swipe.setOnRefreshListener {
            vm.refresh()
            b.swipe.isRefreshing = false
        }
    }

    /** 현재 어댑터 리스트를 CSV로 저장 */
    private fun exportCsv(uri: Uri) {
        val rows = adapter.currentList.toList()
        val resolver = requireContext().contentResolver

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                resolver.openOutputStream(uri)?.use { os ->
                    // UTF-8 BOM (엑셀 호환)
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
                            // 🐞 FIX: 지사명 fallback 중복 → branchId로 보정
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