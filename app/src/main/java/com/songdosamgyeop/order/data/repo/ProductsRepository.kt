package com.songdosamgyeop.order.data.repo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 상품 목록 조회 레포지토리.
 * - products 컬렉션에서 active=true 상품 실시간 구독
 */
class ProductsRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /** 활성 상품 구독 (이름 정렬) */
    fun subscribeActiveProducts(): Flow<List<Product>> = callbackFlow {
        val q: Query = db.collection("products")
            .whereEqualTo("active", true)
            .orderBy("name_lower")
        val reg: ListenerRegistration = q.addSnapshotListener { snap, e ->
            if (e != null) { Log.e("ProductsRepo", "listen error", e); trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.documents?.map { d ->
                val m = d.data ?: emptyMap<String, Any?>()
                Product(
                    id = d.id,
                    name = m["name"] as? String ?: "",
                    price = (m["price"] as? Number)?.toLong() ?: 0L,
                    active = m["active"] as? Boolean ?: false
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }
}
