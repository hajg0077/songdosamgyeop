package com.songdosamgyeop.order.ui.splash

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songdosamgyeop.order.core.model.UserRole
import com.songdosamgyeop.order.data.repo.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _route = MutableLiveData<Destination>()
    val route: LiveData<Destination> = _route

    sealed class Destination {
        data object ToLogin : Destination()
        data object ToBranchHome : Destination()
        data object ToHqHome : Destination()
    }

    fun decideRoute() {
        val user = authRepo.currentUser()
        if (user == null) {
            Log.d("SplashVM", "No auth → login")
            _route.value = Destination.ToLogin
            return
        }
        viewModelScope.launch {
            val role = authRepo.fetchUserRole(user.uid)
            Log.d("SplashVM", "uid=${user.uid}, role=$role")
            when (role) {
                UserRole.HQ -> _route.postValue(Destination.ToHqHome)
                UserRole.BRANCH -> _route.postValue(Destination.ToBranchHome)
                else -> _route.postValue(Destination.ToLogin) // 안전망
            }
        }
    }
}