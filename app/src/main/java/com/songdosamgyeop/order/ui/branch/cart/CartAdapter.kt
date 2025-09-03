package com.songdosamgyeop.order.ui.branch.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.songdosamgyeop.order.data.model.CartItem
import com.songdosamgyeop.order.databinding.ItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val onInc: (CartItem) -> Unit,
    private val onDec: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.VH>(DIFF) {

    class VH(val b: ItemCartBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        h.b.tvName.text = item.name
        h.b.tvQty.text = item.qty.toString()
        h.b.tvPrice.text = "${nf.format(item.unitPrice)}원 x ${item.qty} = ${nf.format(item.lineTotal)}원"
        h.b.btnInc.setOnClickListener { onInc(item) }
        h.b.btnDec.setOnClickListener { onDec(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(a: CartItem, b: CartItem) = a.productId == b.productId
            override fun areContentsTheSame(a: CartItem, b: CartItem) = a == b
        }
    }
}
