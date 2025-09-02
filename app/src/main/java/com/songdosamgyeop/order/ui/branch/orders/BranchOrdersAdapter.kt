package com.songdosamgyeop.order.ui.branch.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.databinding.ItemBranchOrderBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip
import com.songdosamgyeop.order.ui.hq.orders.OrderDisplayRow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/** 브랜치 주문 히스토리 어댑터 */
class BranchOrdersAdapter(
    private val onClick: (orderId: String) -> Unit
) : ListAdapter<OrderDisplayRow, BranchOrdersAdapter.VH>(DIFF) {

    class VH(val b: ItemBranchOrderBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemBranchOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val row = getItem(pos)
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

        h.b.tvTitle.text = "주문 ${row.id.take(8)}"
        val whenStr = row.placedAtMs?.let { sdf.format(Date(it)) } ?: "-"
        val amount = row.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
        val count  = row.itemsCount?.let { "${it}개" } ?: "—"
        h.b.tvSubtitle.text = "$whenStr · $count · $amount"

        applyOrderStatusChip(h.b.chipStatus, row.status)
        h.itemView.setOnClickListener { onClick(row.id) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderDisplayRow>() {
            override fun areItemsTheSame(a: OrderDisplayRow, b: OrderDisplayRow) = a.id == b.id
            override fun areContentsTheSame(a: OrderDisplayRow, b: OrderDisplayRow) = a == b
        }
    }
}