package com.songdosamgyeop.order.data.ui.hq.registrationdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class HqRegistrationDetailViewModel @Inject constructor(
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _result = MutableLiveData<Result<String>>() // 메시지/리셋링크 등
    val result: LiveData<Result<String>> = _result

    fun approve(docId: String) = call("hqApproveRegistration", mapOf("docId" to docId))
    fun reject(docId: String, reason: String?) =
        call("hqRejectRegistration", mapOf("docId" to docId, "reason" to (reason ?: "")))

    private fun call(fn: String, data: Map<String, Any?>) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                val res = functions
                    .getHttpsCallable(fn)
                    .call(data)
                    .await()
                res.data?.toString() ?: "OK"
            }.onSuccess { _result.value = Result.success(it) }
                .onFailure { _result.value = Result.failure(it) }
            _loading.value = false
        }
    }
}