package com.songdosamgyeop.order.ui.hq.orders.detail

/** 서버 전이 규칙과 동일하게 클라이언트 메뉴 노출을 결정 */
object OrderMenuPolicy {
    fun visibleActions(current: String?): Set<String> {
        return when (current?.uppercase()) {
            "PENDING"  -> setOf("APPROVED", "REJECTED")
            "APPROVED" -> setOf("SHIPPED")
            "SHIPPED"  -> setOf("DELIVERED")
            else       -> emptySet() // REJECTED, DELIVERED 등 완료 상태
        }
    }
}