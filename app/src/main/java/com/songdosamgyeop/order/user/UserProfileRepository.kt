package com.songdosamgyeop.order.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val local: UserLocalDataStore
) {
    /** Firestore 실시간 스트림 */
    private fun remoteFlow(): Flow<UserProfile?> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val reg = db.collection("users").document(user.uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(null); return@addSnapshotListener }
                if (snap != null && snap.exists()) {
                    val m = snap.data ?: emptyMap<String, Any?>()
                    val p = UserProfile(
                        uid = user.uid,
                        email = (m["email"] as? String).orEmpty(),
                        name = (m["name"] as? String).orEmpty(),
                        role = (m["role"] as? String).orEmpty(),
                        branchId = m["branchId"] as? String,
                        branchName = m["branchName"] as? String
                    )
                    trySend(p)
                } else {
                    trySend(null)
                }
            }
        awaitClose { /* reg.remove()는 반환값 받았을 때 호출 */ }
    }.onEach { p ->
        // 원격 값을 받으면 로컬 갱신
        if (p != null) local.saveProfile(p) else local.clear()
    }

    /** 외부에서 구독할 Flow — 로컬 먼저, 원격으로 덮어쓰기 */
    fun currentUserFlow(): Flow<UserProfile?> =
        merge(local.getProfileFlow(), remoteFlow())
}