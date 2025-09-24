package com.songdosamgyeop.order.notify

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object TokenUploader {

    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    /** 로그인 성공 직후나 앱 시작 시 호출해 토큰 저장/갱신 */
    fun refreshAndUpload() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { t ->
            upload(t)
        }.addOnFailureListener { e ->
            Log.w("TokenUploader", "token failure", e)
        }
    }

    fun upload(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val doc = db.collection("userTokens").document(uid)
        val data = mapOf(
            "token" to token,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        doc.set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { Log.d("TokenUploader", "token uploaded") }
            .addOnFailureListener { e -> Log.w("TokenUploader", "upload failed", e) }
    }
}