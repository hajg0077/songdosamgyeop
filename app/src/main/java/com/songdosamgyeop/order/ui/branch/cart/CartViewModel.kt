package com.songdosamgyeop.order.ui.branch.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.data.repo.OrdersRepository
import com.songdosamgyeop.order.data.model.CartItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * 장바구니(초안 주문) VM: 아이템 구독 + 수량 변경 + 확정
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository
) : ViewModel() {

    private val orderId = MutableStateFlow<String?>(null)

    /** 장바구니 아이템 목록 */
    val items = orderId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flow { emit(emptyList<CartItem>()) }
        else ordersRepo.subscribeItems(id)
    }.asLiveData()

    /** 합계 금액 */
    val total = items.map { list -> list.sumOf { it.lineTotal } }

    /** 초안 주문 준비 */
    fun ensureDraft() {
        viewModelScope.launch {
            if (orderId.value == null) {
                orderId.value = ordersRepo.getOrCreateDraft()
            }
        }
    }

    fun inc(productId: String, current: Int) {
        val id = orderId.value ?: return
        viewModelScope.launch { ordersRepo.setQty(id, productId, current + 1) }
    }

    fun dec(productId: String, current: Int) {
        val id = orderId.value ?: return
        viewModelScope.launch { ordersRepo.setQty(id, productId, current - 1) }
    }

    fun place(onDone: (Result<Unit>) -> Unit) {
        val id = orderId.value ?: return
        viewModelScope.launch {
            runCatching { ordersRepo.place(id) }
                .onSuccess { onDone(Result.success(Unit)) }
                .onFailure { onDone(Result.failure(it)) }
        }
    }
}
