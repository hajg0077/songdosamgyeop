package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.CartItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BranchOrdersRepository @Inject constructor(
    private val db: FirebaseFirestore,
) {
    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String
    ): String {
        require(items.isNotEmpty()) { "장바구니가 비었습니다." }
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("로그인 필요")

        val totalAmount = items.sumOf { it.amount }
        val itemsCount = items.sumOf { it.qty }

        val orders = db.collection("orders")
        val orderRef = orders.document() // 새 ID

        val now = FieldValue.serverTimestamp()
        val batch = db.batch()

        val order = mapOf(
            "ownerUid" to uid,
            "branchId" to branchId,
            "branchName" to branchName,
            "branchNameLower" to branchName.lowercase(),
            "status" to OrderStatus.PENDING.name,
            "itemsCount" to itemsCount,
            "totalAmount" to totalAmount,
            "placedAt" to now,
            "createdAt" to now,
            "updatedAt" to now
        )
        batch.set(orderRef, order)

        val itemsCol = orderRef.collection("items")
        items.forEach { c ->
            val itemRef = itemsCol.document()
            batch.set(itemRef, mapOf(
                "productId" to c.productId,
                "productName" to c.productName,
                "brandId" to c.brandId,
                "unitPrice" to c.unitPrice,
                "qty" to c.qty,
                "amount" to c.amount
            ))
        }

        batch.commit().await()
        return orderRef.id
    }
}
