package com.songdosamgyeop.order.ui.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.songdosamgyeop.order.core.model.UserRole
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    /** 같은 기기에서 이미 가입한 적 있는지 검사 */
    private suspend fun checkDeviceLock(installationId: String) {
        val devRef = db.collection("devices").document(installationId)
        val snap = devRef.get().await()
        if (snap.exists()) {
            throw IllegalStateException("이 기기에서는 이미 회원가입이 진행되었습니다.")
        }
    }

    /**
     * 지사 회원가입(신청):
     * - (1) devices/{installationId} 존재 여부 확인 → 있으면 실패
     * - (2) Auth 계정 생성
     * - (3) registrations/{uid} 생성 (status=PENDING)
     * - (4) devices/{installationId} 생성(서버 규칙상 최초 1회만 허용)
     */
    suspend fun signUpBranch(email: String, password: String, phone: String?, installationId: String) {
        // (1) 중복 가입 방지 사전 체크
        checkDeviceLock(installationId)

        // (2) 계정 생성
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("계정 생성 실패")

        // (3) 신청서
        val reg = mapOf(
            "email" to email.trim(),
            "status" to "PENDING",
            "userUid" to user.uid,
            "branchTel" to (phone ?: ""),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("registrations").document(user.uid).set(reg, SetOptions.merge()).await()

        // (4) 장치 락(규칙상 최초 1회만 성공)
        val dev = mapOf(
            "registeredUid" to user.uid,
            "email" to email.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("devices").document(installationId).set(dev).await()
    }

    suspend fun getCurrentUserRole(): UserRole {
        val uid = auth.currentUser?.uid ?: return UserRole.UNKNOWN
        val snap = db.collection("users").document(uid).get().await()
        return when (snap.getString("role").orEmpty()) {
            "HQ" -> UserRole.HQ
            "BRANCH" -> UserRole.BRANCH
            else -> UserRole.UNKNOWN
        }
    }
}