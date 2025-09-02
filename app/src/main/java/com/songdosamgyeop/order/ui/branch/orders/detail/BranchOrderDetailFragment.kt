package com.songdosamgyeop.order.ui.branch.orders.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchOrderDetailBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip
import com.songdosamgyeop.order.ui.hq.orders.detail.HqOrderItemsAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/** 브랜치 주문 상세 (읽기 전용) */
@AndroidEntryPoint
class BranchOrderDetailFragment : Fragment(R.layout.fragment_branch_order_detail) {

    private val vm: BranchOrderDetailViewModel by viewModels()
    private lateinit var adapter: HqOrderItemsAdapter // 라인아이템 어댑터 재사용

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentBranchOrderDetailBinding.bind(view)
        adapter = HqOrderItemsAdapter()
        b.recycler.adapter = adapter

        b.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

        vm.header.observe(viewLifecycleOwner) { h ->
            if (h == null) { b.tvTitle.text = "주문을 찾을 수 없습니다."; return@observe }
            b.tvTitle.text = "주문 ${h.id.take(8)}"
            applyOrderStatusChip(b.chipStatus, h.status)
            val whenStr = h.placedAt?.toDate()?.let(sdf::format)
                ?: h.createdAt?.toDate()?.let(sdf::format) ?: "-"
            b.tvWhen.text = whenStr
            val amount = h.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
            val count  = h.itemsCount?.let { "${it}개" } ?: "—"
            b.tvSummary.text = "합계 $amount · $count"
        }

        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
}