package com.songdosamgyeop.order.data.model

data class CartItem(
    val productId: String,
    val productName: String,
    val brandId: String?,
    val unitPrice: Long,
    val qty: Int,
    val unit: String? = null
) {
    val amount: Long get() = unitPrice * qty
}
