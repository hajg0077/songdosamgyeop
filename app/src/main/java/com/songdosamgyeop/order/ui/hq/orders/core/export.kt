package com.songdosamgyeop.order.core.export

import com.songdosamgyeop.order.ui.hq.orders.OrderDisplayRow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HQ 주문 모니터링 CSV 생성기.
 * - 컬럼: orderId, branch, status, when, itemsCount, totalAmountKRW
 * - Excel 호환을 위해 Fragment 쪽에서 UTF-8 BOM을 함께 기록하는 것을 권장.
 */
object OrdersCsvExporter {

    /** 리스트를 CSV 문자열로 직렬화 */
    fun buildCsv(
        rows: List<OrderDisplayRow>,
        locale: Locale = Locale.KOREA
    ): String {
        val nf = NumberFormat.getNumberInstance(locale)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", locale)

        val header = listOf(
            "orderId", "branch", "status", "when", "itemsCount", "totalAmountKRW"
        ).joinToString(",")

        val body = rows.joinToString("\n") { r ->
            val whenStr = r.placedAtMs?.let { sdf.format(Date(it)) }
                ?: r.createdAtMs?.let { sdf.format(Date(it)) } ?: ""
            listOf(
                esc(r.id),
                esc(r.branchLabel),
                esc(r.status),
                esc(whenStr),
                r.itemsCount?.toString() ?: "",
                r.totalAmount?.let { nf.format(it) } ?: ""
            ).joinToString(",")
        }

        return buildString {
            append(header).append('\n')
            if (body.isNotEmpty()) append(body).append('\n')
        }
    }

    /** CSV 안전 이스케이프(따옴표/콤마/개행 처리) */
    private fun esc(raw: String): String {
        if (raw.isEmpty()) return ""
        val needsQuote = raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val doubled = raw.replace("\"", "\"\"")
        return if (needsQuote) "\"$doubled\"" else doubled
    }
}
