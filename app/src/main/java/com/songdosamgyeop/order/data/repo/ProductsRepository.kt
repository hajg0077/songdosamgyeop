package com.songdosamgyeop.order.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.core.model.BrandId
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 상품 조회 레포지토리(브랜드/카테고리/검색 접두 필터) */
@Singleton
class ProductRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    fun subscribeProducts(
        brand: BrandId,
        category: String?,
        q: String?
    ): Flow<List<Product>> = callbackFlow {
        var ref: Query = db.collection("products")
            .whereEqualTo("active", true)
            .whereEqualTo("brandId", brand.value)

        if (!category.isNullOrBlank()) {
            ref = ref.whereEqualTo("category", category)
        }

        ref = if (!q.isNullOrBlank()) {
            val prefix = q.lowercase(Locale.ROOT)
            ref.orderBy("nameLower")
                .whereGreaterThanOrEqualTo("nameLower", prefix)
                .whereLessThan("nameLower", prefix + "\uf8ff")
        } else {
            ref.orderBy("nameLower")
        }

        val reg = ref.addSnapshotListener { s, _ ->
            val list = s?.documents?.map { d ->
                d.toObject(Product::class.java)!!.copy(id = d.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}