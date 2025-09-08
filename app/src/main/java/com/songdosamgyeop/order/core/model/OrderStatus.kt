package com.songdosamgyeop.order.core.model

enum class OrderStatus { PENDING, APPROVED, REJECTED, SHIPPED, DELIVERED }

object OrderStatusHelper {
    /** 현재 상태에서 사용자가 누를 수 있는 다음 상태 목록(UX용) */
    fun nextCandidates(current: OrderStatus): List<OrderStatus> = when (current) {
        OrderStatus.PENDING   -> listOf(OrderStatus.APPROVED, OrderStatus.REJECTED)
        OrderStatus.APPROVED  -> listOf(OrderStatus.SHIPPED)
        OrderStatus.SHIPPED   -> listOf(OrderStatus.DELIVERED)
        else -> emptyList()
    }
}