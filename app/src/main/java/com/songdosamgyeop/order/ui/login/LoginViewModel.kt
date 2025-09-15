// com/songdosamgyeop/order/ui/login/LoginViewModel.kt
package com.songdosamgyeop.order.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.songdosamgyeop.order.core.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    /**
     * 개발용 더미 로그인:
     * - Firebase Anonymous 로그인
     * - users/{uid}에 role, (branch의 경우 branchId/branchName) 병합 저장
     * - 서버시간 기록(lastLoginAt/createdAt)
     */
    suspend fun signInDummy(role: UserRole) {
        // 이미 로그인 되어 있으면 재사용(익명만)하고, 아니면 익명 로그인
        val user = auth.currentUser?.takeIf { it.isAnonymous }
            ?: auth.signInAnonymously().await().user
            ?: error("Anonymous sign-in failed (user null)")

        val users = firestore.collection("users").document(user.uid)

        val base = mutableMapOf(
            "email" to "",
            "name" to "DevUser",
            "role" to when (role) { UserRole.HQ -> "HQ"; UserRole.BRANCH -> "BRANCH" },
            "lastLoginAt" to FieldValue.serverTimestamp()
        )

        // 지사 더미 필드 (앱에서 branch 라벨/필터에 사용)
        if (role == UserRole.BRANCH) {
            base["branchId"] = "BRANCH_DEV"
            base["branchName"] = "개발지사"
        }

        // createdAt이 없는 최초 사용자만 createdAt 세팅
        firestore.runTransaction { tx ->
            val snap = tx.get(users)
            if (!snap.exists()) {
                base["createdAt"] = FieldValue.serverTimestamp()
            }
            tx.set(users, base, SetOptions.merge())
        }.await()

        Log.d("LoginVM", "Dummy sign-in as $role, uid=${user.uid}")
    }
}