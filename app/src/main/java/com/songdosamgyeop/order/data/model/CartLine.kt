package com.songdosamgyeop.order.data.model

/** 장바구니 아이템 (주문 확정 시 브랜드별로 분할 생성) */
data class CartLine(
    val productId: String,
    val brandId: String,      // ★ 주문 분할 키
    val name: String,
    val unitPrice: Long,
    val qty: Int
)