package com.songdosamgyeop.order.ui.hq.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** 로그아웃 상태 */
sealed class LogoutState {
    data object Idle : LogoutState()
    data object Loading : LogoutState()
    data object Success : LogoutState()
    data class Error(val message: String?) : LogoutState()
}

/** HQ 설정 화면용 VM: 계정/지사 정보 로드 + 로그아웃 실행 */
@HiltViewModel
class HqSettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    /** 이메일 */
    private val _email = MutableStateFlow("")
    val email = _email.asLiveData()

    /** "역할: HQ · 지사: OOO" 라벨 */
    private val _roleBranch = MutableStateFlow("")
    val roleBranch = _roleBranch.asLiveData()

    /** 로그아웃 상태 */
    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState = _logoutState.asLiveData()

    init {
        // 시작 시 계정 정보 로드
        viewModelScope.launch { loadProfile() }
    }

    /** users/{uid}에서 role/branchId 조회하고 branches에서 지사명 가져옴 */
    private suspend fun loadProfile() {
        try {
            val u = auth.currentUser ?: return
            _email.value = u.email ?: "(이메일 없음)"

            val userDoc = db.collection("users").document(u.uid).get().await()
            val role = (userDoc.getString("role") ?: "UNKNOWN")
            val branchId = (userDoc.getString("branchId") ?: "HQ")

            val branchName = if (branchId == "HQ") "본사" else {
                val b = db.collection("branches").document(branchId).get().await()
                b.getString("name") ?: branchId
            }

            _roleBranch.value = "역할: $role · 지사: $branchName"
        } catch (e: Exception) {
            // 실패해도 앱이 죽지 않게 기본값 유지
            _roleBranch.value = "역할: UNKNOWN · 지사: -"
        }
    }

    /** Firebase 로그아웃 수행 (Fragment에서 상태 구독) */
    fun logout() {
        // 진행 표시
        _logoutState.value = LogoutState.Loading
        viewModelScope.launch {
            try {
                auth.signOut()
                _logoutState.value = LogoutState.Success
            } catch (e: Exception) {
                _logoutState.value = LogoutState.Error(e.message)
            }
        }
    }
}