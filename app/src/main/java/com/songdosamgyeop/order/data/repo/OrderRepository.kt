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