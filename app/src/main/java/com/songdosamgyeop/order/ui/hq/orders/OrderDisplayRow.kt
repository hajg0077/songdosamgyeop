package com.songdosamgyeop.order.ui.hq.orders

import com.songdosamgyeop.order.data.model.OrderRow

/** 화면 표시용: branchLabel(지사명 or 지사ID) 포함 */

data class OrderDisplayRow(
    val id: String,
    val branchLabel: String,   // ✅ branchName 우선, 없으면 branchId
    val status: String,
    val placedAtMs: Long?,
    val createdAtMs: Long?,
    val itemsCount: Int?,
    val totalAmount: Long?
) {
    companion object {
        fun from(row: OrderRow) = OrderDisplayRow(
            id = row.id,
            branchLabel = row.branchName?.takeIf { it.isNotBlank() } ?: row.branchId,
            status = row.status,
            placedAtMs = row.placedAt?.toDate()?.time,
            createdAtMs = row.createdAt?.toDate()?.time,
            itemsCount = row.itemsCount,
            totalAmount = row.totalAmount
        )
    }
}