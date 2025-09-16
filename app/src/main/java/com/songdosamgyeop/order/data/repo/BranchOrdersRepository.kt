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
class BranchOrdersRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /** 기존 시그니처(호환): note/requestedAt 없이 주문 생성 */
    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String
    ): String = createOrder(items, branchId, branchName, note = null, requestedAt = null)

    /** 확장 시그니처: note, requestedAt 포함 */
    suspend fun createOrder(
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

        // Header (지사명 검색용 branchName_lower 추가)
        val header = hashMapOf(
            "ownerUid" to uid,
            "branchId" to branchId,
            "branchName" to branchName,
            "branchName_lower" to branchName.lowercase(Locale.KOREA), // ✅ 지사명 prefix 검색용
            "status" to "PENDING",
            "placedAt" to FieldValue.serverTimestamp(),
            "totalAmount" to total,
            "itemsCount" to count
        ).apply {
            if (!note.isNullOrBlank()) put("note", note)
            if (requestedAt != null) put("requestedAt", requestedAt)
        }

        // TODO: Env.FUNCTIONS_ENABLED=true 전환 시 서버에서 생성/검증하도록 마이그레이션 (Rules/Functions)
        val orderRef = db.collection("orders").document()
        orderRef.set(header).await()

        // Lines (하위 subcollection)
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