package com.songdosamgyeop.order.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.data.repo.OrderRepository
import com.songdosamgyeop.order.data.repo.OrderPaymentUpdate
import com.songdosamgyeop.order.ui.payment.data.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * UI와 결제/검증 로직을 분리한 ViewModel.
 * - PortOne 콜백(impUid, merchantUid) → verifyAndApply() 호출
 * - Firestore 결제필드(markPayment)는 Repository에서 수행
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepo: PaymentRepository,
    private val orderRepo: OrderRepository
) : ViewModel() {

    sealed class UiEvent {
        data object Verifying : UiEvent()
        data class Success(val message: String = "결제 완료") : UiEvent()
        data class Failure(val message: String) : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    /**
     * PortOne 결제 성공 콜백(impUid!=null, success==true) 이후 호출.
     * - Functions(onCall)로 검증 → 결과를 orders/{orderId}에 반영
     */
    fun verifyAndApply(orderId: String, merchantUid: String, impUid: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.Verifying)
            runCatching {
                val result = paymentRepo.verifyAndApply(orderId, merchantUid, impUid)
                // 추가 반영이 필요한 경우 markPayment로 보수 업데이트 가능(여기서는 verify에서 이미 처리)
                if (!result.ok) {
                    orderRepo.markPayment(
                        orderId,
                        OrderPaymentUpdate(
                            merchantUid = merchantUid,
                            impUid = impUid,
                            paymentStatus = "FAILED",
                            message = result.message ?: "NOT_PAID"
                        )
                    )
                    _events.emit(UiEvent.Failure(result.message ?: "결제 검증 실패"))
                } else {
                    _events.emit(UiEvent.Success())
                }
            }.onFailure { e ->
                _events.emit(UiEvent.Failure(e.message ?: "결제 검증 중 오류"))
            }
        }
    }

    /** 결제 실패/취소 등 앱에서 받은 에러를 Firestore에 남기고 UI 알림 */
    fun markFailed(orderId: String, merchantUid: String, message: String) {
        viewModelScope.launch {
            runCatching {
                orderRepo.markPayment(
                    orderId,
                    OrderPaymentUpdate(
                        merchantUid = merchantUid,
                        impUid = null,
                        paymentStatus = "FAILED",
                        message = message
                    )
                )
            }
            _events.emit(UiEvent.Failure(message))
        }
    }
}