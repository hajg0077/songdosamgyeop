package com.songdosamgyeop.order.ui.hq.registrationlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.songdosamgyeop.order.data.model.Registration
import com.songdosamgyeop.order.data.repo.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class HqRegistrationListViewModel @Inject constructor(
    private val regRepo: RegistrationRepository
) : ViewModel() {
    val pendingList: LiveData<List<Pair<String, Registration>>> =
        regRepo.listenPendingRegistrations().asLiveData()
}