package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.core.model.RegistrationAddress
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 브랜치(지사) 회원가입 신청 저장용 Repository.
 * - Firestore `registrations/{uid}` 에 문서를 생성/갱신한다.
 */
class BranchRegistrationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * 신청서를 Firestore에 저장한다.
     *
     * @param userUid  인증된 사용자 UID (필수)  ← 규칙: 문서ID와 auth.uid가 매칭되어야 함
     * @param email    이메일(필수)
     * @param branchName 지사명(필수)
     * @param phone    연락처(필수)
     * @param installationId 설치ID(필수: 기기 1회 제한)
     * @param address  주소(roadAddr/zipNo/detail)  ← 필수
     */
    suspend fun submitRegistration(
        userUid: String,
        email: String,
        branchName: String,
        phone: String?,
        installationId: String,
        address: RegistrationAddress
    ): Result<Unit> = runCatching {
        require(userUid.isNotBlank()) { "userUid가 필요합니다." }
        require(email.isNotBlank()) { "이메일을 입력하세요." }
        require(branchName.isNotBlank()) { "지사명을 입력하세요." }
        require(address.roadAddr.isNotBlank()) { "도로명주소를 입력하세요." }
        require(address.zipNo.length == 5) { "우편번호 5자리를 입력하세요." }
        // detail은 비워둘 수도 있지만, 현장 정책에 맞춰 필수로 강제하려면 아래 주석 해제
        // require(address.detail.isNotBlank()) { "상세주소를 입력하세요." }

        val data = mapOf(
            "userUid" to userUid,
            "email" to email.trim(),
            "branchName" to branchName.trim(),
            "branchName_lower" to branchName.trim().lowercase(),
            "branchTel" to phone?.trim(),
            "installationId" to installationId,
            "status" to "PENDING",
            "address" to mapOf(
                "roadAddr" to address.roadAddr,
                "zipNo" to address.zipNo,
                "detail" to address.detail
            ),
            "createdAt" to FieldValue.serverTimestamp()
        )

        // ✅ 규칙에 맞게 docId=uid 로 저장
        firestore.collection("registrations").document(userUid).set(data).await()
        Log.d("BranchRegRepo", "submitRegistration ok uid=$userUid")
    }
}