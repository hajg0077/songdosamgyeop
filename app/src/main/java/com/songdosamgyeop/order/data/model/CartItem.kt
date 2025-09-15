package com.songdosamgyeop.order.data.model

data class CartItem(
    val productId: String,
    val productName: String,
    val brandId: String? = null,
    val unitPrice: Long,
    val qty: Int
) {
    val amount: Long get() = unitPrice * qty
}