package com.songdosamgyeop.order.ui.hq.orders.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrderDetailBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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

        b.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

        // 헤더 구독
        vm.header.observe(viewLifecycleOwner) { h ->
            if (h == null) {
                b.tvBranch.text = "주문을 찾을 수 없습니다."
                return@observe
            }
            b.tvBranch.text = h.branchId
            applyOrderStatusChip(b.chipStatus, h.status)

            val whenStr = h.placedAt?.toDate()?.let(sdf::format)
                ?: h.createdAt?.toDate()?.let(sdf::format) ?: "-"
            b.tvWhen.text = whenStr

            val amount = h.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
            val count  = h.itemsCount?.let { "${it}개" } ?: "—"
            b.tvSummary.text = "합계 $amount · $count · ${h.id.take(8)}"
        }

        // 아이템 구독
        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
}
