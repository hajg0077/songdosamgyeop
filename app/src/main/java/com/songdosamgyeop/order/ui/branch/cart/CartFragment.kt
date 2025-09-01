package com.songdosamgyeop.order.ui.branch.cart

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentCartBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart) {
    private val vm: CartViewModel by viewModels()
    private lateinit var adapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentCartBinding.bind(view)
        adapter = CartAdapter(
            onInc = { vm.inc(it.productId, it.qty) },
            onDec = { vm.dec(it.productId, it.qty) }
        )
        b.recycler.adapter = adapter

        vm.ensureDraft()

        vm.items.observe(viewLifecycleOwner) { list -> adapter.submitList(list) }
        vm.total.observe(viewLifecycleOwner) { t ->
            b.tvTotal.text = "합계 ${NumberFormat.getNumberInstance(Locale.KOREA).format(t)}원"
        }

        b.btnPlace.setOnClickListener {
            vm.place { res ->
                res.onSuccess {
                    Snackbar.make(b.root, "주문이 확정되었습니다.", Snackbar.LENGTH_SHORT).show()
                }.onFailure {
                    Snackbar.make(b.root, it.message ?: "실패", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
