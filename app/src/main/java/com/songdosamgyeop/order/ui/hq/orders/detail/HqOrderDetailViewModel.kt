package com.songdosamgyeop.order.ui.hq.orders.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.songdosamgyeop.order.data.model.OrderLine
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
        val brandId: String?,   // 데이터 소스가 null일 수 있어 안전하게 nullable 유지
        val status: String?,
        val itemsCount: Int?,
        val totalAmount: Long?,
        val placedAt: Date?,
        val createdAt: Date?
    )

    /** 라인/헤더 등 섹션 지원을 위한 ItemRow (지금은 Line만 사용) */
    sealed class ItemRow {
        data class Line(val line: OrderLine) : ItemRow() {
            val name: String get() = line.name
            val unitPrice: Long get() = line.unitPrice
            val qty: Int get() = line.qty
            val lineTotal: Long get() = line.unitPrice * line.qty
        }
        // 필요 시 섹션 헤더 등 추가 가능: data class Section(val title: String) : ItemRow()
    }

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

    /** 아이템 구독: OrderLine → ItemRow.Line 로 변환 */
    val items = ordersRepo.observeOrderItems(orderId).map { list ->
        list.map { ItemRow.Line(it) }
    }.asLiveData()
}