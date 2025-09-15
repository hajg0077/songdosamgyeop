package com.songdosamgyeop.order.ui.branch.cart

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchCartBinding
import com.songdosamgyeop.order.ui.branch.shop.BranchShopViewModel
import java.text.NumberFormat
import java.util.*

class BranchCartFragment : Fragment(R.layout.fragment_branch_cart) {

    private val vm: BranchShopViewModel by activityViewModels()
    private lateinit var b: FragmentBranchCartBinding
    private lateinit var adapter: BranchCartAdapter
    private val nf = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentBranchCartBinding.bind(view)

        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = BranchCartAdapter(
            onInc = { line -> vm.setQty(line.productId, line.qty + 1) },
            onDec = { line -> vm.setQty(line.productId, (line.qty - 1).coerceAtLeast(0)) }
        )
        b.recycler.adapter = adapter

        vm.cartList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
        vm.totalAmount.observe(viewLifecycleOwner) { amt ->
            b.tvTotal.text = "합계 ${nf.format(amt)}원"
        }

        b.btnPlace.text = "주문하기" // (선택)
        b.btnPlace.setOnClickListener {
            findNavController().navigate(R.id.action_branchCart_to_branchCheckout)
        }
    }
}