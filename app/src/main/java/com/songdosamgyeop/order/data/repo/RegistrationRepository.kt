package com.songdosamgyeop.order.data.repo

import com.songdosamgyeop.order.data.model.Registration
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
interface RegistrationRepository {
    /**
     * 읽기 권한 없이 중복 방지:
     * - 문서 ID = 소문자 이메일(공백 제거)
     * - create() 사용 → 이미 존재하면 예외 발생 (업데이트 권한 없어도 안전)
     */
    suspend fun submit(reg: Registration): Result<Unit>
    fun listenPendingRegistrations(): Flow<List<Pair<String, Registration>>>
}

@Singleton
class RegistrationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : RegistrationRepository {

    private val col get() = db.collection("registrations")

    override suspend fun submit(reg: Registration): Result<Unit> = runCatching {
        val emailId = reg.email.trim().lowercase()
        require(emailId.isNotBlank()) { "이메일을 입력하세요." }
        val docRef = col.document(emailId)

        // create() → 문서가 이미 있으면 실패(중복 방지), 보안 규칙 create만으로 충분
        docRef.create(reg.copy(email = emailId)).await()

        Timber.d("Registration created: $emailId")
    }

    override fun listenPendingRegistrations(): Flow<List<Pair<String, Registration>>> {
        TODO("Not yet implemented")
    }
}