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
import com.songdosamgyeop.order.data.repo.CurrentUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val ordersRepo: BranchOrdersRepository,   // ✅ createOrder가 orderId 반환으로 변경됨
    private val currentUserRepo: CurrentUserRepository,
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

    // ==== 주문 생성 결과 이벤트 ====
    /** ✅ 통합결제에서 결제 진입에 필요한 orderId를 넘기도록 Success가 orderId를 보유 */
    sealed class PlaceEvent {
        data class Success(val orderId: String) : PlaceEvent()
        data class Failure(val message: String?) : PlaceEvent()
    }
    val placeEvents = MutableSharedFlow<PlaceEvent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** 현재 로그인 사용자의 지점 정보로 통합주문 1건 생성 */
    fun placeAllUsingCurrentUser(
        note: String? = null,
        requestedAt: com.google.firebase.Timestamp? = null
    ) {
        val lines = _cart.value.values.toList()
        if (lines.isEmpty()) {
            viewModelScope.launch { placeEvents.emit(PlaceEvent.Failure("EMPTY_CART")) }
            return
        }

        viewModelScope.launch {
            val info = currentUserRepo.getBranchInfo()
            if (info == null) {
                placeEvents.emit(PlaceEvent.Failure("NO_BRANCH_INFO"))
                return@launch
            }
            // ✅ 단일 주문으로 생성하도록 통일
            placeAll(info.branchId, info.branchName, note, requestedAt)
        }
    }

    /**
     * ✅ 통합주문 + 단일결제:
     * - 레포의 createOrder(...)가 **생성된 orderId(String)** 를 반환
     * - 장바구니 전체 라인을 한 번에 CartItem 리스트로 전달
     * - 성공 시 PlaceEvent.Success(orderId) 방출
     */
    fun placeAll(branchId: String, branchName: String, note: String? = null, requestedAt: Timestamp? = null) {
        val lines = _cart.value.values.toList()
        if (lines.isEmpty()) {
            viewModelScope.launch { placeEvents.emit(PlaceEvent.Failure("EMPTY_CART")) }
            return
        }
        val items = lines.map { it.toCartItem() }
        viewModelScope.launch {
            runCatching {
                ordersRepo.createOrder(items, branchId, branchName, note, requestedAt)
            }.onSuccess { orderId ->
                _cart.value = emptyMap()
                placeEvents.emit(PlaceEvent.Success(orderId))
            }.onFailure { e ->
                placeEvents.emit(PlaceEvent.Failure(e.message))
            }
        }
    }
}