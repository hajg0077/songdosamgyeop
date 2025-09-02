package com.songdosamgyeop.order.ui.branch.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.data.repo.OrdersRepository
import com.songdosamgyeop.order.ui.hq.orders.OrderDisplayRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * 브랜치 주문 히스토리 VM
 * - 기간 필터(선택) + 내 주문 목록 구독
 */
@HiltViewModel
class BranchOrdersViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository
) : ViewModel() {

    private val from = MutableStateFlow<Timestamp?>(null)
    private val to   = MutableStateFlow<Timestamp?>(null)

    // 내 주문 구독 → 화면 표시 모델로 변환
    val list = combine(from, to) { f, t -> f to t }
        .flatMapLatest { (f, t) -> ordersRepo.subscribeMyOrders(f, t) }
        .map { rows -> rows.map { OrderDisplayRow.from(it) } }
        .asLiveData(viewModelScope.coroutineContext)

    /** 기간 설정([start, endExclusive)) */
    fun setDateRange(start: Timestamp?, endExclusive: Timestamp?) {
        from.value = start; to.value = endExclusive
    }
}