package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.data.model.CartItem
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BranchOrdersRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BranchOrdersRepository {

    override suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String,
        note: String?,
        requestedAt: Timestamp?
    ): String {
        require(items.isNotEmpty()) { "items is empty" }
        val uid = auth.currentUser?.uid ?: error("Not authenticated")

        val total = items.sumOf { it.unitPrice * it.qty }
        val count = items.sumOf { it.qty }

        val header = hashMapOf(
            "ownerUid" to uid,
            "branchId" to branchId,
            "branchName" to branchName,
            "branchName_lower" to branchName.lowercase(Locale.KOREA),
            "status" to "PENDING",
            "placedAt" to FieldValue.serverTimestamp(),
            "totalAmount" to total,
            "itemsCount" to count
        ).apply {
            if (!note.isNullOrBlank()) put("note", note)
            if (requestedAt != null) put("requestedAt", requestedAt)
        }

        val orderRef = db.collection("orders").document()
        orderRef.set(header).await()

        val batch = db.batch()
        items.forEach { line ->
            val itemRef = orderRef.collection("items").document(line.productId)
            val item = hashMapOf(
                "productId" to line.productId,
                "name" to line.productName,
                "brandId" to (line.brandId ?: "COMMON"),
                "unit" to (line.unit ?: ""),
                "price" to line.unitPrice,
                "qty" to line.qty
            )
            batch.set(itemRef, item)
        }
        batch.commit().await()

        return orderRef.id
    }
}