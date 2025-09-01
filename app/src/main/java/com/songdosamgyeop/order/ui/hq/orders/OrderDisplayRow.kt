package com.songdosamgyeop.order.ui.hq.orders

import com.songdosamgyeop.order.data.model.OrderRow

/** 화면 표시용: branchLabel(지사명 or 지사ID) 포함 */
data class OrderDisplayRow(
    val id: String,
    val branchLabel: String,   // "인천송도점" 또는 fallback으로 "BR_000123"
    val status: String,
    val placedAtMs: Long?,     // 정렬/표시 편의
    val createdAtMs: Long?,
    val itemsCount: Int?,
    val totalAmount: Long?
) {
    companion object {
        fun from(row: OrderRow, nameMap: Map<String, String>) = OrderDisplayRow(
            id = row.id,
            branchLabel = nameMap[row.branchId] ?: row.branchId,
            status = row.status,
            placedAtMs = row.placedAt?.toDate()?.time,
            createdAtMs = row.createdAt?.toDate()?.time,
            itemsCount = row.itemsCount,
            totalAmount = row.totalAmount
        )
    }
}
