// app/src/main/java/com/songdosamgyeop/order/ui/branch/checkout/BranchCheckoutFragment.kt
package com.songdosamgyeop.order.ui.branch.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchCheckoutBinding
import com.songdosamgyeop.order.ui.branch.shop.BranchShopViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BranchCheckoutFragment : Fragment(R.layout.fragment_branch_checkout) {

    private val vm: BranchShopViewModel by activityViewModels()
    private lateinit var b: FragmentBranchCheckoutBinding
    private lateinit var adapter: BranchCheckoutAdapter
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

    private var requestedAt: Timestamp? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentBranchCheckoutBinding.bind(view)

        b.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        adapter = BranchCheckoutAdapter()
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        // 섹션 데이터 구성(브랜드별 헤더 + 라인)
        vm.cartList.observe(viewLifecycleOwner) { lines ->
            val grouped = lines.groupBy { (it.brandId ?: "COMMON") }
            val seq = mutableListOf<CheckoutRow>()
            grouped.toSortedMap().forEach { (brand, list) ->
                seq += CheckoutRow.Header(brandLabel(brand))
                list.sortedBy { it.productName }.forEach { l ->
                    seq += CheckoutRow.Line(l)
                }
            }
            adapter.submitList(seq)
        }

        // 합계
        vm.totalAmount.observe(viewLifecycleOwner) { amt ->
            b.tvTotal.text = getString(R.string.order_amount_fmt, nf.format(amt))
        }

        // 날짜 선택(희망 납품일) — UTC 보정
        b.etDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_requested_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { utcMillis ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = utcMillis
                // 로컬 표시 + 시분은 10:00으로 고정(가정)
                val local = Calendar.getInstance().apply {
                    set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 10, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                requestedAt = Timestamp(local.time)
                b.etDate.setText(sdf.format(local.time))
            }
            picker.show(parentFragmentManager, "branchCheckoutDate")
        }

        b.btnConfirm.setOnClickListener {
            if (vm.isCartEmpty()) {
                Snackbar.make(b.root, R.string.cart_empty, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val note = b.etNote.text?.toString()?.takeIf { it.isNotBlank() }

            // TODO 실제 로그인/선택된 지점으로 치환
            val branchId = "TODO_BRANCH_ID"
            val branchName = "TODO_BRANCH_NAME"

            b.btnConfirm.isEnabled = false
            vm.placeAll(
                branchId = branchId,
                branchName = branchName,
                note = note,
                requestedAt = requestedAt,
                onSuccess = {
                    Snackbar.make(b.root, R.string.order_placed, Snackbar.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                },
                onError = {
                    b.btnConfirm.isEnabled = true
                    Snackbar.make(b.root, getString(R.string.order_failed_fmt, it.message ?: "unknown"), Snackbar.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun brandLabel(brandId: String) = when (brandId) {
        "SONGDO" -> "송도삼겹"
        "BULBAEK" -> "불백기사식당"
        "HONG" -> "홍선생 직화족발"
        "COMMON" -> "공용"
        else -> brandId
    }
}