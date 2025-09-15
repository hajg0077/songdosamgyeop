// app/src/main/java/com/songdosamgyeop/order/ui/branch/history/BranchOrderLinesAdapter.kt
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

    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemOrderLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, nf)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val b: ItemOrderLineBinding,
        private val nf: NumberFormat
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(it: OrderLineUI) = with(b) {
            tvName.text = it.name
            tvMeta.text = "x${it.qty} ${it.unit}"
            tvAmount.text = nf.format(it.price * it.qty) + "원"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderLineUI>() {
            override fun areItemsTheSame(oldItem: OrderLineUI, newItem: OrderLineUI) =
                oldItem.name == newItem.name && oldItem.unit == newItem.unit
            override fun areContentsTheSame(oldItem: OrderLineUI, newItem: OrderLineUI) =
                oldItem == newItem
        }
    }
}