package com.songdosamgyeop.order.ui.hq.orders.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.databinding.ItemOrderLineBinding
import java.text.NumberFormat
import java.util.Locale

class HqOrderItemsAdapter
    : ListAdapter<HqOrderDetailViewModel.ItemRow, HqOrderItemsAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HqOrderDetailViewModel.ItemRow>() {
            override fun areItemsTheSame(
                old: HqOrderDetailViewModel.ItemRow,
                new: HqOrderDetailViewModel.ItemRow
            ): Boolean = when {
                old is HqOrderDetailViewModel.ItemRow.Line && new is HqOrderDetailViewModel.ItemRow.Line ->
                    old.line.productId == new.line.productId
                else -> old == new   // 다른 타입(섹션 등)이 들어오면 동일성은 equals로 처리
            }

            override fun areContentsTheSame(
                old: HqOrderDetailViewModel.ItemRow,
                new: HqOrderDetailViewModel.ItemRow
            ): Boolean = old == new
        }
    }

    class VH(val b: ItemOrderLineBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemOrderLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)

        when (item) {
            is HqOrderDetailViewModel.ItemRow.Line -> {
                val l = item.line
                h.b.tvName.text = l.name
                h.b.tvUnit.text = "${nf.format(l.unitPrice)}원 × ${l.qty}개"
                h.b.tvLineTotal.text = "${nf.format(l.unitPrice * l.qty)}원"
            }
            // 필요 시 섹션 등 다른 뷰타입 처리 추가
        }
    }
}