import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.data.repo.HqOrdersRepository
import com.songdosamgyeop.order.ui.hq.orders.OrderDisplayRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class HqOrdersViewModel @Inject constructor(
    private val repo: HqOrdersRepository,
    private val saved: SavedStateHandle
) : ViewModel() {

    private companion object {
        const val K_STATUS = "orders.status"
        const val K_BRANCH = "orders.branchQ"
        const val K_FROM   = "orders.from"
        const val K_TO     = "orders.to"
    }
    private val status = MutableStateFlow(saved.get<String>(K_STATUS) ?: "PLACED")
    private val branchNameQuery = MutableStateFlow<String?>(saved[K_BRANCH])
    private val from = MutableStateFlow<Timestamp?>(saved[K_FROM])
    private val to   = MutableStateFlow<Timestamp?>(saved[K_TO])


    data class Params(
        val status: String,
        val branchNamePrefix: String?,
        val from: Timestamp?,
        val to: Timestamp?
    )

    @OptIn(FlowPreview::class)
    val displayList = combine(
        status,
        branchNameQuery.debounce(200).map { it?.trim() },
        from,
        to
    ) { s, nameQ, f, t ->
        Params(
            status = s,
            branchNamePrefix = nameQ?.takeIf { it.isNotBlank() },
            from = f,
            to = t
        )
    }.flatMapLatest { p ->
        repo.subscribeOrders(
            status = p.status,
            branchNamePrefix = p.branchNamePrefix,
            from = p.from,
            to = p.to
        )
    }.map { rows -> rows.map { OrderDisplayRow.from(it) } }
        .asLiveData()

    fun setBranchNameQuery(value: String?) {
        branchNameQuery.value = value
        saved[K_BRANCH] = value
    }
    fun setDateRange(start: Timestamp?, endExclusive: Timestamp?) {
        from.value = start; to.value = endExclusive
        saved[K_FROM] = start; saved[K_TO] = endExclusive
    }
    fun setStatus(value: String) {
        status.value = value
        saved[K_STATUS] = value
    }

    fun refresh() {
        status.value = status.value; branchNameQuery.value = branchNameQuery.value
        from.value = from.value; to.value = to.value
    }
}