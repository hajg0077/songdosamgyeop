package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 브랜치(지사) 회원가입 신청 저장용 Repository.
 * - Firestore `registrations` 컬렉션에 문서를 생성한다.
 */
class BranchRegistrationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * 신청서를 Firestore에 저장한다.
     *
     * @param email 이메일(필수)
     * @param name 신청자 이름(필수)
     * @param branchName 지사명(필수)
     * @param branchCode 지사 코드(선택)
     * @param phone 연락처(선택)
     * @param memo 메모(선택)
     * @return 성공 시 생성된 문서 ID, 실패 시 예외를 포함한 Result
     */
    suspend fun submitRegistration(
        email: String,
        name: String,
        branchName: String,
        branchCode: String?,
        phone: String?,
        memo: String?
    ): Result<String> {
        return try {
            // 간단 검증 (필요시 강화)
            require(email.isNotBlank()) { "이메일을 입력하세요." }
            require(name.isNotBlank()) { "신청자 이름을 입력하세요." }
            require(branchName.isNotBlank()) { "지사명을 입력하세요." }

            // ✅ 저장 데이터(접두 검색/정렬용 보조 필드 포함)
            val data = mapOf(
                "email" to email.trim(),
                // "email_lower" 제거 ✅
                "name" to name.trim(),
                "branchName" to branchName.trim(),
                "branchName_lower" to branchName.trim().lowercase(),
                "branchCode" to branchCode?.trim(),
                "phone" to phone?.trim(),
                "memo" to memo?.trim(),
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()          // ✅ 최신순 정렬
            )

            val ref = firestore.collection("registrations").add(data).await()
            Log.d("BranchRegRepo", "submitRegistration ok id=${ref.id}")
            Result.success(ref.id)
        } catch (e: Exception) {
            Log.e("BranchRegRepo", "submitRegistration error", e)
            Result.failure(e)
        }
    }
}