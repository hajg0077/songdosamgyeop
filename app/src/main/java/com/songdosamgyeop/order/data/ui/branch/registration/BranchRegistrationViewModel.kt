package com.songdosamgyeop.order.data.ui.branch.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.data.model.Registration
import com.songdosamgyeop.order.data.repo.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BranchRegistrationViewModel @Inject constructor(
    private val repo: RegistrationRepository
) : ViewModel() {

    val email = MutableLiveData("")
    val name = MutableLiveData("")
    val branchName = MutableLiveData("")
    val branchCode = MutableLiveData("")
    val phone = MutableLiveData("")
    val memo = MutableLiveData("")

    private val _submitting = MutableLiveData(false)
    val submitting: LiveData<Boolean> = _submitting

    private val _result = MutableLiveData<Result<Unit>>()
    val result: LiveData<Result<Unit>> = _result

    fun isFormValid(): Boolean {
        val e = email.value?.trim().orEmpty()
        val n = name.value?.trim().orEmpty()
        val bn = branchName.value?.trim().orEmpty()
        val bc = branchCode.value?.trim().orEmpty()

        val emailOk = e.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()
        return emailOk && n.isNotBlank() && bn.isNotBlank() && bc.isNotBlank()
    }

    fun submit() {
        if (!isFormValid()) {
            _result.value = Result.failure(IllegalArgumentException("필수 항목을 확인하세요."))
            return
        }
        val reg = Registration(
            email = email.value!!.trim(),
            name = name.value!!.trim(),
            branchName = branchName.value!!.trim(),
            branchCode = branchCode.value!!.trim(),
            phone = phone.value?.trim().orEmpty(),
            memo = memo.value?.trim().orEmpty()
        )

        _submitting.value = true
        viewModelScope.launch {
            repo.submit(reg)
                .onSuccess {
                    Timber.d("신청서 제출 성공")
                    _result.value = Result.success(Unit)
                }
                .onFailure { t ->
                    Timber.e(t, "신청서 제출 실패")
                    _result.value = Result.failure(t)
                }
            _submitting.value = false
        }
    }
}