package com.songdosamgyeop.order.ui.hq.orders.core

import android.content.ContentResolver
import android.net.Uri
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    /** UTF-8 BOM 포함, 한국 엑셀 호환 */
    fun exportOrders(
        resolver: ContentResolver,
        uri: Uri,
        rows: List<com.songdosamgyeop.order.ui.hq.orders.OrderDisplayRow>
    ) {
        resolver.openOutputStream(uri)?.use { os ->
            // BOM
            os.write(0xEF); os.write(0xBB); os.write(0xBF)

            OutputStreamWriter(os, Charsets.UTF_8).use { w ->
                w.appendLine("주문일시,브랜드,지사,상태,상품개수,합계,주문ID")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
                    .apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }

                rows.forEach { r ->
                    val whenStr = r.placedAt?.let(sdf::format)
                        ?: r.createdAt?.let(sdf::format) ?: "-"

                    val brand  = r.brandId ?: "-"
                    val branch = r.branchName ?: r.branchId ?: "-"
                    val status = r.status ?: "-"
                    val count  = r.itemsCount?.toString() ?: "-"
                    val total  = r.totalAmount?.toString() ?: "-"
                    val id     = r.id

                    w.appendLine(listOf(whenStr, brand, branch, status, count, total, id).joinToCsv())
                }
            }
        } ?: error("OutputStream is null")
    }

    private fun List<String>.joinToCsv(): String = buildString {
        this@joinToCsv.forEachIndexed { i, raw ->
            if (i > 0) append(',')
            append(raw.escapeCsv())
        }
    }
    private fun String.escapeCsv(): String {
        val needsQuote = contains(',') || contains('"') || contains('\n') || contains('\r')
        val s = replace("\"", "\"\"")
        return if (needsQuote) "\"$s\"" else s
    }
}
