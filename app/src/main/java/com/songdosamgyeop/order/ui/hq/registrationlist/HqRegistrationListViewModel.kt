package com.songdosamgyeop.order.ui.hq.registrationlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.core.model.RegistrationStatus
import com.songdosamgyeop.order.data.repo.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest

/**
 * HQ 신청서 목록 VM
 * - 상태 칩 + 검색어를 조합하여 Firestore 실시간 구독
 */
@HiltViewModel
class HqRegistrationListViewModel @Inject constructor(
    private val repo: RegistrationRepository
) : ViewModel() {

    private companion object {
        const val K_STATUS = "regs.status"
        const val K_QUERY  = "regs.query"
    }

    private val status = MutableStateFlow(
        saved.get<String>(K_STATUS)?.let { RegistrationStatus.valueOf(it) }
            ?: RegistrationStatus.PENDING
    )
    private val query  = MutableStateFlow(saved.get<String?>(K_QUERY))
    fun setStatus(s: RegistrationStatus) {
        status.value = s
        saved[K_STATUS] = s.name
    }
    fun setQuery(q: String?) {
        query.value = q
        saved[K_QUERY] = q
    }

    /** 당겨서 새로고침 등 강제 재시작용 */
    fun refresh() {
        status.value = status.value
        query.value = query.value
    }
    /** 외부에서 구독할 목록 LiveData (Pair<docId, Registration>) */
    @OptIn(FlowPreview::class)
    val list = combine(
        status,
        query.debounce(300) // 과도한 쿼리 방지
    ) { st, q -> st to q.trim() }
        .flatMapLatest { (st, q) -> repo.subscribeList(st, q.ifBlank { null }) }
        .asLiveData(viewModelScope.coroutineContext)
}
