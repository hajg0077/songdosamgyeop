package com.songdosamgyeop.order.ui.branch.orders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.songdosamgyeop.order.data.repo.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** 브랜치 주문 상세 VM: 헤더/아이템 구독 */
@HiltViewModel
class BranchOrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: OrdersRepository
) : ViewModel() {

    /** 네비게이션 인자: orderId (필수) */
    val orderId: String = checkNotNull(savedStateHandle.get<String>("orderId")) {
        "orderId argument is required"
    }

    val header = repo.observeOrderHeader(orderId).asLiveData()
    val items  = repo.observeOrderItems(orderId).asLiveData()
}