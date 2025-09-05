package com.songdosamgyeop.order.ui.hq.orders

import com.songdosamgyeop.order.data.model.OrderRow
import java.util.Date

/** 화면 표시용: branchLabel(지사명 or 지사ID) 포함 */

data class OrderDisplayRow(
    val id: String?,
    val brandId: String?,
    val branchName: String?,
    val status: String?,
    val itemsCount: Int?,
    val totalAmount: Long?,
    val placedAt: Date?,     // ✅ Date로 저장
    val createdAt: Date?     // ✅ Date로 저장
) {
    companion object {
        fun from(row: OrderRow) = OrderDisplayRow(
            id = row.id,
            brandId = row.branchId,
            branchName = row.branchName,
            status = row.status,
            itemsCount = row.itemsCount,
            totalAmount = row.totalAmount,
            placedAt = row.placedAt?.toDate(),   // ✅ 여기서 변환
            createdAt = row.createdAt?.toDate()  // ✅ 여기서 변환
        )
    }
}