package com.songdosamgyeop.order.ui.branch.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.databinding.ItemOrderLineBinding
import java.text.NumberFormat
import java.util.Locale

class BranchOrderLinesAdapter :
    ListAdapter<OrderLineUI, BranchOrderLinesAdapter.VH>(DIFF) {

    private val money = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemOrderLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, money)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(
        private val b: ItemOrderLineBinding,
        private val money: NumberFormat
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: OrderLineUI) {
            b.tvName.text = item.name
            val unit = item.unit.ifBlank { "-" }
            val unitPrice = money.format(item.price)
            val amt = money.format(item.price * item.qty)
            b.tvUnit.text = "단가 $unitPrice × ${item.qty}개 ($unit)"
            b.tvLineTotal.text = amt
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderLineUI>() {
            override fun areItemsTheSame(old: OrderLineUI, new: OrderLineUI) =
                old.name == new.name && old.unit == new.unit

            override fun areContentsTheSame(old: OrderLineUI, new: OrderLineUI) = old == new
        }
    }
}