package com.songdosamgyeop.order.data.repo

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : OrderRepository {

    override suspend fun markPayment(orderId: String, update: OrderPaymentUpdate) {
        val patch = hashMapOf<String, Any?>(
            "merchantUid" to update.merchantUid,
            "impUid" to update.impUid,
            "paymentStatus" to update.paymentStatus,
            "paidAt" to update.paidAt,
            "paymentMethod" to update.method,
            "paymentTxId" to update.txId,
            "paymentMessage" to update.message,
            // 요청시각(서버 기준) 보정/기록
            "requestedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        db.collection("orders").document(orderId)
            .update(patch)
            .await()
    }
}