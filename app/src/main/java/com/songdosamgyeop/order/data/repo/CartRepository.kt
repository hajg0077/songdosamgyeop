package com.songdosamgyeop.order.data.repo

import com.songdosamgyeop.order.data.model.CartItem
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor() {

    private val _items = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val items: StateFlow<List<CartItem>> =
        _items.map { it.values.sortedBy { item -> item.productName } }
            .stateIn(scope = kotlinx.coroutines.GlobalScope, started = SharingStarted.Eagerly, initialValue = emptyList())

    val totalAmount: StateFlow<Long> =
        items.map { it.sumOf { c -> c.amount } }
            .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, 0L)

    val totalQty: StateFlow<Int> =
        items.map { it.sumOf { c -> c.qty } }
            .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, 0)

    fun add(p: Product) {
        val cur = _items.value.toMutableMap()
        val existing = cur[p.id]
        cur[p.id] = if (existing == null)
            CartItem(p.id, p.name, p.brandId, p.price, 1)
        else existing.copy(qty = existing.qty + 1)
        _items.value = cur
    }

    fun setQty(productId: String, qty: Int) {
        val cur = _items.value.toMutableMap()
        val ex = cur[productId] ?: return
        if (qty <= 0) cur.remove(productId) else cur[productId] = ex.copy(qty = qty)
        _items.value = cur
    }

    fun remove(productId: String) {
        val cur = _items.value.toMutableMap()
        cur.remove(productId)
        _items.value = cur
    }

    fun clear() {
        _items.value = emptyMap()
    }
}
