package com.songdosamgyeop.order.ui.hq.registrationlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
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
    private val repo: RegistrationRepository,
    private val saved: SavedStateHandle,          // ✅ SavedStateHandle 주입
) : ViewModel() {

    private companion object {
        const val K_STATUS = "regs.status"
        const val K_QUERY  = "regs.query"
    }

    // ✅ 제네릭 명시 + SavedState 복원 (잘못된 문자열 방지용 runCatching)
    private val status: MutableStateFlow<RegistrationStatus> =
        MutableStateFlow(
            saved.get<String>(K_STATUS)
                ?.let { runCatching { RegistrationStatus.valueOf(it) }.getOrNull() }
                ?: RegistrationStatus.PENDING
        )

    // ✅ 제네릭 명시 (nullable 검색어)
    private val query: MutableStateFlow<String?> =
        MutableStateFlow(saved.get<String>(K_QUERY))

    fun setStatus(s: RegistrationStatus) {
        status.value = s
        saved.set(K_STATUS, s.name)                 // ✅ set 명시적 사용
    }

    fun setQuery(q: String?) {
        query.value = q
        saved.set(K_QUERY, q)                       // ✅ set 명시적 사용
    }

    /** 당겨서 새로고침 등 강제 재시작용 */
    fun refresh() {
        // 같은 값 재할당로 플로우 재발행
        status.value = status.value
        query.value = query.value
    }

    /** 외부에서 구독할 목록 LiveData (Pair<docId, Registration>) */
    @OptIn(FlowPreview::class)
    val list = combine(
        status,
        query.debounce(300)                         // 과도한 쿼리 방지
    ) { st: RegistrationStatus, q: String? ->
        st to q?.trim()                             // ✅ null-safe
    }.flatMapLatest { (st, qTrimmed) ->
        // 공백이면 null로 치환
        val qOrNull: String? = qTrimmed.takeUnless { it.isNullOrBlank() }
        repo.subscribeList(st, qOrNull)             // ✅ Repository 시그니처: (RegistrationStatus, String?)
    }.asLiveData(viewModelScope.coroutineContext)
}