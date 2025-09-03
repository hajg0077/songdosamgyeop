package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.core.model.RegistrationStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject


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


/** 신청서 레포지토리 (목록/상세) */
class RegistrationRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /**
     * 상태+검색어에 맞는 신청서를 실시간 구독한다.
     * - 검색어가 있으면 branchName_lower prefix 검색(startAt/endAt)
     * - 검색어 없으면 createdAt DESC 정렬
     * ⚠️ 최초 실행 시 Firestore에서 "index 필요" 링크가 뜰 수 있음 → 안내대로 인덱스 생성.
     */
    fun subscribeList(
        status: RegistrationStatus,
        queryText: String?
    ): Flow<List<Pair<String, Registration>>> = callbackFlow {
        var q: Query = db.collection("registrations")
            .whereEqualTo("status", status.name)

        q = if (!queryText.isNullOrBlank()) {
            // ✅ 지사명 접두 검색만
            q.orderBy("branchName_lower")
                .startAt(queryText.lowercase())
                .endAt(queryText.lowercase() + "\uf8ff")
        } else {
            q.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        Log.d("RegRepo", "subscribeList status=$status, query=$queryText")
        val reg: ListenerRegistration = q.addSnapshotListener { snap, e ->
            if (e != null) {
                Log.e("RegRepo", "listen error", e)
                trySend(emptyList()); return@addSnapshotListener
            }
            val list = snap?.documents?.map { d ->
                val data = d.data ?: emptyMap<String, Any>()
                val item = Registration(
                    id = d.id,
                    email = data["email"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    branchName = data["branchName"] as? String ?: "",
                    branchCode = data["branchCode"] as? String,
                    phone = data["phone"] as? String,
                    memo = data["memo"] as? String
                )
                d.id to item
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    /**
     * 단일 문서 상세 구독 (이미 적용했지만 함께 유지).
     */
    fun observeRegistration(docId: String): Flow<Registration?> = callbackFlow {
        val ref = db.collection("registrations").document(docId)
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(null)
                return@addSnapshotListener
            }

            val data = snap?.data
            if (data == null) {
                trySend(null)
                return@addSnapshotListener
            }

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
            // 필요하면 성공여부 소비: trySend(...).isSuccess
        }
        awaitClose { reg.remove() }
    }
}