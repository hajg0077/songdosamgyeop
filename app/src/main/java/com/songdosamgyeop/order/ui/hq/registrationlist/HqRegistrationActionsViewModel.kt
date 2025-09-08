package com.songdosamgyeop.order.ui.hq.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.Env
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class HqOrderActionsViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _message = MutableLiveData<Result<String>>()
    val message: LiveData<Result<String>> = _message

    fun updateStatus(orderId: String, nextStatus: String) {
        if (_busy.value == true) return
        if (!Env.FUNCTIONS_ENABLED) {
            _message.value = Result.failure(IllegalStateException("서버 기능(Functions) 미연결"))
            return
        }
        _busy.value = true
        viewModelScope.launch {
            try {
                val res = functions.getHttpsCallable("hqUpdateOrderStatus")
                    .call(mapOf("orderId" to orderId, "nextStatus" to nextStatus))
                    .await()
                val payload = res.getData()
                val msg = when (payload) {
                    is Map<*,*> -> payload["message"]?.toString() ?: "상태 변경 완료"
                    is String    -> payload
                    else         -> "상태 변경 완료"
                }
                _message.value = Result.success(msg)
            } catch (t: Throwable) {
                _message.value = Result.failure(t)
            } finally {
                _busy.value = false
            }
        }
    }
}