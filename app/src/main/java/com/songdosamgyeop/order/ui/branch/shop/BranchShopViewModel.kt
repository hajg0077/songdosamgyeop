package com.songdosamgyeop.order.ui.branch.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.core.model.BrandId
import com.songdosamgyeop.order.data.model.CartItem
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.data.repo.BranchOrdersRepository
import com.songdosamgyeop.order.data.repo.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class BranchShopViewModel @Inject constructor(
    private val productRepo: ProductsRepository,
    private val ordersRepo: BranchOrdersRepository
) : ViewModel() {

    private val brand = MutableStateFlow(BrandId.SONGDO)
    private val category = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow<String?>(null)

    val products = combine(brand, category, query.debounce(200)) { b, c, q ->
        Triple(b, c, q)
    }.flatMapLatest { (b, c, q) ->
        productRepo.subscribeProducts(b, c, q)
    }.asLiveData()

    data class Line(
        val productId: String,
        val productName: String,
        val unitPrice: Long,
        val qty: Int,
        val brandId: String?,
        val category: String
    ) {
        val amount: Long get() = unitPrice * qty
        fun toCartItem() = CartItem(productId, productName, brandId, unitPrice, qty)
    }

    private val _cart = MutableStateFlow<Map<String, Line>>(emptyMap())
    val cart: StateFlow<Map<String, Line>> = _cart.asStateFlow()

    val cartList = cart.map { it.values.sortedBy { l -> l.productName } }.asLiveData()
    val totalQty = cart.map { it.values.sumOf { l -> l.qty } }.asLiveData()
    val totalAmount = cart.map { it.values.sumOf { l -> l.amount } }.asLiveData()

    fun setQty(productId: String, qty: Int) {
        val cur = _cart.value.toMutableMap()
        val ex = cur[productId] ?: return
        if (qty <= 0) cur.remove(productId) else cur[productId] = ex.copy(qty = qty)
        _cart.value = cur
    }

    fun setBrand(b: BrandId) { brand.value = b }
    fun setCategory(c: String?) { category.value = c }
    fun setQuery(q: String?) { query.value = q?.takeIf { it.isNotBlank() } }



    fun plus(p: Product) {
        val cur = _cart.value.toMutableMap()
        val ex = cur[p.id]
        cur[p.id] = if (ex == null)
            Line(p.id, p.name, p.price, 1, p.brandId, p.category)
        else ex.copy(qty = ex.qty + 1)
        _cart.value = cur
    }

    fun minus(p: Product) {
        val cur = _cart.value.toMutableMap()
        val ex = cur[p.id] ?: return
        val next = ex.qty - 1
        if (next <= 0) cur.remove(p.id) else cur[p.id] = ex.copy(qty = next)
        _cart.value = cur
    }

    /** 브랜드별로 주문 생성(PENDING) 후 장바구니 비움 */
    fun placeAll(
        branchId: String,
        branchName: String,
        note: String? = null,
        requestedAt: com.google.firebase.Timestamp? = null
    ) {
        val lines = _cart.value.values.toList()
        if (lines.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                lines.groupBy { it.brandId ?: "COMMON" }.forEach { (_, grouped) ->
                    val items = grouped.map { it.toCartItem() }
                    // 레포가 확장 시그니처를 지원하면 전달하고, 아니면 기존 메서드 호출
                    try {
                        ordersRepo.createOrder(items, branchId, branchName, note, requestedAt)
                    } catch (_: Throwable) {
                        ordersRepo.createOrder(items, branchId, branchName)
                    }
                }
            }.onSuccess { _cart.value = emptyMap() }
        }
    }
}