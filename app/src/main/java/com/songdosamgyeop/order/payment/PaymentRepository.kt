package com.songdosamgyeop.order.payment

import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.data.repo.OrderPaymentUpdate
import com.songdosamgyeop.order.data.repo.OrderRepository
import kotlinx.coroutines.tasks.await

data class VerifyParams(
    val orderId: String,
    val merchantUid: String,
    val impUid: String
)

interface PaymentRepository {
    suspend fun verifyAndApply(params: VerifyParams)
}

class PaymentRepositoryImpl(
    private val functions: FirebaseFunctions,
    private val orderRepository: OrderRepository
) : PaymentRepository {

    override suspend fun verifyAndApply(params: VerifyParams) {
        val fn = functions
            .getHttpsCallable("verifyPortOnePayment")
            .call(hashMapOf(
                "orderId" to params.orderId,
                "merchantUid" to params.merchantUid,
                "impUid" to params.impUid
            ))
            .await()

        val data = fn.getData() as Map<*, *>
        val ok = data["ok"] as Boolean
        val method = data["method"] as? String
        val paidAt = (data["paidAt"] as? Long) ?: 0L
        val txId = data["impUid"] as? String
        val message = data["message"] as? String

        val upd = OrderPaymentUpdate(
            merchantUid = params.merchantUid,
            impUid = params.impUid,
            paymentStatus = if (ok) "PAID" else "FAILED",
            paidAt = if (ok && paidAt > 0) com.google.firebase.Timestamp(paidAt / 1000, ((paidAt % 1000)*1_000_000).toInt()) else null,
            method = method,
            txId = txId,
            message = message
        )
        orderRepository.markPayment(params.orderId, upd)
    }
}