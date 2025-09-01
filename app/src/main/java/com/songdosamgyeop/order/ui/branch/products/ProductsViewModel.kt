package com.songdosamgyeop.order.ui.branch.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.data.model.Product
import com.songdosamgyeop.order.data.repo.OrdersRepository
import com.songdosamgyeop.order.data.repo.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 상품 목록 VM: 목록 구독 + '장바구니에 담기'
 */
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepo: ProductsRepository,
    private val ordersRepo: OrdersRepository
) : ViewModel() {

    /** 활성 상품 목록 */
    val products = productsRepo.subscribeActiveProducts().asLiveData()

    /** 장바구니에 1개 추가 */
    fun addToCart(p: Product, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val orderId = ordersRepo.getOrCreateDraft()
                ordersRepo.addOne(orderId, p)
            }.onSuccess { onDone(Result.success(Unit)) }
                .onFailure { onDone(Result.failure(it)) }
        }
    }
}
