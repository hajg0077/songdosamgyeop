package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.data.model.CartItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BranchOrdersRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /** 기본 시그니처: note/requestedAt 없이 주문 생성 */
    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String
    ): String {
        return createOrder(items, branchId, branchName, note = null, requestedAt = null)
    }

    /** 확장 시그니처: note, requestedAt 포함 */
    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String,
        note: String? = null,
        requestedAt: Timestamp? = null
    ): String {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

        // order 헤더
        val header = hashMapOf(
            "ownerUid" to uid,
            "branchId" to branchId,
            "branchName" to branchName,
            "status" to "PENDING",
            "placedAt" to Timestamp.now(),
            "totalAmount" to items.sumOf { it.amount },
            "itemsCount" to items.sumOf { it.qty },
            "note" to note,
            "requestedAt" to requestedAt
        )

        // 주문 문서 생성
        val orderRef = db.collection("orders").document()
        orderRef.set(header).await()

        // 아이템 저장
        val batch = db.batch()
        items.forEach { item ->
            val line = hashMapOf(
                "productId" to item.productId,
                "name" to item.productName,
                "brandId" to item.brandId,
                "price" to item.unitPrice,
                "qty" to item.qty,
                "unit" to (item.unit ?: "")
            )
            val itemRef = orderRef.collection("items").document(item.productId)
            batch.set(itemRef, line)
        }
        batch.commit().await()

        return orderRef.id
    }
}