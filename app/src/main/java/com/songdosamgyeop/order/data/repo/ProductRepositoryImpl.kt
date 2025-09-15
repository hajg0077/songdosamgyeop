// data/repo/ProductRepositoryImpl.kt
package com.songdosamgyeop.order.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.data.mapping.toProduct
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : ProductRepository {

    override fun subscribeProducts(
        brandId: String?, category: String?, query: String?
    ): Flow<List<Product>> = callbackFlow {
        var q: Query = db.collection("products").whereEqualTo("active", true)

        if (!brandId.isNullOrBlank()) q = q.whereEqualTo("brandId", brandId)
        if (!category.isNullOrBlank()) q = q.whereEqualTo("category", category)

        q = if (!query.isNullOrBlank()) {
            val lc = query.lowercase()
            db.collection("products")
                .whereEqualTo("active", true)
                .let { base ->
                    var q2: Query = base
                    if (!brandId.isNullOrBlank()) q2 = q2.whereEqualTo("brandId", brandId)
                    if (!category.isNullOrBlank()) q2 = q2.whereEqualTo("category", category)
                    q2.orderBy("nameLower")
                        .whereGreaterThanOrEqualTo("nameLower", lc)
                        .whereLessThan("nameLower", lc + "\uf8ff")
                }
        } else {
            q.orderBy("name")
        }

        val reg = q.addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents.orEmpty().map { it.toProduct() }
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}