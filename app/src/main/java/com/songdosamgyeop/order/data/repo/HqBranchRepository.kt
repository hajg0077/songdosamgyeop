// data/repo/HqBranchRepository.kt
package com.songdosamgyeop.order.data.repo

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.data.model.Branch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HqBranchRepository @Inject constructor(
    private val db: FirebaseFirestore,
    // 선택: Functions 경유가 필요하면 주입
    private val functionsDS: com.songdosamgyeop.order.data.remote.HqFunctionsDataSource?
) {
    private val col get() = db.collection("branches")

    suspend fun list(): List<Branch> {
        val snap = col.orderBy("name").get().await()
        return snap.documents.map { d ->
            d.toObject(Branch::class.java)!!.copy(id = d.id)
        }
    }

    /** 지사 생성/업서트. docId = branchCode(대문자 권장) */
    suspend fun upsert(branchCode: String, name: String, contactName: String?, phone: String?) {
        if (Env.FUNCTIONS_ENABLED) {
            // 운영 경로: Cloud Function 호출 (원하면 함수명 맞춰 구현)
            // functionsDS?.hqCreateOrUpdateBranch(branchCode, name, contactName, phone)!!
            // 임시로 실패 처리
            error("Functions 경로는 아직 미구현입니다.")
        } else {
            val doc = mapOf(
                "name" to name,
                "active" to true,
                "contactName" to contactName,
                "phone" to phone,
                "updatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp() // merge 시 최초만 의미
            )
            col.document(branchCode.uppercase()).set(doc, SetOptions.merge()).await()
        }
    }

    suspend fun setActive(branchCode: String, active: Boolean) {
        if (Env.FUNCTIONS_ENABLED) {
            // functionsDS?.hqSetBranchActive(branchCode, active)!!
            error("Functions 경로는 아직 미구현입니다.")
        } else {
            col.document(branchCode.uppercase())
                .set(mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
        }
    }
}