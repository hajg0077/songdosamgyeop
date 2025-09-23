package com.songdosamgyeop.order.ui.payment.data

import com.google.firebase.Timestamp
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.data.repo.OrderPaymentUpdate
import com.songdosamgyeop.order.data.repo.OrderRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PortOne 결제 검증 + orders/{orderId} 반영까지 단일 책임.
 * Functions: verifyPortOnePayment(orderId, merchantUid, impUid)
 */
interface PaymentRepository {
    data class VerifyResult(val ok: Boolean, val message: String? = null)

    suspend fun verifyAndApply(
        orderId: String,
        merchantUid: String,
        impUid: String
    ): VerifyResult
}

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val orderRepo: OrderRepository
) : PaymentRepository {

    override suspend fun verifyAndApply(
        orderId: String,
        merchantUid: String,
        impUid: String
    ): PaymentRepository.VerifyResult {
        val res = functions
            .getHttpsCallable("verifyPortOnePayment")
            .call(hashMapOf("orderId" to orderId, "merchantUid" to merchantUid, "impUid" to impUid))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = res.getData() as? Map<*, *> ?: emptyMap<String, Any?>()  // ⚠️ getData() 사용
        val ok = (data["ok"] as? Boolean) == true
        val method = data["method"] as? String
        val paidAtMs = (data["paidAt"] as? Number)?.toLong() ?: 0L
        val txId = (data["impUid"] as? String) ?: impUid
        val message = data["message"] as? String

        val ts = if (ok && paidAtMs > 0L) {
            val sec = paidAtMs / 1000
            val nanos = ((paidAtMs % 1000) * 1_000_000).toInt()
            Timestamp(sec, nanos)
        } else null

        orderRepo.markPayment(
            orderId,
            OrderPaymentUpdate(
                merchantUid = merchantUid,
                impUid = impUid,
                paymentStatus = if (ok) "PAID" else "FAILED",
                paidAt = ts,
                method = method,
                txId = txId,
                message = message
            )
        )
        return PaymentRepository.VerifyResult(ok = ok, message = message)
    }
}