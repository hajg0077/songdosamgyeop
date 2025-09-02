package com.songdosamgyeop.order.ui.branch.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

/** 상품 카드 어댑터: + / - 클릭 콜백 제공 */
class ProductsAdapter(
    private val onPlus: (Product) -> Unit,
    private val onMinus: (Product) -> Unit,
    /** 현재 수량을 보여주기 위한 조회 함수 (VM의 cart에서 가져옴) */
    private val getQty: (productId: String) -> Int
) : ListAdapter<Product, ProductsAdapter.VH>(DIFF) {

    class VH(val b: ItemProductBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = getItem(position)
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        h.b.tvName.text = p.name
        h.b.tvMeta.text = "SKU ${p.sku} · 단위 ${p.unit}"
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