package com.songdosamgyeop.order.ui.branch.checkout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.databinding.ItemCheckoutHeaderBinding
import com.songdosamgyeop.order.databinding.ItemCheckoutLineBinding
import com.songdosamgyeop.order.ui.branch.shop.BranchShopViewModel
import java.text.NumberFormat
import java.util.*

sealed class CheckoutRow {
    data class Header(val brandLabel: String): CheckoutRow()
    data class Line(val line: BranchShopViewModel.Line): CheckoutRow()
}

class BranchCheckoutAdapter :
    ListAdapter<CheckoutRow, RecyclerView.ViewHolder>(DIFF) {

    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is CheckoutRow.Header -> 0
        is CheckoutRow.Line   -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == 0) HeaderVH(
            ItemCheckoutHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        ) else LineVH(
            ItemCheckoutLineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is CheckoutRow.Header -> (holder as HeaderVH).bind(row.brandLabel)
            is CheckoutRow.Line   -> (holder as LineVH).bind(row.line, nf)
        }
    }

    class HeaderVH(private val b: ItemCheckoutHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(label: String) { b.tvHeader.text = label }
    }
    class LineVH(private val b: ItemCheckoutLineBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(l: BranchShopViewModel.Line, nf: NumberFormat) {
            b.tvName.text = l.productName
            b.tvMeta.text = "${nf.format(l.unitPrice)}원 × ${l.qty} = ${nf.format(l.amount)}원"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CheckoutRow>() {
            override fun areItemsTheSame(a: CheckoutRow, b: CheckoutRow): Boolean =
                (a is CheckoutRow.Header && b is CheckoutRow.Header && a.brandLabel == b.brandLabel) ||
                        (a is CheckoutRow.Line && b is CheckoutRow.Line && a.line.productId == b.line.productId)
            override fun areContentsTheSame(a: CheckoutRow, b: CheckoutRow) = a == b
        }
    }
}