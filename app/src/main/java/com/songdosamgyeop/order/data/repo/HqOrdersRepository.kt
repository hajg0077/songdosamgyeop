package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.data.model.OrderHeader
import com.songdosamgyeop.order.data.model.OrderLine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
}
