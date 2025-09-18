package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class OrderPaymentUpdate(
    val merchantUid: String,
    val impUid: String?,
    val paymentStatus: String, // READY/PAID/FAILED/CANCELED
    val paidAt: Timestamp? = null,
    val method: String? = null,
    val txId: String? = null,
    val message: String? = null
)

interface OrderRepository {
    suspend fun markPayment(orderId: String, upd: OrderPaymentUpdate)
}

class OrderRepositoryImpl(
    private val db: FirebaseFirestore
) : OrderRepository {

    override suspend fun markPayment(orderId: String, upd: OrderPaymentUpdate) {
        val ref = db.collection("orders").document(orderId)
        val map = hashMapOf<String, Any?>(
            "merchantUid" to upd.merchantUid,
            "paymentStatus" to upd.paymentStatus,
            "paymentTxId" to (upd.txId ?: upd.impUid),
            "paymentMethod" to upd.method,
            "paidAt" to upd.paidAt,
            "paymentMessage" to upd.message
        )
        ref.update(map).await()
    }
}