package com.songdosamgyeop.order.ui.hq.orders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.data.repo.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.map

/** 주문 상세: 헤더 + 아이템 구독 */
@HiltViewModel
class HqOrderDetailViewModel @Inject constructor(
    private val ordersRepo: OrdersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String =
        savedStateHandle.get<String>("orderId") ?: savedStateHandle.get<String>("id") ?: ""

    data class Header(
        val id: String,
        val branchId: String,
        val brandId: String?,
        val status: String?,
        val itemsCount: Int?,
        val totalAmount: Long?,
        val placedAt: Date?,
        val createdAt: Date?
    )

    data class ItemRow(
        val name: String,
        val unitPrice: Long,
        val qty: Int,
        val lineTotal: Long
    )

    /** 헤더 구독 (Timestamp → Date 변환) */
    val header = ordersRepo.observeOrderHeader(orderId).map { o ->
        if (o == null) null else Header(
            id = o.id,
            branchId = o.branchId,
            brandId = o.brandId,
            status = o.status,
            itemsCount = o.itemsCount,
            totalAmount = o.totalAmount,
            placedAt = o.placedAt?.toDate(),
            createdAt = o.createdAt?.toDate()
        )
    }.asLiveData()

    /** 아이템 구독 */
    val items = ordersRepo.observeOrderItems(orderId).map { list ->
        list.map { i ->
            ItemRow(
                name = i.name,
                unitPrice = i.unitPrice,
                qty = i.qty,
                lineTotal = i.unitPrice * i.qty
            )
        }
    }.asLiveData()
}