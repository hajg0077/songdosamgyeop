package com.songdosamgyeop.order.ui.hq.orders.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.songdosamgyeop.order.data.model.OrderLine
import com.songdosamgyeop.order.databinding.ItemOrderLineBinding
import java.text.NumberFormat
import java.util.Locale

class HqOrderItemsAdapter
    : ListAdapter<HqOrderDetailViewModel.ItemRow, HqOrderItemsAdapter.VH>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<HqOrderDetailViewModel.ItemRow>() {
        override fun areItemsTheSame(old: HqOrderDetailViewModel.ItemRow, new: HqOrderDetailViewModel.ItemRow) = old.id == new.id
        override fun areContentsTheSame(old: HqOrderDetailViewModel.ItemRow, new: HqOrderDetailViewModel.ItemRow) = old == new
    }
        class VH(val b: ItemOrderLineBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemOrderLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = getItem(pos)
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        h.b.tvName.text = it.name
        h.b.tvUnit.text = "${nf.format(it.unitPrice)}원 × ${it.qty}개"
        h.b.tvLineTotal.text = "${nf.format(it.lineTotal)}원"
    }
}
