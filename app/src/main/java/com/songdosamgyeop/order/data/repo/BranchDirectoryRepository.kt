package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * 지사 디렉터리( branchId -> branchName ) 구독 레포지토리.
 * - 컬렉션: branches (docId = branchId, fields: { name })
 * - HQ 전용 조회를 가정(읽기 권한 필요)
 */
class BranchDirectoryRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /** branchId → branchName 맵을 실시간 구독 */
    fun subscribeMap(): Flow<Map<String, String>> = callbackFlow {
        val reg = db.collection("branches")
            .addSnapshotListener { snap, e ->
                if (e != null) { Log.e("BranchDirRepo", "listen error", e); trySend(emptyMap()); return@addSnapshotListener }
                val map = snap?.documents?.associate { d ->
                    val name = (d.get("name") as? String)?.ifBlank { d.id } ?: d.id
                    d.id to name
                } ?: emptyMap()
                trySend(map)
            }
        awaitClose { reg.remove() }
    }
}
