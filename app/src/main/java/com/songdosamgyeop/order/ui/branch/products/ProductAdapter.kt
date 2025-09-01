package com.songdosamgyeop.order.ui.branch.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val onAdd: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.VH>(DIFF) {

    class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = getItem(pos)
        h.b.tvName.text = p.name
        h.b.tvPrice.text = NumberFormat.getNumberInstance(Locale.KOREA).format(p.price) + "원"
        h.b.btnAdd.setOnClickListener { onAdd(p) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}
