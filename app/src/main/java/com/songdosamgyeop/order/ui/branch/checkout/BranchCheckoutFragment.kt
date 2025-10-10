package com.songdosamgyeop.order.ui.branch.checkout

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchCheckoutBinding
import com.songdosamgyeop.order.ui.branch.shop.BranchShopViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BranchCheckoutFragment : Fragment(R.layout.fragment_branch_checkout) {

    private val vm: BranchShopViewModel by activityViewModels()
    private lateinit var b: FragmentBranchCheckoutBinding
    private lateinit var adapter: BranchCheckoutAdapter
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    private var selectedDate: Timestamp? = null

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

        // 날짜 선택
        b.etDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_requested_date)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { utcMillis ->
                val date = Date(utcMillis)
                b.etDate.setText(sdf.format(date))
                b.inputDate.error = null
                selectedDate = Timestamp(date)
            }
            picker.show(parentFragmentManager, "branchCheckoutDate")
        }

        // 사용자가 직접 텍스트를 바꿨을 때 selectedDate 동기화 시도
        b.etDate.doAfterTextChanged {
            val text = it?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                selectedDate = null
                b.inputDate.error = null
            } else {
                try {
                    val parsed = sdf.parse(text)
                    selectedDate = parsed?.let(::Timestamp)
                    b.inputDate.error = null
                } catch (_: ParseException) {
                    selectedDate = null
                    b.inputDate.error = getString(R.string.invalid_date_fmt, "yyyy-MM-dd")
                }
            }
        }

        // 주문 확정
        b.btnConfirm.setOnClickListener {
            // 빈 카트 방지
            val qty = vm.totalQty.value ?: 0
            if (qty <= 0) {
                Snackbar.make(b.root, R.string.cart_empty, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 날짜 유효성(입력칸이 비어있으면 null 허용, 값이 있으면 파싱 성공해야 함)
            val dateText = b.etDate.text?.toString()?.trim().orEmpty()
            if (dateText.isNotEmpty() && selectedDate == null) {
                b.inputDate.error = getString(R.string.invalid_date_fmt, "yyyy-MM-dd")
                return@setOnClickListener
            }

            val note = b.etNote.text?.toString()?.takeIf { it.isNotBlank() }
            // TODO: 실제 로그인 사용자/지점 정보 사용
            val branchId = "BR001"
            val branchName = "송도"

            vm.placeAll(branchId, branchName, note = note, requestedAt = selectedDate)
        }

        // 주문 결과 이벤트 처리 → 성공 시 PortOne 결제화면 진입
        viewLifecycleOwner.lifecycleScope.launch {
            vm.placeEvents.collectLatest { ev ->
                when (ev) {
                    is BranchShopViewModel.PlaceEvent.Success -> {
                        Snackbar.make(b.root, R.string.order_placed, Snackbar.LENGTH_SHORT).show()

                        // 결제 파라미터 구성
                        val orderId = ev.orderId
                        val amount = vm.totalAmount.value ?: 0L
                        val dateStr = b.etDate.text?.toString()?.takeIf { it.isNotBlank() }
                        val title = buildTitleForPayment(dateStr)

                        // 구매자 정보 (실서비스에선 사용자 프로필에서 가져와 세팅)
                        val buyerName: String? = null // TODO
                        val buyerEmail: String? = null // TODO
                        val buyerTel: String = ""   // TODO

                        // 결제 화면으로 이동
                        CheckoutNavigator.goPayment(
                            context = requireContext(),
                            orderId = orderId,
                            title = title,
                            amount = amount,
                            buyerName = buyerName,
                            buyerEmail = buyerEmail,
                            buyerTel = buyerTel
                        )

                        // 결제화면으로 넘어가므로 현재 화면은 종료해도 무방
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    is BranchShopViewModel.PlaceEvent.Failure -> {
                        Snackbar.make(
                            b.root,
                            getString(R.string.order_failed_fmt, ev.message ?: "unknown"),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun brandLabel(brandId: String) = when (brandId) {
        "SONGDO" -> "송도삼겹"
        "BULBAEK" -> "불백기사식당"
        "HONG" -> "홍선생 직화족발"
        "COMMON" -> "공용"
        else -> brandId
    }

    /** 결제 타이틀: 지점명 + 요청일(있으면) */
    private fun buildTitleForPayment(dateStr: String?): String {
        val base = getString(R.string.checkout_title_base) // 예: "주문 결제"
        return if (dateStr.isNullOrBlank()) base else "$base · $dateStr"
    }
}