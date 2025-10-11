package com.songdosamgyeop.order.ui.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.songdosamgyeop.order.core.model.UserRole
import com.songdosamgyeop.order.Env
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    /** 이메일/비밀번호 로그인 */
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    /**
     * 지사 회원가입(신청):
     * - Firebase Auth 계정 생성
     * - registrations 컬렉션에 신청서 생성(status=PENDING)
     *   { email, name?, branchTel, status, userUid, createdAt }
     * - (옵션) users/{uid} 문서를 미리 최소 필드로 생성해도 되지만, 쓰기는 Functions에서 하는 게 안전함
     */
    suspend fun signUpBranch(email: String, password: String, phone: String?) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("계정 생성 실패")

        // 신청서 작성 (누구나 create 가능 규칙 가정)
        val reg = mapOf(
            "email" to email.trim(),
            "name" to (user.displayName ?: ""),    // 없으면 빈값
            "branchName" to null,                  // HQ가 지정
            "branchCode" to null,                  // HQ가 지정 시 입력
            "status" to "PENDING",
            "userUid" to user.uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "branchTel" to (phone?.trim())
        )
        db.collection("registrations").document(user.uid).set(reg, SetOptions.merge()).await()

        // (선택) Functions 사용이 켜져 있으면 서버 로직 호출 가능
        if (Env.FUNCTIONS_ENABLED) {
            // ex) requestBranchRegistration callable 호출 etc. (이미 Functions 구축되어 있다면)
        }
    }

    /** 현재 로그인 사용자의 역할을 가져온다. 없으면 UNKNOWN 반환 */
    suspend fun getCurrentUserRole(): UserRole {
        val uid = auth.currentUser?.uid ?: return UserRole.UNKNOWN
        val snap = db.collection("users").document(uid).get().await()
        val roleStr = snap.getString("role").orEmpty()
        return when (roleStr) {
            "HQ" -> UserRole.HQ
            "BRANCH" -> UserRole.BRANCH
            else -> UserRole.UNKNOWN
        }
    }
}