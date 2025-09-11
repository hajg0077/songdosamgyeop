package com.songdosamgyeop.order.ui.hq.orders

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.OrderRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HqOrdersViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "HqOrdersViewModel"
        private const val PAGE_SIZE = 30L
        const val TAB_IN_PROGRESS = "inProgress"
        const val TAB_COMPLETED   = "completed"
    }

    // 필터
    private val _branchQuery = MutableLiveData(savedStateHandle.get<String>("branchQuery") ?: "")
    val branchQuery: LiveData<String> = _branchQuery

    private val _dateStart = MutableLiveData<Timestamp?>(savedStateHandle.get<Timestamp>("dateStart"))
    private val _dateEnd = MutableLiveData<Timestamp?>(savedStateHandle.get<Timestamp>("dateEnd"))
    val dateStart: LiveData<Timestamp?> = _dateStart
    val dateEnd: LiveData<Timestamp?> = _dateEnd

    // 페이징용 커서
    private var lastDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isLoading = false
    private var isEndReached = false

    // 현재 로드된 목록(LiveData로 노출)
    private val _items = MutableLiveData<List<Map<String, Any>>>(emptyList())
    val items: LiveData<List<Map<String, Any>>> = _items

    // 진행/완료 상태 세트
    private val progressing = listOf(OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.REJECTED)
    private val completed = listOf(OrderStatus.SHIPPED, OrderStatus.DELIVERED)


    // 🔒 설정 제거: 하드코딩 기본값
    private val defaultTab = TAB_IN_PROGRESS
    private val includeRejectedInProgress = false
    private val sortMode = SortMode.PLACED_AT_DESC

    private val _tab = MutableLiveData(defaultTab)
    val tab: LiveData<String> = _tab
    private val _displayList = MutableLiveData<List<OrderDisplayRow>>(emptyList())
    val displayList: LiveData<List<OrderDisplayRow>> = _displayList

    // 탭별 상태 매핑 (거절 미포함)
    private fun statusesForTab(tab: String): List<OrderStatus> = when (tab) {
        TAB_COMPLETED -> listOf(OrderStatus.DELIVERED)
        else -> buildList {
            add(OrderStatus.PENDING)
            add(OrderStatus.APPROVED)
            add(OrderStatus.SHIPPED)
            if (includeRejectedInProgress) add(OrderStatus.REJECTED)
        }
    }

    // 정렬 모드 간단 enum (금액순은 나중에 필요해지면)
    enum class SortMode { PLACED_AT_DESC /*, TOTAL_AMOUNT_DESC*/ }

    // 🔎 쿼리 정렬(기본: placedAt DESC)
    private fun applySort(qBase: com.google.firebase.firestore.Query): com.google.firebase.firestore.Query {
        return when (sortMode) {
            SortMode.PLACED_AT_DESC -> qBase.orderBy("placedAt", Query.Direction.DESCENDING)
        }
    }

    fun setTab(newTab: String) {
        if (_tab.value == newTab) return
        Log.d(TAG, "setTab: $newTab")
        _tab.value = newTab
        resetAndLoad()
    }

    fun setBranchQuery(q: String) {
        _branchQuery.value = q
        resetAndLoad()
    }

    fun setDateRange(start: Timestamp?, end: Timestamp?) {
        _dateStart.value = start
        _dateEnd.value = end
        resetAndLoad()
    }

    private fun resetAndLoad() {
        lastDoc = null
        isEndReached = false
        _items.value = emptyList()
        loadNext()
    }

    fun loadNext() {
        if (isLoading || isEndReached) return
        isLoading = true

        val statuses = statusesForTab(_tab.value ?: TAB_IN_PROGRESS).map { it.name }

        var q: Query = db.collection("orders")
            .whereIn("status", statuses)

        val branchQ = _branchQuery.value?.lowercase()
        val startTs = _dateStart.value
        val endTs   = _dateEnd.value

        q = applySort(q)

        // 지사명 검색 있을 때 (branchNameLower 먼저, 그 다음 placedAt)
        q = q.orderBy("branchNameLower")
        q = q.orderBy("placedAt", Query.Direction.DESCENDING)

        if (startTs != null) q = q.whereGreaterThanOrEqualTo("placedAt", startTs)
        if (endTs   != null) q = q.whereLessThan("placedAt", endTs)

        if (lastDoc != null) q = q.startAfter(lastDoc!!)
        q = q.limit(PAGE_SIZE)

        q.get()
            .addOnSuccessListener { snap ->
                val mapped = snap.documents.mapNotNull { it.toOrderRow() }
                    .map(OrderDisplayRow::from) // ✅ 여기서 UI 모델로 변환
                val current = _displayList.value.orEmpty()
                _displayList.value = current + mapped

                if (snap.size() < PAGE_SIZE) isEndReached = true
                if (snap.documents.isNotEmpty()) lastDoc = snap.documents.last()
            }
            .addOnFailureListener { e -> Log.e(TAG, "loadNext(): error", e) }
            .addOnCompleteListener { isLoading = false }
    }

    /** Firestore → OrderRow 매핑 (원본 모델) */
    private fun DocumentSnapshot.toOrderRow(): OrderRow? = try {
        OrderRow(
            id = id,
            brandId = getString("brandId"),
            branchId = getString("branchId"),
            branchName = getString("branchName"),
            status = getString("status"),
            itemsCount = (getLong("itemsCount") ?: getLong("itemCount"))?.toInt(),
            totalAmount = getLong("totalAmount"),
            placedAt = getTimestamp("placedAt"),
            createdAt = getTimestamp("createdAt")
        )
    } catch (e: Exception) {
        Log.e(TAG, "toOrderRow(): ${e.message}", e)
        null
    }
}