package com.songdosamgyeop.order.data.model

/** 장바구니(초안주문) 항목 */
data class CartItem(
    val productId: String,
    val name: String,
    val unitPrice: Long,
    val qty: Int
) {
    val lineTotal: Long get() = unitPrice * qty
}