// data/mapping/ProductMapping.kt
package com.songdosamgyeop.order.data.mapping

import com.google.firebase.firestore.DocumentSnapshot
import com.songdosamgyeop.order.data.model.Product
import java.util.Locale

fun Product.toDoc(): Map<String, Any?> = mapOf(
    "name" to name,
    "nameLower" to name.lowercase(Locale.ROOT),
    "price" to price,
    "brandId" to brandId,
    "category" to category,
    "unit" to unit,
    "sku" to sku,
    "active" to active
)

fun DocumentSnapshot.toProduct(): Product = Product(
    id        = id,
    name      = getString("name") ?: "",
    nameLower = getString("nameLower") ?: "",
    price     = getLong("price") ?: 0L,
    brandId   = getString("brandId") ?: "COMMON",
    category  = getString("category") ?: "",
    unit      = getString("unit"),
    sku       = getString("sku"),
    active    = getBoolean("active") ?: true
)