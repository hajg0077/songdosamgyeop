package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.data.model.OrderHeader
import com.songdosamgyeop.order.data.model.OrderLine
import com.songdosamgyeop.order.data.model.OrderRow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.security.Timestamp
import javax.inject.Inject

class HqOrdersRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /**
     * 주문 헤더(상단 요약) 실시간 구독
     * @param orderId 주문 문서 ID
     */
    fun observeOrderHeader(orderId: String): Flow<OrderHeader?> = callbackFlow {
        val ref = db.collection("orders").document(orderId)
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { Log.e("HqOrdersRepo", "header listen error", e); trySend(null); return@addSnapshotListener }
            if (snap == null || !snap.exists()) { trySend(null); return@addSnapshotListener }
            val m = snap.data ?: emptyMap<String, Any?>()
            trySend(
                OrderHeader(
                    id = snap.id,
                    branchId = m["branchId"] as? String ?: "-",
                    ownerUid = m["ownerUid"] as? String ?: "-",
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

    /**
     * 주문 라인아이템 실시간 구독
     * @param orderId 주문 문서 ID
     */
    fun observeOrderItems(orderId: String): Flow<List<OrderLine>> = callbackFlow {
        val ref = db.collection("orders").document(orderId).collection("items")
        val reg = ref.addSnapshotListener { snap, e ->
            if (e != null) { Log.e("HqOrdersRepo", "items listen error", e); trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.map { d ->
                val m = d.data ?: emptyMap<String, Any?>()
                OrderLine(
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

    /**
     * HQ 주문 리스트 구독
     * - branchNamePrefix: 접두 검색(서버)
     * - 날짜 범위: prefix가 있을 땐 Firestore 제약으로 서버 필터 불가 → 클라 후처리
     */
    fun subscribeOrders(
        status: String = "PLACED",
        branchNamePrefix: String? = null,
        from: Timestamp? = null,
        to: Timestamp? = null
    ): Flow<List<OrderRow>> = callbackFlow {
        val orders = db.collection("orders")
        val prefix = branchNamePrefix?.trim().orEmpty()

        val q: Query = if (prefix.isNotEmpty()) {
            // 서버 접두 검색: status ==, orderBy branchName_lower → placedAt desc
            orders.whereEqualTo("status", status)
                .orderBy("branchName_lower")
                .orderBy("placedAt", Query.Direction.DESCENDING)
                .startAt(prefix.lowercase())
                .endAt(prefix.lowercase() + '\uf8ff')
        } else {
            // 날짜 범위 서버 필터 + 최신순
            var base = orders.whereEqualTo("status", status)
            if (from != null) base = base.whereGreaterThanOrEqualTo("placedAt", from)
            if (to != null)   base = base.whereLessThan("placedAt", to)
            base.orderBy("placedAt", Query.Direction.DESCENDING)
        }

        val reg = q.addSnapshotListener { snap, e ->
            if (e != null) { trySend(emptyList()); return@addSnapshotListener }
            var list = snap?.documents?.map { d ->
                val m = d.data ?: emptyMap<String, Any?>()
                OrderRow(
                    id = d.id,
                    branchId = m["branchId"] as? String ?: "-",
                    branchName = m["branchName"] as? String,
                    ownerUid  = m["ownerUid"] as? String ?: "-",
                    status    = m["status"] as? String ?: "UNKNOWN",
                    placedAt  = m["placedAt"] as? com.google.firebase.Timestamp,
                    createdAt = m["createdAt"] as? com.google.firebase.Timestamp,
                    itemsCount = (m["itemsCount"] as? Number)?.toInt(),
                    totalAmount = (m["totalAmount"] as? Number)?.toLong()
                )
            } ?: emptyList()

            // ✅ prefix 검색일 때는 날짜 범위를 클라에서 후처리(제약 회피)
            if (prefix.isNotEmpty() && (from != null || to != null)) {
                list = list.filter { row ->
                    val t = row.placedAt ?: return@filter false
                    val okFrom = from?.let { t >= it } ?: true
                    val okTo   = to?.let   { t < it }  ?: true
                    okFrom && okTo
                }
            }

            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}
