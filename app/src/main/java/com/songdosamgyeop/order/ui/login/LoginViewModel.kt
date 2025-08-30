package com.songdosamgyeop.order.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.songdosamgyeop.order.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * 로그인 관련 최소 스텁 ViewModel.
 * - 개발 편의를 위한 Anonymous 로그인 + users/{uid} 기본 문서 생성
 * - 실제 출시는 이메일/비번/커스텀 토큰 등으로 교체
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    /**
     * (개발용) 익명 로그인 후 users/{uid} 문서를 role에 맞춰 생성/병합한다.
     * @param role 로그인 후 부여할 역할(HQ/BRANCH). 스플래시 분기 테스트용.
     */
    suspend fun signInDummy(role: UserRole) {
        // 1) Firebase Anonymous 로그인 수행
        val user = auth.signInAnonymously().await().user ?: run {
            Log.e("LoginVM", "Anonymous sign-in failed: user null")
            return
        }

        // 2) users/{uid} 문서 준비 (개발 편의 필드)
        val data = mutableMapOf(
            "email" to "",
            "name" to "DevUser",
            "role" to if (role == UserRole.HQ) "HQ" else "BRANCH"
        )
        if (role == UserRole.BRANCH) {
            data["branchId"] = "BRANCH_DEV"
        }

        // 3) Firestore에 병합 저장
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()

        Log.d("LoginVM", "Dummy sign-in as $role, uid=${user.uid}")
    }
}
