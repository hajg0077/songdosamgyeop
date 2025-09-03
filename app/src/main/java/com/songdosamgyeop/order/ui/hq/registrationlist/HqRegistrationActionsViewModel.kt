package com.songdosamgyeop.order.ui.hq.registrationlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class HqRegistrationActionsViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _message = MutableLiveData<Result<String>>()
    val message: LiveData<Result<String>> = _message

    fun approve(docId: String) = call("hqApproveRegistration", mapOf("docId" to docId))
    fun reject(docId: String, reason: String?) =
        call("hqRejectRegistration", mapOf("docId" to docId, "reason" to (reason ?: "")))

    private fun call(fn: String, data: Map<String, Any?>) {
        if (!com.songdosamgyeop.order.Env.FUNCTIONS_ENABLED) {
            _message.value = Result.failure(IllegalStateException("서버 기능(Functions) 미연결"))
            return
        }
        _busy.value = true
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable(fn).call(data).await()

                // ✅ data 프로퍼티 직접 접근 X, getData() 사용
                val payload: Any? = result.getData()

                val msg = when (payload) {
                    is Map<*, *> -> payload["message"]?.toString() ?: "OK"
                    is String -> payload
                    null -> "OK"
                    else -> payload.toString()
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