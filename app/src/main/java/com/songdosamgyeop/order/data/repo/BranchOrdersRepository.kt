package com.songdosamgyeop.order.data.repo

import com.google.firebase.Timestamp
import com.songdosamgyeop.order.data.model.CartItem

interface BranchOrdersRepository {
    /**
     * 통합주문 1건 생성.
     * @return 생성된 orderId(document id)
     */
    suspend fun createOrder(
        items: List<CartItem>,
        branchId: String,
        branchName: String,
        note: String? = null,
        requestedAt: Timestamp? = null
    ): String
}