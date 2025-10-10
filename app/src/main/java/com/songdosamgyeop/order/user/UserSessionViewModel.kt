package com.songdosamgyeop.order.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UserSessionViewModel @Inject constructor(
    repo: UserProfileRepository
) : ViewModel() {
    val profile: StateFlow<UserProfile?> =
        repo.currentUserFlow().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )
}