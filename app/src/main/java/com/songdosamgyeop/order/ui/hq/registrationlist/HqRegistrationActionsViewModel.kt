package com.songdosamgyeop.order.ui.hq.registrationlist

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.songdosamgyeop.order.Env
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * HQ 신청서 승인/반려/되돌리기 액션 전용 ViewModel.
 * Cloud Functions:
 *  - hqApproveRegistration({ docId })
 *  - hqRejectRegistration({ docId, reason })
 *  - hqResetRegistration({ docId })
 */
@HiltViewModel
class HqRegistrationActionsViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _message = MutableLiveData<Result<String>>()
    val message: LiveData<Result<String>> = _message

    fun approve(docId: String) = launchIfIdle {
        call(
            name = "hqApproveRegistration",
            data = mapOf("docId" to docId),
            successMsg = "승인 완료"
        )
    }

    fun reject(docId: String, reason: String?) = launchIfIdle {
        call(
            name = "hqRejectRegistration",
            data = mapOf("docId" to docId, "reason" to (reason ?: "")),
            successMsg = "반려 완료"
        )
    }

    fun reset(docId: String) = launchIfIdle {
        call(
            name = "hqResetRegistration",
            data = mapOf("docId" to docId),
            successMsg = "되돌리기 완료"
        )
    }

    private inline fun launchIfIdle(crossinline block: suspend () -> Unit) {
        if (_busy.value == true) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (t: Throwable) {
                _message.value = Result.failure(t)
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun call(
        name: String,
        data: Map<String, Any?>,
        successMsg: String
    ) {
        if (!Env.FUNCTIONS_ENABLED) {
            _message.value = Result.failure(IllegalStateException("서버 기능(Functions) 미연결"))
            return
        }

        try {
            Log.d("HQ_ACTION", "call name=$name data=$data region=${Env.FUNCTIONS_REGION}")

            val res = functions.getHttpsCallable(name).call(data).await()
            val payload = res.getData()

            val msg = when (payload) {
                is Map<*, *> -> payload["message"]?.toString() ?: successMsg
                is String -> payload
                null -> successMsg
                else -> payload.toString()
            }

            _message.value = Result.success(msg)
        } catch (e: FirebaseFunctionsException) {
            Log.e("HQ_ACTION", "call failed name=$name code=${e.code}", e)
            _message.value = Result.failure(mapFunctionsError(e))
        } catch (t: Throwable) {
            Log.e("HQ_ACTION", "call failed name=$name", t)
            _message.value = Result.failure(t)
        }
    }

    private fun mapFunctionsError(e: FirebaseFunctionsException): Throwable {
        val msg = when (e.code) {
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "권한이 없습니다. (HQ 전용 기능)"
            FirebaseFunctionsException.Code.NOT_FOUND -> "대상을 찾을 수 없습니다."
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> "이미 처리되었거나 전이 규칙 위반입니다."
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "서버 응답이 지연되고 있습니다."
            FirebaseFunctionsException.Code.UNAVAILABLE -> "서버가 일시적으로 응답하지 않습니다. (에뮬레이터/네트워크 확인)"
            else -> e.message ?: "처리에 실패했습니다."
        }
        return IllegalStateException(msg, e)
    }
}