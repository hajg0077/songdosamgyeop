package com.songdosamgyeop.order.data.model

import com.google.firebase.Timestamp

/** HQ 주문 상세 헤더(상단 요약용) */
data class OrderHeader(
    val id: String,
    val brandId: String,
    val branchId: String,
    val branchName: String?,
    val ownerUid: String,
    val status: String,
    val placedAt: Timestamp?,
    val createdAt: Timestamp?,
    val itemsCount: Int?,
    val totalAmount: Long?
)

/** 주문 상세의 라인아이템 */
data class OrderLine(
    val productId: String,
    val name: String,
    val unitPrice: Long,
    val qty: Int
) {
    val lineTotal: Long get() = unitPrice * qty
}