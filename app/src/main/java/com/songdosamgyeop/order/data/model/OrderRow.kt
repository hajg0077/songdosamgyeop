package com.songdosamgyeop.order.data.model

import com.google.firebase.Timestamp

/** HQ 모니터링용 주문 행 모델(리스트에 표시할 최소 정보) */
data class OrderRow(
    val id: String,
    val branchId: String,
    val ownerUid: String,
    val status: String,
    val placedAt: Timestamp?,   // PLACED일 때 사용
    val createdAt: Timestamp?,  // DRAFT일 때 대비
    val itemsCount: Int?,       // 선택
    val totalAmount: Long?      // 선택 (KRW)
)