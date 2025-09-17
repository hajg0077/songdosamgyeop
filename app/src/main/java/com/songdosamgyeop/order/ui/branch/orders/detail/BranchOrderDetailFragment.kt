package com.songdosamgyeop.order.ui.branch.orders.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchOrderDetailBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip
import com.songdosamgyeop.order.ui.hq.orders.detail.HqOrderItemsAdapter
import com.songdosamgyeop.order.ui.hq.orders.detail.HqOrderDetailViewModel   // ✅ 추가
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BranchOrderDetailFragment : Fragment(R.layout.fragment_branch_order_detail) {

    private val vm: BranchOrderDetailViewModel by viewModels()
    private lateinit var adapter: HqOrderItemsAdapter

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

            val whenStr = h.placedAt?.toDate()?.let { sdf.format(it) }
                ?: h.createdAt?.toDate()?.let { sdf.format(it) } ?: "-"
            b.tvWhen.text = whenStr

            val amount = h.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
            val count  = h.itemsCount?.let { "${it}개" } ?: "—"
            b.tvSummary.text = "합계 $amount · $count"
        }

        vm.items.observe(viewLifecycleOwner) { lines ->
            // ✅ 어댑터가 기대하는 타입으로 변환
            val rows: List<HqOrderDetailViewModel.ItemRow> =
                lines.map { line -> HqOrderDetailViewModel.ItemRow.Line(line) }  // ItemRow.Line(…) 형태 가정
            adapter.submitList(rows)
            b.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}