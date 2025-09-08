package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class HqDashboardRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /** 오늘 주문 수/금액 합계 */
    suspend fun getTodayOrdersSummary(): Pair<Int, Long> {
        val tz = java.util.TimeZone.getTimeZone("Asia/Seoul")
        val cal = java.util.Calendar.getInstance(tz).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startTs = com.google.firebase.Timestamp(cal.time)

        val snap = db.collection("orders")
            .whereGreaterThanOrEqualTo("placedAt", startTs)
            .get()
            .await()

        val count = snap.size()
        val sum = snap.documents.sumOf { it.getLong("totalAmount") ?: 0L }
        return count to sum
    }

    /** 승인 대기 신청서 수 */
    suspend fun getPendingRegistrations(): Int {
        val snap = db.collection("registrations")
            .whereEqualTo("status", "PENDING")
            .get()
            .await()
        return snap.size()
    }

    /** 진행 중 주문 수 */
    suspend fun getActiveOrders(): Int {
        val snap = db.collection("orders")
            .whereIn("status", listOf("PENDING", "APPROVED", "SHIPPED"))
            .get()
            .await()
        return snap.size()
    }
}