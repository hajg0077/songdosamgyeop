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
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

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
            b.tvTotal.text = "합계 ${nf.format(amt)}원"
        }

        // 날짜 선택(선택)
        b.etDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("희망 납품일 선택")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { utcMillis ->
                // 선택값을 로컬 Date로 표시, 서버 저장은 Timestamp로
                val date = Date(utcMillis)
                b.etDate.setText(sdf.format(date))
                selectedDate = Timestamp(date)
            }
            picker.show(parentFragmentManager, "branchCheckoutDate")
        }

        b.btnConfirm.setOnClickListener {
            val note = b.etNote.text?.toString()?.takeIf { it.isNotBlank() }
            // TODO: 실제 로그인 사용자 정보로 치환
            val branchId = "BR001"
            val branchName = "송도"

            vm.placeAll(branchId, branchName, note = note, requestedAt = selectedDate)
            Snackbar.make(b.root, "주문이 접수되었습니다.", Snackbar.LENGTH_LONG).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private var selectedDate: Timestamp? = null

    private fun brandLabel(brandId: String) = when (brandId) {
        "SONGDO" -> "송도삼겹"
        "BULBAEK" -> "불백기사식당"
        "HONG" -> "홍선생 직화족발"
        "COMMON" -> "공용"
        else -> brandId
    }
}