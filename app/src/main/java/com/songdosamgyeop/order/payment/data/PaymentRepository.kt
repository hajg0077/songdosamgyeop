package com.songdosamgyeop.order.ui.payment.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PortOne 결제 검증 onCall 호출 전담 Repository.
 * - Functions: verifyPortOnePayment(orderId, merchantUid, impUid)
 * - 결과(ok/paidAt/method/impUid/message) 반환
 */
interface PaymentRepository {
    data class VerifyResult(
        val ok: Boolean,
        val message: String? = null
    )

    suspend fun verifyAndApply(
        orderId: String,
        merchantUid: String,
        impUid: String
    ): VerifyResult
}

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions
) : PaymentRepository {

    override suspend fun verifyAndApply(
        orderId: String,
        merchantUid: String,
        impUid: String
    ): PaymentRepository.VerifyResult {
        val res = functions
            .getHttpsCallable("verifyPortOnePayment")
            .call(
                hashMapOf(
                    "orderId" to orderId,
                    "merchantUid" to merchantUid,
                    "impUid" to impUid
                )
            ).await()

        val data = res.data as Map<*, *>
        val ok = data["ok"] as? Boolean ?: false
        val msg = data["message"] as? String
        return PaymentRepository.VerifyResult(ok = ok, message = msg)
    }
}