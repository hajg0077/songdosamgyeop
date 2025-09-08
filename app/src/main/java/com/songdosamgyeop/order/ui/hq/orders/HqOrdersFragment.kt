package com.songdosamgyeop.order.ui.hq.orders

import HqOrdersViewModel
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
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrdersBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.OutputStreamWriter
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

        val nav = findNavController()
        adapter = HqOrdersAdapter { orderId ->
            nav.navigate(
                R.id.action_hqOrders_to_hqOrderDetail,
                bundleOf("orderId" to orderId)
            )
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        // 상태 칩
        b.chipPlaced.setOnClickListener { vm.setStatus("PLACED") }
        b.chipDraft.setOnClickListener  { vm.setStatus("DRAFT") }   // 향후 대비

        // 지사명 검색
        b.etBranchName.doOnTextChanged { text, _, _, _ ->
            vm.setBranchNameQuery(text?.toString())
        }

        // 기간 선택
        b.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("조회 기간 선택")
                .build()
            picker.addOnPositiveButtonClickListener { range ->
                val start = range.first ?: return@addOnPositiveButtonClickListener
                val end   = range.second ?: return@addOnPositiveButtonClickListener

                // endExclusive = 종료일 다음날 00:00 (UTC)
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
            }
            picker.show(parentFragmentManager, "hqDateRange")
        }
        b.btnClearDate.setOnClickListener { vm.setDateRange(null, null) }

        // 메뉴 (CSV 내보내기)
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_hq_orders, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
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

        // 리스트 구독
        vm.displayList.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            b.tvEmpty.visibility = if (rows.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // ✅ 상세에서 돌아올 때 "업데이트됨" 신호 수신 → 목록 재조회
        val navController = findNavController()
        val handle = navController.currentBackStackEntry?.savedStateHandle
        handle?.getLiveData<Boolean>(KEY_ORDER_UPDATED)
            ?.observe(viewLifecycleOwner) { changed ->
                if (changed == true) {
                    // 네 ViewModel의 재조회 메서드로 교체. 없으면 간단 wrapper 하나 만들어도 OK.
                    vm.refresh()

                    // 소비 후 제거(중복 호출 방지)
                    handle.remove<Boolean>(KEY_ORDER_UPDATED)
                }
            }
    }

    /** 현재 어댑터 리스트를 CSV로 저장 */
    private fun exportCsv(uri: Uri) {
        val rows = adapter.currentList.toList() // ListAdapter의 immutable snapshot
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
                            val branch = r.branchName ?: r.branchName ?: "-"
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
                    Snackbar.make(requireView(), getString(R.string.msg_export_failed, e.message ?: ""), Snackbar.LENGTH_LONG).show()
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