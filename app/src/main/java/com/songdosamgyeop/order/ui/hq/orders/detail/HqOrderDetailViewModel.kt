package com.songdosamgyeop.order.ui.hq.orders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.songdosamgyeop.order.data.repo.HqOrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * HQ 주문 상세 VM
 * - SavedStateHandle에서 orderId 수신
 * - 헤더/아이템 각각 실시간 구독
 */
@HiltViewModel
class HqOrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: HqOrdersRepository
) : ViewModel() {

    /** 네비게이션 인자: 주문 ID (필수) */
    val orderId: String = checkNotNull(savedStateHandle.get<String>("orderId")) {
        "orderId argument is required"
    }

    /** 상단 요약 정보 */
    val header = repo.observeOrderHeader(orderId).asLiveData()

    /** 라인아이템 목록 */
    val items = repo.observeOrderItems(orderId).asLiveData()
}
