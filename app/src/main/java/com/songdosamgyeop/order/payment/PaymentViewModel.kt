package com.songdosamgyeop.order.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.ui.payment.data.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 결제 성공 콜백 → 서버 검증 호출 → Firestore 반영까지 Repository가 수행.
 * VM은 결과만 받아 UI 이벤트로 전달.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepo: PaymentRepository
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

    fun verifyAndApply(orderId: String, merchantUid: String, impUid: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.Verifying)
            runCatching {
                paymentRepo.verifyAndApply(orderId, merchantUid, impUid)
            }.onSuccess { r ->
                if (r.ok) _events.emit(UiEvent.Success())
                else _events.emit(UiEvent.Failure(r.message ?: "결제 검증 실패"))
            }.onFailure { e ->
                _events.emit(UiEvent.Failure(e.message ?: "결제 검증 중 오류"))
            }
        }
    }

    fun notifyFailed(message: String) {
        viewModelScope.launch { _events.emit(UiEvent.Failure(message)) }
    }
}