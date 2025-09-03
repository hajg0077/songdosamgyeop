package com.songdosamgyeop.order.ui.hq.orders.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrderDetailBinding
import com.songdosamgyeop.order.ui.common.StatusBadge
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * HQ 주문 상세 화면 (읽기 전용)
 * - 헤더/아이템을 ViewModel에서 구독하여 표시
 */
@AndroidEntryPoint
class HqOrderDetailFragment : Fragment(R.layout.fragment_hq_order_detail) {

    private val vm: HqOrderDetailViewModel by viewModels()
    private lateinit var adapter: HqOrderItemsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqOrderDetailBinding.bind(view)
        adapter = HqOrderItemsAdapter()
        b.recycler.adapter = adapter

        // 툴바 뒤로가기
        b.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }

        // 헤더 구독
        vm.header.observe(viewLifecycleOwner) { h ->
            if (h == null) {
                b.tvBranch.text = getString(R.string.msg_order_not_found)
                b.chipStatus.visibility = View.GONE
                b.tvWhen.text = "-"
                b.tvSummary.text = "-"
                return@observe
            }
            // 지점명/코드가 있으면 표시 우선순위 조정(원하는 값으로 바꿔도 됨)
            b.tvBranch.text = h.branchId

            // 상태 칩 적용
            applyStatusChip(b.chipStatus, h.status)

            // 주문 시각
            val whenStr = h.placedAt?.toDate()?.let(sdf::format)
                ?: h.createdAt?.toDate()?.let(sdf::format) ?: "-"
            b.tvWhen.text = whenStr

            // 합계/아이템 개수/짧은 ID
            val amount = h.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
            val count  = h.itemsCount?.let { "${it}개" } ?: "—"
            val shortId = h.id?.take(8) ?: "—"
            b.tvSummary.text = getString(R.string.hq_order_detail_summary, amount, count, shortId)
        }

        // 아이템 구독
        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** 주문 상태 문자열 -> 배지 스타일 적용 */
    private fun applyStatusChip(chip: Chip, status: String?) {
        val mapped = when (status?.uppercase(Locale.ROOT)) {
            "DRAFT"     -> StatusBadge.OrderStatus.DRAFT
            "PLACED"    -> StatusBadge.OrderStatus.PLACED
            "PREPARING" -> StatusBadge.OrderStatus.PREPARING
            "SHIPPED"   -> StatusBadge.OrderStatus.SHIPPED
            "CANCELED", "CANCELLED" -> StatusBadge.OrderStatus.CANCELED
            else -> StatusBadge.OrderStatus.DRAFT
        }
        StatusBadge.apply(chip, mapped)
        chip.visibility = View.VISIBLE
    }
}