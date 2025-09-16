package com.songdosamgyeop.order.ui.branch.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.OrderHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BranchOrderHistoryViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val endReached: Boolean = false,
        val items: List<OrderHeader> = emptyList(),
        val noteSearchActive: Boolean = false
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    private var lastSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
    private var currentQuery: String = ""
    private var period: Int = 0
    private val enabledStatus = mutableSetOf<OrderStatus>(
        OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.REJECTED,
        OrderStatus.SHIPPED, OrderStatus.DELIVERED
    )

    fun setQuery(q: String) {
        currentQuery = q
        refresh()
    }

    fun setPeriod(p: Int) {
        period = p
        refresh()
    }

    fun setStatusEnabled(status: OrderStatus, enabled: Boolean) {
        if (enabled) enabledStatus += status else enabledStatus -= status
        refresh()
    }

    fun refresh() {
        lastSnapshot = null
        load(initial = true)
    }

    fun loadNext() {
        if (_uiState.value?.endReached == true || _uiState.value?.loadingMore == true) return
        load(initial = false)
    }

    private fun load(initial: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value?.copy(loading = initial, loadingMore = !initial)

        viewModelScope.launch {
            runCatching {
                var q = db.collection("orders")
                    .whereEqualTo("ownerUid", uid)
                    .orderBy("placedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

                // 기간 필터
                when (period) {
                    1 -> { // 오늘
                        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        q = q.whereGreaterThanOrEqualTo("placedAt", Timestamp(cal.time))
                    }
                    2 -> { // 최근 7일
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                        q = q.whereGreaterThanOrEqualTo("placedAt", Timestamp(cal.time))
                    }
                    3 -> { // 이번 달
                        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        q = q.whereGreaterThanOrEqualTo("placedAt", Timestamp(cal.time))
                    }
                }

                // 검색
                var noteSearchActive = false
                if (currentQuery.startsWith("#")) {
                    // 문서 ID 단건 조회
                    val id = currentQuery.removePrefix("#").trim()
                    val snap = db.collection("orders").document(id).get().await()
                    val header = snap.toObject(OrderHeader::class.java)?.copy(id = snap.id)
                    val list = header?.let { listOf(it) } ?: emptyList()
                    _uiState.postValue(UiState(loading = false, items = list, endReached = true))
                    return@launch
                } else if (currentQuery.isNotBlank()) {
                    // 지사명 prefix 검색
                    val lower = currentQuery.lowercase()
                    q = q.whereGreaterThanOrEqualTo("branchName_lower", lower)
                        .whereLessThan("branchName_lower", lower + "\uf8ff")
                } else {
                    // 상태칩 필터
                    if (enabledStatus.isNotEmpty()) {
                        q = q.whereIn("status", enabledStatus.map { it.name })
                    }
                }

                // 페이징
                if (lastSnapshot != null) {
                    q = q.startAfter(lastSnapshot!!)
                }
                q = q.limit(20)

                val snap = q.get().await()
                val list = snap.documents.map { d ->
                    d.toObject(OrderHeader::class.java)!!.copy(id = d.id)
                }
                lastSnapshot = snap.documents.lastOrNull()

                val merged = if (initial) list else _uiState.value?.items.orEmpty() + list
                _uiState.postValue(
                    UiState(
                        loading = false,
                        loadingMore = false,
                        items = merged,
                        endReached = list.isEmpty(),
                        noteSearchActive = noteSearchActive
                    )
                )
            }.onFailure {
                _uiState.postValue(UiState(loading = false))
            }
        }
    }
}