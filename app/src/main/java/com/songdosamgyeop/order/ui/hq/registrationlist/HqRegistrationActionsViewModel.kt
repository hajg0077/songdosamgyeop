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
        if (_busy.value == true) return
        _busy.value = true
        viewModelScope.launch {
            runCatching {
                val res = functions.getHttpsCallable(fn).call(data).await()
                res.data?.toString() ?: "OK"
            }.onSuccess { _message.value = Result.success(it) }
                .onFailure { _message.value = Result.failure(it) }
            _busy.value = false
        }
    }
}