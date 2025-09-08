package com.songdosamgyeop.order.ui.hq.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.songdosamgyeop.order.data.repo.HqDashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HqHomeViewModel @Inject constructor(
    private val repo: HqDashboardRepository
) : ViewModel() {

    data class UiState(
        val todayOrdersCount: Int = 0,
        val todayOrdersSum: Long = 0,
        val pendingRegistrations: Int = 0,
        val activeOrders: Int = 0,
        val loading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val (count, sum) = repo.getTodayOrdersSummary()
                val pending = repo.getPendingRegistrations()
                val active = repo.getActiveOrders()

                _state.value = UiState(
                    todayOrdersCount = count,
                    todayOrdersSum = sum,
                    pendingRegistrations = pending,
                    activeOrders = active,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}