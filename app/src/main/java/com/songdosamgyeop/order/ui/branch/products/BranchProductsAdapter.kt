package com.songdosamgyeop.order.ui.branch.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.*

class BranchProductsAdapter(
    private val onPlus: (Product) -> Unit,
    private val onMinus: (Product) -> Unit,
    private val getQty: (productId: String) -> Int
) : ListAdapter<Product, BranchProductsAdapter.VH>(DIFF) {

    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = getItem(pos)
        h.b.tvName.text = p.name
        val meta = listOfNotNull(p.sku?.takeIf { it.isNotBlank() }, p.unit?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
        h.b.tvMeta.text = meta
        h.b.tvMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE
        h.b.tvPrice.text = "${nf.format(p.price)}원"

        val qty = getQty(p.id)
        h.b.tvQty.text = qty.toString()

        h.b.btnPlus.setOnClickListener { onPlus(p) }
        h.b.btnMinus.setOnClickListener { onMinus(p) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}