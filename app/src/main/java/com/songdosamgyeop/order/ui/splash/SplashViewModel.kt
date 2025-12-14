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
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    sealed class Destination {
        data object ToLogin : Destination()
        data object ToBranchHome : Destination()
        data object ToHqHome : Destination()
        data object ToBranchWaiting : Destination()
    }

    private val _route = MutableLiveData<Destination>()
    val route: LiveData<Destination> = _route

    private var decideJob: Job? = null
    @Volatile private var decided = false

    fun decideRoute() {
        if (decided) return
        decideJob?.cancel()
        decideJob = viewModelScope.launch {
            val user = runCatching { authRepo.currentUser() }.getOrNull()
            if (user == null) {
                Log.d(TAG, "No auth → login")
                emitOnce(Destination.ToLogin)
                return@launch
            }

            // 최대 3초 안에 역할을 받아오고, 실패/지연 시 로그인으로 안전하게 보냄
            val role = try {
                withTimeoutOrNull(3_000) {
                    authRepo.fetchUserRole(user.uid)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "fetchUserRole failed: ${t.message}")
                null
            }

            Log.d(TAG, "uid=${user.uid}, role=$role")

            when (role) {
                UserRole.HQ -> emitOnce(Destination.ToHqHome)
                UserRole.BRANCH -> emitOnce(Destination.ToBranchHome)
                else -> emitOnce(Destination.ToLogin) // 프로필 미구성/오류/타임아웃
            }
        }
    }

    private fun emitOnce(dest: Destination) {
        if (decided) return
        decided = true
        _route.postValue(dest)
    }

    override fun onCleared() {
        decideJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TAG = "SplashVM"
    }
}