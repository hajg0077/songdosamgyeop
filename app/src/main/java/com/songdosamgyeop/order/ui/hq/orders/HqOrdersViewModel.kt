package com.songdosamgyeop.order.ui.hq.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.data.repo.BranchDirectoryRepository
import com.songdosamgyeop.order.data.repo.HqOrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@HiltViewModel
class HqOrdersViewModel @Inject constructor(
    private val repo: HqOrdersRepository,
    private val branches: BranchDirectoryRepository
) : ViewModel() {

    private val status = MutableStateFlow("PLACED")
    private val branchId = MutableStateFlow<String?>(null)
    private val from = MutableStateFlow<Timestamp?>(null)
    private val to   = MutableStateFlow<Timestamp?>(null)
    private val branchNameQuery = MutableStateFlow<String?>(null)

    @OptIn(FlowPreview::class)
    private val rawList: Flow<List<com.songdosamgyeop.order.data.model.OrderRow>> =
        combine(status, branchId.debounce(250), from, to) { s, b, f, t ->
            Params(s, b?.takeIf { it.isNotBlank() }, f, t)
        }.flatMapLatest { p ->
            repo.subscribeOrders(p.status, p.branchId, p.from, p.to)
        }

    /** branchId → branchName 맵 */
    private val branchMap: Flow<Map<String, String>> = branches.subscribeMap()

    // 기존 displayList 생성로직 교체
    val displayList = combine(rawList, branchMap, branchNameQuery.debounce(200)) { rows, map, nameQ ->
        val display = rows.map { OrderDisplayRow.from(it, map) }
        val q = nameQ?.trim().orEmpty()
        if (q.isEmpty()) display
        else {
            // 대소문자 무시 포함 검색
            val lowerQ = q.lowercase()
            display.filter { it.branchLabel.lowercase().contains(lowerQ) }
        }
    }.asLiveData(viewModelScope.coroutineContext)

    data class Params(
        val status: String,
        val branchId: String?,
        val from: Timestamp?,
        val to: Timestamp?
    )

    fun setBranchId(value: String?) { branchId.value = value }
    fun setDateRange(start: Timestamp?, endExclusive: Timestamp?) { from.value = start; to.value = endExclusive }
    fun setStatus(value: String) { status.value = value }
    // 외부에서 호출할 setter
    fun setBranchNameQuery(value: String?) { branchNameQuery.value = value }
}
