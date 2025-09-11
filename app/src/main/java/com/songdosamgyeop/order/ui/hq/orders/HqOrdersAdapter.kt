package com.songdosamgyeop.order.ui.hq.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.databinding.ItemHqOrderBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HqOrdersAdapter(
    private val onClick: (orderId: String) -> Unit
) : ListAdapter<OrderDisplayRow, HqOrdersAdapter.VH>(DIFF) {

    class VH(val b: ItemHqOrderBinding) : RecyclerView.ViewHolder(b.root)

    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHqOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val row = getItem(pos)
        val amount = row.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
        val count  = row.itemsCount?.let { "${it}개" } ?: "—"

        h.b.tvTitle.text = "${row.branchLabel} · $amount · $count"

        val whenStr = row.placedAt?.let(sdf::format)
            ?: row.createdAt?.let(sdf::format) ?: "-"

        val shortId = row.id.take(8)
        h.b.tvSubtitle.text = "$whenStr · $shortId (${row.status})"

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