package com.songdosamgyeop.order.ui.hq.orders

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrdersBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.TimeZone
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.BuildConfig
import com.songdosamgyeop.order.core.export.OrdersCsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HqOrdersFragment : Fragment(R.layout.fragment_hq_orders) {
    private val vm: HqOrdersViewModel by viewModels()
    private lateinit var b: FragmentHqOrdersBinding
    private lateinit var adapter: HqOrdersAdapter

    // SAF: CSV 파일 생성
    private val createCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        exportTo(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nav = findNavController()
        adapter = HqOrdersAdapter { orderId ->
            nav.navigate(
                R.id.action_hqOrders_to_hqOrderDetail,
                bundleOf("orderId" to orderId)
            )
        }
        b.recycler.adapter = adapter

        // 상태 칩
        b.chipPlaced.setOnClickListener { vm.setStatus("PLACED") }
        b.chipDraft.setOnClickListener { vm.setStatus("DRAFT") } // 향후 대비

        // 지사명 검색
        b.etBranchName.doOnTextChanged { text, _, _, _ ->
            vm.setBranchNameQuery(text?.toString())
        }

        // 기간 선택: 머티리얼 범위 피커 (UTC millis → Timestamp)
        b.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("조회 기간 선택")
                .build()
            picker.addOnPositiveButtonClickListener { range ->
                val start = range.first ?: return@addOnPositiveButtonClickListener
                val end = range.second ?: return@addOnPositiveButtonClickListener

                // end는 '끝 날짜의 23:59:59' 포함 위해 +1일 00:00으로 배타 상한 처리
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = end
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                vm.setDateRange(
                    Timestamp(start / 1000, ((start % 1000) * 1_000_000).toInt()),
                    Timestamp(cal.timeInMillis / 1000, 0)
                )
            }
            picker.show(parentFragmentManager, "dateRange")
        }

        b.btnClearDate.setOnClickListener { vm.setDateRange(null, null) }

        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_hq_orders, menu)
                // 필요 시: BuildConfig.FEATURE_EXPORT에 따라 숨기기/보이기
                menu.findItem(R.id.action_export_csv).isVisible = true
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId == R.id.action_export_csv) {
                    if (!BuildConfig.FEATURE_EXPORT) {
                        Snackbar.make(b.root, "CSV 내보내기는 추후 제공 예정입니다.", Snackbar.LENGTH_SHORT)
                            .show()
                        return true
                    }
                    val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA).format(Date())
                    createCsv.launch("orders_$ts.csv")
                    return true
                }
                return false
            }
        }, viewLifecycleOwner)

        // 리스트 구독
        vm.displayList.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            b.tvEmpty.visibility = if (rows.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
    /** 현재 화면 리스트를 CSV로 생성해 지정 URI에 기록 */
    private fun exportTo(uri: Uri) {
        val rows = vm.displayList.value.orEmpty()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1) CSV 문자열 생성
                val csv = OrdersCsvExporter.buildCsv(rows)

                // 2) Excel 호환 위해 UTF-8 BOM 추가
                val bom = "\uFEFF".toByteArray(Charset.forName("UTF-8"))
                val bytes = csv.toByteArray(Charset.forName("UTF-8"))

                // 3) SAF로 쓰기
                requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(bom)
                    os.write(bytes)
                    os.flush()
                } ?: error("파일을 열 수 없습니다.")

                withContext(Dispatchers.Main) {
                    Snackbar.make(b.root, "CSV로 내보냈어요.", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(b.root, "내보내기 실패: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

