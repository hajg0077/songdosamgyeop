package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.CartItem
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 주문/장바구니 레포지토리.
 * 구조:
 * orders/{orderId} { ownerUid, branchId, status, createdAt, placedAt? }
 * orders/{orderId}/items/{productId} { name, unitPrice, qty }
 */
class OrdersRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /** 현재 사용자 uid */
    private fun uid(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("로그인이 필요합니다.")

    /** users/{uid}.branchId 조회 */
    private suspend fun fetchBranchId(): String {
        val u = uid()
        val snap = db.collection("users").document(u).get().await()
        return snap.getString("branchId")
            ?: throw IllegalStateException("users/$u 에 branchId가 없습니다.")
    }

    /** 장바구니 항목 실시간 구독 */
    fun subscribeItems(orderId: String): Flow<List<CartItem>> = callbackFlow {
        val reg = db.collection("orders").document(orderId)
            .collection("items")
            .addSnapshotListener { snap, e ->
                if (e != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.map { d ->
                    val m = d.data ?: emptyMap<String, Any?>()
                    CartItem(
                        productId = d.id,
                        name = m["name"] as? String ?: "",
                        unitPrice = (m["unitPrice"] as? Number)?.toLong() ?: 0L,
                        qty = (m["qty"] as? Number)?.toInt() ?: 0
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** 상품 1개 추가(이미 있으면 qty+1) */
    suspend fun addOne(orderId: String, p: Product) {
        val ref = db.collection("orders").document(orderId)
            .collection("items").document(p.id)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val prev = (snap.getLong("qty") ?: 0L).toInt()
            tx.set(ref, mapOf(
                "name" to p.name,
                "unitPrice" to p.price,
                "qty" to prev + 1
            ), com.google.firebase.firestore.SetOptions.merge())
        }.await()
    }

    /** 수량 변경 (0이면 삭제) */
    suspend fun setQty(orderId: String, productId: String, qty: Int) {
        val ref = db.collection("orders").document(orderId)
            .collection("items").document(productId)
        if (qty <= 0) {
            ref.delete().await()
        } else {
            ref.set(mapOf("qty" to qty), com.google.firebase.firestore.SetOptions.merge()).await()
        }
    }

    /** branches/{branchId}.name → 없으면 branchId 반환 */
    private suspend fun fetchBranchName(branchId: String): String {
        val snap = db.collection("branches").document(branchId).get().await()
        val name = snap.getString("name")?.trim()
        return if (!name.isNullOrEmpty()) name else branchId
    }

    /** 현재 사용자 DRAFT 주문 get or create → orderId 반환 (지사명 디노멀라이즈 포함) */
    suspend fun getOrCreateDraft(): String {
        val u = uid()
        val existing = db.collection("orders")
            .whereEqualTo("ownerUid", u)
            .whereEqualTo("status", OrderStatus.DRAFT.name)
            .limit(1)
            .get().await()
        if (!existing.isEmpty) return existing.documents.first().id

        val branchId = fetchBranchId()
        val branchName = fetchBranchName(branchId)
        val data = mapOf(
            "ownerUid" to u,
            "branchId" to branchId,
            "branchName" to branchName,
            "branchName_lower" to branchName.lowercase(),
            "status" to OrderStatus.DRAFT.name,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return db.collection("orders").add(data).await().id
    }

    /** 상품 1개 추가/수량 변경은 기존 그대로… */

    /** 주문 확정: 합계 계산 + (안전망) 지사명 보정 */
    suspend fun place(orderId: String) {
        // 합계 계산
        val itemsSnap = db.collection("orders").document(orderId)
            .collection("items").get().await()
        val total = itemsSnap.documents.sumOf { d ->
            val m = d.data ?: emptyMap<String, Any?>()
            val price = (m["unitPrice"] as? Number)?.toLong() ?: 0L
            val qty = (m["qty"] as? Number)?.toInt() ?: 0
            price * qty
        }
        val itemsCount = itemsSnap.size()

        // 지사명 안전 보정
        val orderRef = db.collection("orders").document(orderId)
        val orderSnap = orderRef.get().await()
        val branchId = orderSnap.getString("branchId") ?: fetchBranchId()
        val branchName = orderSnap.getString("branchName") ?: fetchBranchName(branchId)

        orderRef.set(
            mapOf(
                "status" to OrderStatus.PLACED.name,
                "placedAt" to FieldValue.serverTimestamp(),
                "totalAmount" to total,
                "itemsCount" to itemsCount,
                "branchName" to branchName,
                "branchName_lower" to branchName.lowercase()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }
}
