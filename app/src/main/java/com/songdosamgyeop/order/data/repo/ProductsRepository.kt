package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.core.model.BrandId
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** 상품 조회 레포지토리(브랜드/카테고리/검색 접두 필터) */
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

        if (!category.isNullOrBlank()) ref = ref.whereEqualTo("category", category)

        val hasQ = !q.isNullOrBlank()
        ref = if (hasQ) {
            ref.orderBy("name_lower")
                .startAt(q!!.lowercase())
                .endAt(q.lowercase() + '\uf8ff')
        } else {
            ref.orderBy("name_lower")
        }

        val reg = ref.addSnapshotListener { s, _ ->
            val list = s?.documents?.map { d ->
                val p = d.toObject(Product::class.java)!!.copy()
                p.copy(id = d.id) // docId 주입
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}