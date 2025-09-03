package com.songdosamgyeop.order.ui.branch.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.core.model.BrandId
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.data.repo.OrdersRepository
import com.songdosamgyeop.order.data.repo.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * 상품 목록 VM: 필터(브랜드/카테고리/검색) + 목록 구독 + '장바구니에 담기'
 */
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepo: ProductRepository,
    private val ordersRepo: OrdersRepository
) : ViewModel() {

    // ---- 필터 상태 ----
    private val brand = MutableStateFlow(BrandId.SONGDO)
    private val category = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow<String?>(null)

    // ---- 목록 구독 ----
    val products = combine(brand, category, query.debounce(200)) { b, c, q ->
        Triple(b, c, q)
    }.flatMapLatest { (b, c, q) ->
        productsRepo.subscribeProducts(b, c, q)
    }.asLiveData()

    fun setBrand(b: BrandId) { brand.value = b }
    fun setCategory(c: String?) { category.value = c }
    fun setQuery(q: String?) { query.value = q }

    // ---- 장바구니에 1개 추가 (프로젝트 구현에 맞춰 호출부만 유지) ----
    fun addToCart(p: Product, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            runCatching {
                // ※ 당신 프로젝트의 OrdersRepository 구현에 맞게 사용
                // 예: draft를 하나 가져와 아이템 1개 추가
                val orderId = ordersRepo.getOrCreateDraft()
                ordersRepo.addOne(orderId, p)
            }.onSuccess { onDone(Result.success(Unit)) }
                .onFailure { onDone(Result.failure(it)) }
        }
    }
}