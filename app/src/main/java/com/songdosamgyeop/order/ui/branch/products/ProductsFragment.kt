package com.songdosamgyeop.order.ui.branch.products

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentProductsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductsFragment : Fragment(R.layout.fragment_products) {

    private val vm: ProductsViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentProductsBinding.bind(view)
        adapter = ProductAdapter { p ->
            vm.addToCart(p) { res ->
                res.onSuccess { Snackbar.make(b.root, "담겼습니다.", Snackbar.LENGTH_SHORT).show() }
                    .onFailure { Snackbar.make(b.root, it.message ?: "실패", Snackbar.LENGTH_LONG).show() }
            }
        }
        b.recycler.adapter = adapter

        vm.products.observe(viewLifecycleOwner) { list -> adapter.submitList(list) }
    }
}
