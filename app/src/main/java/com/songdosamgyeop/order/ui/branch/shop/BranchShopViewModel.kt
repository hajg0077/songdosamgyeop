// app/src/main/java/com/songdosamgyeop/order/ui/branch/shop/BranchShopViewModel.kt
package com.songdosamgyeop.order.ui.branch.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
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

    fun isCartEmpty(): Boolean = _cart.value.isEmpty()

    fun clearCart() { _cart.value = emptyMap() }

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

    /**
     * 주문 생성:
     * - 1순위: repo.placeAll(branchId, branchName, linesByBrand, note, requestedAt) (확장 시그니처)
     * - 2순위: 브랜드별 loop 후 repo.createOrder(items, branchId, branchName, note, requestedAt)
     * - 3순위: 가장 구버전 createOrder(items, branchId, branchName)
     */
    fun placeAll(
        branchId: String,
        branchName: String,
        note: String? = null,
        requestedAt: Timestamp? = null,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (_cart.value.isEmpty()) {
            onError(IllegalStateException("Cart is empty"))
            return
        }
        val grouped: Map<String, List<CartItem>> =
            _cart.value.values.groupBy { it.brandId ?: "COMMON" }
                .mapValues { (_, ls) -> ls.map { it.toCartItem() } }

        viewModelScope.launch {
            runCatching {
                // 확장 시그니처가 있는 경우(권장)
                val placeAll = ordersRepo::class.members.firstOrNull { it.name == "placeAll" }
                if (placeAll != null) {
                    // 리포지토리에서 CartItem/CartLine 타입을 다룰 수 있게 구현되어 있어야 함
                    @Suppress("UNCHECKED_CAST")
                    val m = ordersRepo.javaClass.getMethod(
                        "placeAll",
                        String::class.java,
                        String::class.java,
                        Map::class.java,
                        String::class.java,
                        Timestamp::class.java
                    )
                    m.invoke(ordersRepo, branchId, branchName, grouped, note, requestedAt)
                } else {
                    // 폴백: 브랜드별 createOrder
                    grouped.forEach { (_, items) ->
                        runCatching {
                            val mExt = ordersRepo.javaClass.getMethod(
                                "createOrder",
                                List::class.java, String::class.java, String::class.java, String::class.java, Timestamp::class.java
                            )
                            mExt.invoke(ordersRepo, items, branchId, branchName, note, requestedAt)
                        }.recoverCatching {
                            val mLegacy = ordersRepo.javaClass.getMethod(
                                "createOrder",
                                List::class.java, String::class.java, String::class.java
                            )
                            mLegacy.invoke(ordersRepo, items, branchId, branchName)
                        }.getOrThrow()
                    }
                }
            }.onSuccess {
                clearCart()
                onSuccess()
            }.onFailure(onError)
        }
    }
}