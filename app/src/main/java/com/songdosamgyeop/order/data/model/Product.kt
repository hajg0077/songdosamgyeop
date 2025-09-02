package com.songdosamgyeop.order.data.model


/** 재고/발주용 상품 */
data class Product(
    val id: String = "",             // Firestore docId 저장용(필드엔 없음)
    val name: String = "",
    val name_lower: String = "",
    val sku: String = "",
    val unit: String = "",           // 팩/봉/BOX 등
    val price: Long = 0L,
    val brandId: String = "COMMON",  // SONGDO | BULBAEK | HONG | COMMON
    val category: String = "",       // 육류/채소/소스...
    val active: Boolean = true
)