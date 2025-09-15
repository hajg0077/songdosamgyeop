package com.songdosamgyeop.order.data.model

data class Product(
    val id: String = "",          // docId
    val name: String = "",
    val price: Long = 0L,

    val brandId: String = "COMMON", // "SONGDO" | "BULBAEK" | "HONG" | "COMMON"
    val category: String = "",

    val unit: String? = null,     // 옵션
    val sku: String? = null,      // 옵션
    val nameLower: String = "",   // 검색용
    val active: Boolean = true
)