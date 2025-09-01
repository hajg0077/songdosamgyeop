package com.songdosamgyeop.order.data.model


/** 상품 도메인 모델 (필요 필드만) */
data class Product(
    val id: String,
    val name: String,
    val price: Long,           // KRW
    val active: Boolean
)