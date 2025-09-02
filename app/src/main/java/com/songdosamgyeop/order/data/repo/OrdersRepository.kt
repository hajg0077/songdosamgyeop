package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.ktx.toObject
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.CartItem
import com.songdosamgyeop.order.data.model.CartLine
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.Timestamp
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
        val doc = db.collection("users").document(u).get().await()
        return doc.getString("branchId") ?: error("users/$u 에 branchId 없음")
    }

    /** 브랜드별 DRAFT 주문을 하나 만들거나(없으면) 재사용한다. */
    suspend fun createDraftForBrand(brandId: String): String {
        val u = uid()
        val branchId = fetchBranchId()

        // 기존 동일 브랜드 DRAFT가 있으면 재사용 (중복 방지)
        val existing = db.collection("orders")
            .whereEqualTo("ownerUid", u)
            .whereEqualTo("branchId", branchId)
            .whereEqualTo("brandId", brandId)
            .whereEqualTo("status", "DRAFT")
            .limit(1).get().await()
        if (!existing.isEmpty) return existing.documents.first().id

        val data = mapOf(
            "ownerUid" to u,
            "branchId" to branchId,
            "brandId" to brandId,
            "status" to "DRAFT",
            "createdAt" to FieldValue.serverTimestamp()
        )
        return db.collection("orders").add(data).await().id
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


    /** 주문의 items 서브컬렉션을 라인으로 채운다(덮어쓰기). */
    suspend fun putItems(orderId: String, lines: List<CartLine>) {
        val batch: WriteBatch = db.batch()
        val items = db.collection("orders").document(orderId).collection("items")
        // 같은 productId를 docId로 쓰면 수량 변경 시 덮어쓰기 편함
        lines.forEach { l ->
            val ref = items.document(l.productId)
            val m = mapOf(
                "productId" to l.productId,
                "name" to l.name,
                "unitPrice" to l.unitPrice,
                "qty" to l.qty
            )
            batch.set(ref, m, SetOptions.merge())
        }
        batch.commit().await()
    }

    /** 합계/개수 계산 후 주문 확정(PLACED) + placedAt 세팅 */
    suspend fun place(orderId: String) {
        val orderRef = db.collection("orders").document(orderId)
        val itemsSnap = orderRef.collection("items").get().await()
        val total = itemsSnap.documents.sumOf { d ->
            val price = (d.getLong("unitPrice") ?: 0L)
            val qty = (d.getLong("qty") ?: 0L)
            price * qty
        }
        val count = itemsSnap.size()

        orderRef.set(
            mapOf(
                "status" to "PLACED",
                "placedAt" to FieldValue.serverTimestamp(),
                "itemsCount" to count,
                "totalAmount" to total
            ),
            SetOptions.merge()
        ).await()
    }

    /**
     * 내(브랜치 사용자) 주문 히스토리 구독.
     * - 기본: PLACED만, 최신순(placedAt DESC)
     * - 기간 필터: [from, to) (둘 중 null 허용)
     */
    fun subscribeMyOrders(
        from: Timestamp? = null,
        to: Timestamp? = null
    ): Flow<List<com.songdosamgyeop.order.data.model.OrderRow>> = callbackFlow {
        val u = uid()
        var q: Query = db.collection("orders")
            .whereEqualTo("ownerUid", u)
            .whereEqualTo("status", OrderStatus.PLACED.name)

        if (from != null) q = q.whereGreaterThanOrEqualTo("placedAt", from)
        if (to != null)   q = q.whereLessThan("placedAt", to)

        q = q.orderBy("placedAt", Query.Direction.DESCENDING)

        val reg: ListenerRegistration = q.addSnapshotListener { snap, e ->
            if (e != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.map { d ->
                val m = d.data ?: emptyMap<String, Any?>()
                com.songdosamgyeop.order.data.model.OrderRow(
                    id = d.id,
                    branchId = m["branchId"] as? String ?: "-",
                    branchName = m["branchName"] as? String,
                    ownerUid  = m["ownerUid"] as? String ?: u,
                    status    = m["status"] as? String ?: "UNKNOWN",
                    placedAt  = m["placedAt"] as? com.google.firebase.Timestamp,
                    createdAt = m["createdAt"] as? com.google.firebase.Timestamp,
                    itemsCount = (m["itemsCount"] as? Number)?.toInt(),
                    totalAmount = (m["totalAmount"] as? Number)?.toLong()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    /** 주문 헤더(요약) 실시간 구독 (브랜치 읽기 전용) */
    fun observeOrderHeader(orderId: String): Flow<com.songdosamgyeop.order.data.model.OrderHeader?> = callbackFlow {
        val ref = db.collection("orders").document(orderId)
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { trySend(null); return@addSnapshotListener }
            if (snap == null || !snap.exists()) { trySend(null); return@addSnapshotListener }
            val m = snap.data ?: emptyMap<String, Any?>()
            trySend(
                com.songdosamgyeop.order.data.model.OrderHeader(
                    id = snap.id,
                    branchId = m["branchId"] as? String ?: "-",
                    ownerUid = m["ownerUid"] as? String ?: "",
                    status = m["status"] as? String ?: "UNKNOWN",
                    placedAt = m["placedAt"] as? com.google.firebase.Timestamp,
                    createdAt = m["createdAt"] as? com.google.firebase.Timestamp,
                    itemsCount = (m["itemsCount"] as? Number)?.toInt(),
                    totalAmount = (m["totalAmount"] as? Number)?.toLong()
                )
            )
        }
        awaitClose { reg.remove() }
    }

    /** 주문 아이템 실시간 구독 (브랜치 읽기 전용) */
    fun observeOrderItems(orderId: String): Flow<List<com.songdosamgyeop.order.data.model.OrderLine>> = callbackFlow {
        val ref = db.collection("orders").document(orderId).collection("items")
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.map { d ->
                val m = d.data ?: emptyMap<String, Any?>()
                com.songdosamgyeop.order.data.model.OrderLine(
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
}
