package com.songdosamgyeop.order.ui.branch.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.core.model.BrandId
import com.songdosamgyeop.order.data.model.CartLine
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.data.repo.OrdersRepository
import com.songdosamgyeop.order.data.repo.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** 브랜치 상품 주문 탭 VM: 상품 목록 필터 + 장바구니 + 브랜드별 분할 확정 */
@HiltViewModel
class BranchShopViewModel @Inject constructor(
    private val productsRepo: ProductRepository,
    private val ordersRepo: OrdersRepository
) : ViewModel() {

    // 필터 상태
    private val brandTab = MutableStateFlow(BrandId.SONGDO)
    private val category = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow<String?>(null)

    // 장바구니
    private val _cart = MutableStateFlow<Map<String, CartLine>>(emptyMap()) // key = productId
    val cart = _cart.asStateFlow()
    val cartList = cart.map { it.values.toList() }.asLiveData()

    // 상품 목록 구독
    val products = combine(brandTab, category, query.debounce(200)) { b, c, q ->
        Triple(b, c, q)
    }.flatMapLatest { (b, c, q) ->
        productsRepo.subscribeProducts(b, c, q)
    }.asLiveData(viewModelScope.coroutineContext)

    // --------- 장바구니 조작 ---------
    fun plus(p: Product) {
        val cur = _cart.value[p.id]
        val next = (cur?.qty ?: 0) + 1
        _cart.value = _cart.value + (p.id to CartLine(p.id, p.brandId, p.name, p.price, next))
    }
    fun minus(p: Product) {
        val cur = _cart.value[p.id] ?: return
        val next = (cur.qty - 1).coerceAtLeast(0)
        _cart.value = if (next == 0) _cart.value - p.id else _cart.value + (p.id to cur.copy(qty = next))
    }
    fun clearCart() { _cart.value = emptyMap() }

    // --------- 주문 확정(브랜드별 분할 생성) ---------
    fun placeAll() = viewModelScope.launch {
        val lines = _cart.value.values.filter { it.qty > 0 }
        if (lines.isEmpty()) return@launch

        val groups = lines.groupBy { it.brandId }           // SONGDO / BULBAEK / HONG / COMMON
        for ((brandId, group) in groups) {
            val orderId = ordersRepo.createDraftForBrand(brandId)
            ordersRepo.putItems(orderId, group)
            ordersRepo.place(orderId)
        }
        clearCart()
    }

    // --------- 외부에서 필터 변경 ---------
    fun setBrand(b: BrandId) { brandTab.value = b }
    fun setCategory(c: String?) { category.value = c }
    fun setQuery(q: String?) { query.value = q }
}