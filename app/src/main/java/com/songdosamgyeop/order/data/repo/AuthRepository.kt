package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.core.model.UserRole
import jakarta.inject.Inject
import kotlinx.coroutines.tasks.await


class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    /** 현재 로그인 유저 반환 */
    fun currentUser() = auth.currentUser

    /** Firestore users/{uid}에서 role 읽기 */
    suspend fun fetchUserRole(uid: String): UserRole {
        return try {
            val snap = firestore.collection("users").document(uid).get().await()
            when (snap.getString("role")) {
                "HQ" -> UserRole.HQ
                "BRANCH" -> UserRole.BRANCH
                else -> UserRole.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "fetchUserRole error", e)
            UserRole.UNKNOWN
        }
    }
}
