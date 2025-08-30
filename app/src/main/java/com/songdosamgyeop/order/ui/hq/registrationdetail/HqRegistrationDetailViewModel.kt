package com.songdosamgyeop.order.ui.hq.registrationdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.songdosamgyeop.order.data.repo.Registration
import com.songdosamgyeop.order.data.repo.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HQ 신청서 상세 ViewModel.
 * - SavedStateHandle로 nav 인자(id) 수신
 * - Repository에서 실시간 구독하여 UI 상태로 노출
 */
@HiltViewModel
class HqRegistrationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: RegistrationRepository,
) : ViewModel() {

    /** 상세 대상 문서 ID (네비게이션 인자) */
    val docId: String = checkNotNull(savedStateHandle.get<String>("id")) {
        "id argument is required for HqRegistrationDetail"
    }

    /** UI 상태 모델 */
    sealed interface UiState {
        /** 로딩 중 */
        data object Loading : UiState
        /** 데이터 성공 */
        data class Success(val reg: Registration) : UiState
        /** 문서 없음 */
        data object NotFound : UiState
        /** 오류 (네트워크/권한 등) */
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    /** 화면에서 구독할 상태 */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        Log.d("HqRegDetailVM", "init with docId=$docId")
        viewModelScope.launch {
            repo.observeRegistration(docId).collect { reg ->
                _uiState.value = if (reg == null) UiState.NotFound else UiState.Success(reg)
            }
        }
    }
}