// ui/hq/orders/HqOrdersViewModel.kt
package com.songdosamgyeop.order.ui.hq.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.data.repo.HqOrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@HiltViewModel
class HqOrdersViewModel @Inject constructor(
    private val repo: HqOrdersRepository
) : ViewModel() {

    private val status = MutableStateFlow("PLACED")
    private val branchNameQuery = MutableStateFlow<String?>(null)
    private val from = MutableStateFlow<Timestamp?>(null)
    private val to   = MutableStateFlow<Timestamp?>(null)

    @OptIn(FlowPreview::class)
    val displayList = combine(
        status,
        branchNameQuery.debounce(200),
        from,
        to
    ) { s, nameQ, f, t ->
        Params(s, nameQ?.takeIf { !it.isNullOrBlank() }, f, t)
    }.flatMapLatest { p ->
        repo.subscribeOrders(
            status = p.status,
            branchNamePrefix = p.branchNamePrefix,
            from = p.from,
            to = p.to
        )
    }.map { rows -> rows.map { OrderDisplayRow.from(it) } }
        .asLiveData(viewModelScope.coroutineContext)

    data class Params(
        val status: String,
        val branchNamePrefix: String?,
        val from: Timestamp?,
        val to: Timestamp?
    )

    fun setBranchNameQuery(value: String?) { branchNameQuery.value = value }
    fun setDateRange(start: Timestamp?, endExclusive: Timestamp?) { from.value = start; to.value = endExclusive }
    fun setStatus(value: String) { status.value = value }
}