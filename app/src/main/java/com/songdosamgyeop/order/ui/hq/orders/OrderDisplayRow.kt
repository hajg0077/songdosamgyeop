// ui/hq/orders/OrderDisplayRow.kt
package com.songdosamgyeop.order.ui.hq.orders

import com.songdosamgyeop.order.data.model.OrderRow
import java.util.Date

/** 화면 표시용: branchLabel(지사명 or 지사ID) 포함 */
data class OrderDisplayRow(
    val id: String,
    val brandId: String?,        // 브랜드 ID
    val branchId: String?,       // 지사 ID (UI fallback용)
    val branchName: String?,     // 지사명
    val status: String?,
    val itemsCount: Int?,
    val totalAmount: Long?,
    val placedAt: Date?,         // ✅ Date로 변환된 값
    val createdAt: Date?         // ✅ Date로 변환된 값
) {
    /** UI에서 편하게 쓰는 라벨 (지사명 우선 → 지사ID) */
    val branchLabel: String
        get() = branchName ?: branchId ?: "-"

    companion object {
        fun from(row: OrderRow) = OrderDisplayRow(
            id         = row.id ?: "-",
            brandId    = row.brandId,
            branchId   = row.branchId,
            branchName = row.branchName,
            status     = row.status,
            itemsCount = row.itemsCount,
            totalAmount= row.totalAmount,
            placedAt   = row.placedAt?.toDate(),   // ✅ 여기서 Timestamp→Date
            createdAt  = row.createdAt?.toDate()
        )
    }
}