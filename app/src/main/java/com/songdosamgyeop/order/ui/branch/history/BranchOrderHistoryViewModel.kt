package com.songdosamgyeop.order.ui.branch.history

import androidx.lifecycle.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.ui.hq.orders.OrderDisplayRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BranchOrderHistoryViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _dateStart = MutableLiveData<Timestamp?>(null)
    private val _dateEnd = MutableLiveData<Timestamp?>(null)
    private val _status = MutableLiveData<String?>(null)

    val list: LiveData<List<OrderDisplayRow>> = MediatorLiveData<List<OrderDisplayRow>>().apply {
        val reload = {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@apply
            var q = db.collection("orders")
                .whereEqualTo("ownerUid", uid)

            _status.value?.let { q = q.whereEqualTo("status", it) }
            _dateStart.value?.let { q = q.whereGreaterThanOrEqualTo("placedAt", it) }
            _dateEnd.value?.let { q = q.whereLessThan("placedAt", it) }

            q = q.orderBy("placedAt", Query.Direction.DESCENDING)

            q.get().addOnSuccessListener { snap ->
                value = snap.documents.map { d ->
                    OrderDisplayRow(
                        id = d.id,
                        brandId = d.getString("brandId"),
                        branchId = d.getString("branchId"),
                        branchName = d.getString("branchName"),
                        status = d.getString("status"),
                        itemsCount = (d.getLong("itemsCount") ?: 0L).toInt(),
                        totalAmount = d.getLong("totalAmount") ?: 0L,
                        placedAt = d.getTimestamp("placedAt")?.toDate(),
                        createdAt = d.getTimestamp("createdAt")?.toDate()
                    )
                }
            }
        }
        addSource(_dateStart) { reload() }
        addSource(_dateEnd) { reload() }
        addSource(_status) { reload() }
        reload()
    }

    fun setDateRange(start: Timestamp?, endExclusive: Timestamp?) { _dateStart.value = start; _dateEnd.value = endExclusive }
    fun setStatus(s: String?) { _status.value = s }
}
