// app/src/main/java/com/songdosamgyeop/order/ui/branch/history/BranchOrderHistoryViewModel.kt
package com.songdosamgyeop.order.ui.branch.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.OrderHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

data class BranchHistoryState(
    val loading: Boolean = false,
    val items: List<OrderHeader> = emptyList(),
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val noteSearchActive: Boolean = false // 메모 검색 시 상태칩 무시 안내용
)

@HiltViewModel
class BranchOrderHistoryViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableLiveData(BranchHistoryState(loading = true))
    val uiState: LiveData<BranchHistoryState> = _uiState

    private var period: Int = 1 // 0:none 1:today 2:7days 3:this month
    private val statusEnabled = linkedSetOf(
        OrderStatus.PENDING, OrderStatus.APPROVED, OrderStatus.REJECTED, OrderStatus.SHIPPED, OrderStatus.DELIVERED
    )

    private val pageSize = 20
    private var lastDoc: DocumentSnapshot? = null
    private var loadingNext = false

    private var searchQuery: String = ""
    private var debounceJob: Job? = null

    init { resetAndLoad() }

    fun setPeriod(p: Int) {
        period = p
        resetAndLoad()
    }

    fun setStatusEnabled(status: OrderStatus, enabled: Boolean) {
        if (enabled) statusEnabled.add(status) else statusEnabled.remove(status)
        resetAndLoad()
    }

    fun setQuery(q: String) {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            searchQuery = q
            delay(300)
            resetAndLoad()
        }
    }

    fun refresh() = resetAndLoad()

    fun loadNext() {
        if (loadingNext || _uiState.value?.endReached == true) return
        // 메모 검색 중엔 페이징도 지원 (쿼리 기반이라 동일)
        loadingNext = true
        _uiState.value = _uiState.value?.copy(loadingMore = true)
        query(next = true)
    }

    private fun resetAndLoad() {
        lastDoc = null
        loadingNext = false
        _uiState.value = BranchHistoryState(loading = true, noteSearchActive = isNoteSearchActive())
        query(next = false)
    }

    private fun timeRange(): Pair<Timestamp?, Timestamp?> {
        val cal = Calendar.getInstance()
        return when (period) {
            1 -> { // today
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = Timestamp(cal.time)
                cal.add(Calendar.DAY_OF_MONTH, 1)
                start to Timestamp(cal.time)
            }
            2 -> { // last 7 days
                val end = Timestamp(Calendar.getInstance().time)
                cal.add(Calendar.DAY_OF_YEAR, -7)
                Timestamp(cal.time) to end
            }
            3 -> { // this month
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = Timestamp(cal.time)
                val endCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, 1) }
                start to Timestamp(endCal.time)
            }
            else -> null to null
        }
    }

    private fun isOrderIdQuery(): String? {
        val q = searchQuery.trim()
        // 간단 규칙: 8~28자 영숫자면 주문ID로 간주(문서ID 길이 가정), # 접두어 허용
        val s = if (q.startsWith("#")) q.drop(1) else q
        return if (s.length in 8..28 && s.all { it.isLetterOrDigit() }) s else null
    }

    private fun noteTokens(): List<String> {
        val raw = searchQuery.trim().lowercase()
        if (raw.isBlank()) return emptyList()
        // 구두점/공백 분리, 1~10개 제한, 2글자 이상만
        return raw.split(Regex("[^a-z0-9가-힣]+"))
            .mapNotNull { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(10)
    }

    private fun isNoteSearchActive() = searchQuery.isNotBlank() && isOrderIdQuery() == null

    private fun baseQuery(uid: String): Query {
        var q: Query = db.collection("orders")
            .whereEqualTo("ownerUid", uid)

        val (start, end) = timeRange()
        if (start != null) q = q.whereGreaterThanOrEqualTo("placedAt", start)
        if (end != null) q = q.whereLessThan("placedAt", end)

        val idQuery = isOrderIdQuery()
        val tokens = noteTokens()

        // 메모 검색(active)인 경우 상태칩(whereIn) 적용 불가 → 무시
        if (idQuery == null && tokens.isEmpty()) {
            if (statusEnabled.isNotEmpty() && statusEnabled.size < OrderStatus.values().size) {
                q = q.whereIn("status", statusEnabled.map { it.name })
            }
        }

        if (idQuery == null && tokens.isNotEmpty()) {
            q = q.whereArrayContainsAny("noteKeywords", tokens)
        }

        return q.orderBy("placedAt", Query.Direction.DESCENDING)
    }

    private fun query(next: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val idQuery = isOrderIdQuery()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (idQuery != null) {
                    // 단건 조회 후 로컬 필터링
                    val doc = db.collection("orders").document(idQuery).get().await()
                    val header = doc.toObject(OrderHeader::class.java)?.copy(id = doc.id)
                    val result = header?.takeIf { it.ownerUid == uid }?.let { h ->
                        val (start, end) = timeRange()
                        val placed = h.placedAt
                        val inPeriod = when {
                            start != null && end != null -> placed != null && placed >= start && placed < end
                            start != null -> placed != null && placed >= start
                            end != null -> placed != null && placed < end
                            else -> true
                        }
                        val inStatus = if (statusEnabled.size < OrderStatus.values().size)
                            h.status != null && statusEnabled.map { it.name }.contains(h.status) else true
                        if (inPeriod && inStatus) listOf(h) else emptyList()
                    } ?: emptyList()

                    _uiState.postValue(
                        BranchHistoryState(
                            loading = false,
                            items = result,
                            loadingMore = false,
                            endReached = true,
                            noteSearchActive = false
                        )
                    )
                    return@launch
                }

                // 리스트 쿼리(메모 검색 or 일반)
                var q = baseQuery(uid).limit(pageSize.toLong())
                lastDoc?.let { q = q.startAfter(it) }

                val snap = q.get().await()
                val docs = snap.documents
                val list = docs.mapNotNull { d -> d.toObject(OrderHeader::class.java)?.copy(id = d.id) }

                val prev = _uiState.value ?: BranchHistoryState()
                val merged = if (next) prev.items + list else list

                lastDoc = docs.lastOrNull()
                val endReached = docs.size < pageSize

                _uiState.postValue(
                    prev.copy(
                        loading = false,
                        items = merged,
                        loadingMore = false,
                        endReached = endReached,
                        noteSearchActive = isNoteSearchActive()
                    )
                )
            } catch (e: Exception) {
                _uiState.postValue(
                    BranchHistoryState(
                        loading = false,
                        items = if (next) _uiState.value?.items.orEmpty() else emptyList(),
                        loadingMore = false,
                        endReached = true,
                        noteSearchActive = isNoteSearchActive()
                    )
                )
            } finally {
                loadingNext = false
            }
        }
    }

    // local await
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            addOnCompleteListener { t ->
                if (t.isSuccessful) cont.resume(t.result, null)
                else cont.resumeWithException(t.exception ?: RuntimeException("Task error"))
            }
        }
    }
}