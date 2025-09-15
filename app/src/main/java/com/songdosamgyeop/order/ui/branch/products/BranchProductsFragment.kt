package com.songdosamgyeop.order.ui.branch.products

import android.os.Bundle
import android.view.View
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.BrandId
import com.songdosamgyeop.order.databinding.FragmentProductsBinding
import com.songdosamgyeop.order.ui.branch.shop.BranchShopViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

/** 브랜치 상품 주문 탭: 브랜드/카테고리/검색 + 장바구니 + 확정 */
@AndroidEntryPoint
class BranchProductsFragment : Fragment(R.layout.fragment_products) {

    private val vm: BranchShopViewModel by viewModels()
    private lateinit var b: FragmentProductsBinding
    private lateinit var adapter: BranchProductsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentProductsBinding.bind(view)

        adapter = BranchProductsAdapter(
            onPlus = { vm.plus(it) },
            onMinus = { vm.minus(it) },
            getQty = { id -> vm.cart.value[id]?.qty ?: 0 }
        )
        b.recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        b.recycler.adapter = adapter

        b.chipGroupBrand.setOnCheckedStateChangeListener { _, _ ->
            val brand = when {
                b.chipBrandSongdo.isChecked -> BrandId.SONGDO
                b.chipBrandBulbaek.isChecked -> BrandId.BULBAEK
                b.chipBrandHong.isChecked -> BrandId.HONG
                b.chipBrandCommon.isChecked -> BrandId.COMMON
                else -> BrandId.SONGDO
            }
            vm.setBrand(brand)
            vm.setCategory(null)
            b.chipGroupCategory.clearCheck()
            b.etSearch.setText("")
        }

        b.etSearch.doOnTextChanged { text, _, _, _ ->
            vm.setQuery(text?.toString())
        }

        vm.products.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val cats = list.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
            buildCategoryChips(cats)
        }

        vm.cartList.observe(viewLifecycleOwner) { lines ->
            val nf = NumberFormat.getNumberInstance(Locale.KOREA)
            val totalQty = lines.sumOf { it.qty }
            val totalAmount = lines.sumOf { it.qty * it.unitPrice }
            b.tvCartSummary.text = "총 ${totalQty}개 · ${nf.format(totalAmount)}원"
            b.btnPlace.isEnabled = totalQty > 0
        }

        b.btnPlace.setOnClickListener {
            vm.placeAll()
            Snackbar.make(b.root, "브랜드별로 주문이 생성되었습니다.", Snackbar.LENGTH_SHORT).show()
        }

        b.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun buildCategoryChips(categories: List<String>) {
        val wanted = listOf<String?>(null) + categories
        val currentLabels = b.chipGroupCategory.children.map { (it as Chip).text?.toString() }.toList()
        val targetLabels = wanted.map { it ?: "전체" }
        if (currentLabels == targetLabels) return

        b.chipGroupCategory.removeAllViews()

        b.chipGroupCategory.addView(makeCategoryChip("전체", checked = true) {
            vm.setCategory(null)
        })
        categories.forEach { cat ->
            b.chipGroupCategory.addView(makeCategoryChip(cat, checked = false) {
                vm.setCategory(cat)
            })
        }
    }

    private fun makeCategoryChip(label: String, checked: Boolean, onChecked: () -> Unit): Chip {
        return Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
            text = label
            isCheckable = true
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> if (isChecked) onChecked() }
        }
    }
}