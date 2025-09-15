// app/src/main/java/com/songdosamgyeop/order/data/repo/BranchOrdersRepository.kt
package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String
    ): String = createOrder(items, branchId, branchName, note = null, requestedAt = null)

    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String,
        note: String?,
        requestedAt: Timestamp?
    ): String {
        require(items.isNotEmpty()) { "items is empty" }
        val uid = auth.currentUser?.uid ?: error("Not authenticated")

        val total = items.sumOf { (it.price ?: 0L) * it.qty }
        val count = items.sumOf { it.qty }

        val keywords = buildNoteKeywords(note)

        val header = hashMapOf(
            "ownerUid" to uid,
            "branchId" to branchId,
            "branchName" to branchName,
            "status" to "PLACED",
            "placedAt" to FieldValue.serverTimestamp(),
            "totalAmount" to total,
            "itemsCount" to count
        ).apply {
            if (!note.isNullOrBlank()) put("note", note)
            if (requestedAt != null) put("requestedAt", requestedAt)
            if (keywords.isNotEmpty()) put("noteKeywords", keywords)
        }

        val orderRef = db.collection("orders").document()
        orderRef.set(header).await()

        val batch = db.batch()
        items.forEach { line ->
            val itemRef = orderRef.collection("items").document(line.productId)
            val item = hashMapOf(
                "productId" to line.productId,
                "name" to (line.productName ?: line.productId),
                "qty" to line.qty,
                "unit" to (line.unit ?: ""),
                "price" to (line.price ?: 0L),
                "brandId" to (line.brandId ?: "COMMON")
            )
            batch.set(itemRef, item)
        }
        batch.commit().await()
        return orderRef.id
    }

    private fun buildNoteKeywords(note: String?): List<String> {
        val raw = note?.lowercase()?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(Regex("[^a-z0-9가-힣]+"))
            .mapNotNull { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(10)
    }
}