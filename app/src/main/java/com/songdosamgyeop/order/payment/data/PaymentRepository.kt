package com.songdosamgyeop.order.payment.data

import com.google.firebase.Timestamp
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.data.repo.OrderPaymentUpdate
import com.songdosamgyeop.order.data.repo.OrderRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.get

/**
 * KG 이니시스 결제 검증 + orders/{orderId} 반영.
 * Cloud Functions HTTPS Callable: verifyInicisPayment
 *
 * 서버 응답 기대 형식:
 *  {
 *    ok: Boolean,
 *    message?: String,
 *    method?: String,     // "Card" | "VBANK" | ...
 *    paidAt?: Long,       // epoch millis
 *    tid?: String,        // INICIS TID
 *    oid?: String         // merchantUid (우리 주문번호)
 *  }
 */
interface PaymentRepository {
    data class VerifyResult(val ok: Boolean, val message: String? = null)

    suspend fun verifyAndApply(
        orderId: String,     // Firestore 문서 ID
        merchantUid: String, // = oid (우리 주문번호)
        txId: String         // = tid (이니시스 거래 ID)
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
        txId: String
    ): PaymentRepository.VerifyResult {
        // ✅ 이니시스 검증 함수 호출
        val res = functions
            .getHttpsCallable(Env.FUNCTIONS_VERIFY_INICIS)
            .call(hashMapOf("orderId" to orderId, "merchantUid" to merchantUid, "tid" to txId))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = res.getData() as? Map<*, *> ?: emptyMap<String, Any?>()
        val ok = (data["ok"] as? Boolean) == true
        val method = data["method"] as? String
        val paidAtMs = (data["paidAt"] as? Number)?.toLong() ?: 0L
        val tid = (data["tid"] as? String) ?: txId
        val message = data["message"] as? String

        val ts = if (ok && paidAtMs > 0L) {
            val sec = paidAtMs / 1000
            val nanos = ((paidAtMs % 1000) * 1_000_000).toInt()
            Timestamp(sec, nanos)
        } else null

        orderRepo.markPayment(
            orderId,
            OrderPaymentUpdate(
                merchantUid = merchantUid,   // oid
                impUid = tid,                // 필드명 호환을 위해 impUid 자리에 tid 저장
                paymentStatus = if (ok) "PAID" else "FAILED",
                paidAt = ts,
                method = method,
                txId = tid,
                message = message
            )
        )
        return PaymentRepository.VerifyResult(ok = ok, message = message)
    }
}