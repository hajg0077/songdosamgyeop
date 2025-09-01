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

    /** 선택된 상태 (기본: PENDING) */
    private val status = MutableStateFlow(RegistrationStatus.PENDING)

    /** 검색어 (사용자 입력) */
    private val query = MutableStateFlow("")

    /** 외부에서 구독할 목록 LiveData (Pair<docId, Registration>) */
    @OptIn(FlowPreview::class)
    val list = combine(
        status,
        query.debounce(300) // 과도한 쿼리 방지
    ) { st, q -> st to q.trim() }
        .flatMapLatest { (st, q) -> repo.subscribeList(st, q.ifBlank { null }) }
        .asLiveData(viewModelScope.coroutineContext)

    /** 상태 변경 */
    fun setStatus(newStatus: RegistrationStatus) { status.value = newStatus }

    /** 검색어 변경 */
    fun setQuery(text: String) { query.value = text }
}
