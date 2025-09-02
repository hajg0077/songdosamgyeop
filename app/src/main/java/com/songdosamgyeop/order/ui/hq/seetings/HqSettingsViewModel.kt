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

/** HQ 설정 화면용 VM: 계정/지사 정보 로드 + 로그아웃 실행 */
@HiltViewModel
class HqSettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    /** 이메일 */
    private val _email = MutableStateFlow<String>("")
    val email = _email.asLiveData()

    /** "역할: HQ · 지사: OOO" 라벨 */
    private val _roleBranch = MutableStateFlow<String>("")
    val roleBranch = _roleBranch.asLiveData()

    init {
        // 시작 시 계정 정보 로드
        viewModelScope.launch { loadProfile() }
    }

    /** users/{uid}에서 role/branchId 조회하고 branches에서 지사명 가져옴 */
    private suspend fun loadProfile() {
        val u = auth.currentUser ?: return
        _email.value = u.email ?: "(이메일 없음)"

        val userDoc = db.collection("users").document(u.uid).get().await()
        val role = userDoc.getString("role") ?: "UNKNOWN"
        val branchId = userDoc.getString("branchId") ?: "HQ"

        val branchName = if (branchId == "HQ") "본사" else {
            val b = db.collection("branches").document(branchId).get().await()
            b.getString("name") ?: branchId
        }

        _roleBranch.value = "역할: $role · 지사: $branchName"
    }

    /** Firebase 로그아웃 수행 */
    fun logout() {
        auth.signOut()
        // MainActivity의 AuthStateListener가 알아서 로그인 그래프로 전환
    }
}