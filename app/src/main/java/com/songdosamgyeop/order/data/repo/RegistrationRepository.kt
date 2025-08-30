package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * 신청서(registrations) 읽기 전용 레포지토리.
 * - 단일 문서 구독으로 상세 화면에 실시간 반영.
 */
class RegistrationRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /**
     * 주어진 docId의 신청서를 실시간으로 구독한다.
     * @param docId Firestore registrations 문서 ID
     * @return Flow<Registration?> 문서가 없으면 null
     */
    fun observeRegistration(docId: String): Flow<Registration?> = callbackFlow {
        Log.d("RegRepo", "observeRegistration start docId=$docId")
        val ref = db.collection("registrations").document(docId)
        val reg: ListenerRegistration = ref.addSnapshotListener { snap, e ->
            if (e != null) {
                Log.e("RegRepo", "snapshot error", e)
                trySend(null); return@addSnapshotListener
            }
            val data = snap?.data
            if (snap != null && data != null) {
                trySend(
                    Registration(
                        id = snap.id,
                        email = data["email"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        branchName = data["branchName"] as? String ?: "",
                        branchCode = data["branchCode"] as? String,
                        phone = data["phone"] as? String,
                        memo = data["memo"] as? String
                    )
                )
            } else {
                trySend(null)
            }
        }
        awaitClose { Log.d("RegRepo", "observeRegistration stop"); reg.remove() }
    }
}

/** 신청서 도메인 모델 (상세에서 필요한 필드만) */
data class Registration(
    val id: String,
    val email: String,
    val name: String,
    val branchName: String,
    val branchCode: String?,
    val phone: String?,
    val memo: String?
)
