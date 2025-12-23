package com.songdosamgyeop.order.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.core.model.RegistrationAddress
import com.songdosamgyeop.order.core.model.UserRole
import com.songdosamgyeop.order.data.repo.BranchRegistrationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val regRepo: BranchRegistrationRepository
) : ViewModel() {

    sealed class SignupState {
        data object Idle : SignupState()
        data object Loading : SignupState()
        data object Success : SignupState()
        data class Error(val msg: String) : SignupState()
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState = _signupState.asStateFlow()

    fun signUpBranchAsync(
        email: String,
        password: String,
        phone: String?,
        installationId: String,
        branchName: String,
        address: RegistrationAddress
    ) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading

            runCatching {
                signUpBranch(
                    email, password, phone,
                    installationId, branchName, address
                )
            }.onSuccess {
                _signupState.value = SignupState.Success
            }.onFailure { e ->
                // ❗ Cancellation은 무시
                if (e is CancellationException) return@onFailure
                _signupState.value =
                    SignupState.Error(e.message ?: "회원가입 실패")
            }
        }
    }

    /** 기존 suspend 함수 그대로 사용 */
    suspend fun signUpBranch(
        email: String,
        password: String,
        phone: String?,
        installationId: String,
        branchName: String,
        address: RegistrationAddress
    ) {
        _signupState.value = SignupState.Loading
        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("계정 생성 실패")

            regRepo.submitRegistration(
                userUid = user.uid,
                email = email,
                branchName = branchName,
                phone = phone,
                installationId = installationId,
                address = address
            ).getOrThrow()

            db.collection("devices").document(installationId).set(
                mapOf(
                    "registeredUid" to user.uid,
                    "email" to email.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            _signupState.value = SignupState.Success
        } catch (e: Exception) {
            _signupState.value = SignupState.Error(e.message ?: "회원가입 실패")
        }
    }

    suspend fun getCurrentUserRole(): UserRole {
        val uid = auth.currentUser?.uid ?: return UserRole.UNKNOWN
        val snap = db.collection("users").document(uid).get().await()
        return when (snap.getString("role").orEmpty()) {
            "HQ" -> UserRole.HQ
            "BRANCH" -> UserRole.BRANCH
            else -> UserRole.UNKNOWN
        }
    }
}