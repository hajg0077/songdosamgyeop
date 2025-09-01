package com.songdosamgyeop.order.ui.branch.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.data.repo.BranchRegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 지사 회원가입 신청 ViewModel.
 * - Repository를 호출하여 신청서를 저장한다.
 */
@HiltViewModel
class BranchRegistrationViewModel @Inject constructor(
    private val repo: BranchRegistrationRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    /** 저장 중 로딩 상태 */
    val loading: LiveData<Boolean> = _loading

    private val _submitResult = MutableLiveData<Result<String>>()
    /** 저장 결과 (성공: docId, 실패: 에러) */
    val submitResult: LiveData<Result<String>> = _submitResult

    /**
     * 회원가입 신청서를 저장한다.
     */
    fun submit(
        email: String,
        name: String,
        branchName: String,
        branchCode: String?,
        phone: String?,
        memo: String?
    ) {
        _loading.value = true
        viewModelScope.launch {
            val res = repo.submitRegistration(email, name, branchName, branchCode, phone, memo)
            _submitResult.value = res
            _loading.value = false
        }
    }
}