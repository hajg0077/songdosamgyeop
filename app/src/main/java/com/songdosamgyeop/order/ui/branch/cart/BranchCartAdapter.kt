package com.songdosamgyeop.order.ui.branch.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.databinding.ItemCartBinding
import com.songdosamgyeop.order.ui.branch.shop.BranchShopViewModel.Line
import java.text.NumberFormat
import java.util.*

class BranchCartAdapter(
    private val onInc: (Line) -> Unit,
    private val onDec: (Line) -> Unit
) : ListAdapter<Line, BranchCartAdapter.VH>(DIFF) {

    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    class VH(val b: ItemCartBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val line = getItem(pos)
        h.b.tvName.text = line.productName
        val unit = nf.format(line.unitPrice)
        val total = nf.format(line.amount)
        h.b.tvPrice.text = "${unit}원 x ${line.qty} = ${total}원"
        h.b.tvQty.text = line.qty.toString()

        // item_cart.xml의 버튼 id에 맞춤
        h.b.btnInc.setOnClickListener { onInc(line) }
        h.b.btnDec.setOnClickListener { onDec(line) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Line>() {
            override fun areItemsTheSame(a: Line, b: Line) = a.productId == b.productId
            override fun areContentsTheSame(a: Line, b: Line) = a == b
        }
    }
}