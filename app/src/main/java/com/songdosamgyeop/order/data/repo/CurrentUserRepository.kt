package com.songdosamgyeop.order.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.core.model.BranchInfo
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentUserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    /** Firestore: users/{uid} 문서에서 branchId/branchName 읽어오기 */
    suspend fun getBranchInfo(): BranchInfo? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("users").document(uid).get().await()
        val branchId = doc.getString("branchId") ?: return null
        val branchName = doc.getString("branchName") ?: branchId
        return BranchInfo(uid, branchId, branchName)
    }

    /** (선택) 로그인 직후 저장/동기화용 */
    suspend fun upsertBranchInfo(branchId: String, branchName: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = mapOf("branchId" to branchId, "branchName" to branchName)
        db.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }
}