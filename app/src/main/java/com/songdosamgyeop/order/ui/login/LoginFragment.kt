package com.songdosamgyeop.order.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.UserRole
import com.songdosamgyeop.order.ui.branch.BranchActivity
import com.songdosamgyeop.order.ui.hq.HqActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val vm: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPw = view.findViewById<TextInputEditText>(R.id.etPassword)
        val etPhone = view.findViewById<TextInputEditText>(R.id.etPhone)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnSignup = view.findViewById<Button>(R.id.btnSignup)

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString().orEmpty()
            val pw = etPw.text?.toString().orEmpty()
            lifecycleScope.launch {
                runCatching { vm.signIn(email, pw) }
                    .onSuccess {
                        val role = runCatching { vm.getCurrentUserRole() }.getOrDefault(UserRole.UNKNOWN)
                        goHome(role)
                    }
                    .onFailure { e ->
                        Toast.makeText(requireContext(), e.message ?: "로그인 실패", Toast.LENGTH_LONG).show()
                    }
            }
        }

        btnSignup.setOnClickListener {
            PhoneInputBottomSheet { phone ->
                val email = etEmail.text?.toString().orEmpty()
                val pw = etPw.text?.toString().orEmpty()

                lifecycleScope.launch {
                    // 설치 ID 가져오기
                    val installationId = com.google.firebase.installations.FirebaseInstallations.getInstance()
                        .id.await()

                    runCatching { vm.signUpBranch(email, pw, phone, installationId) }
                        .onSuccess {
                            // ✅ 로컬 알림(간단 토스트) — HQ 푸시는 Functions가 전송
                            Toast.makeText(requireContext(), "신청 완료! 본사 승인 대기", Toast.LENGTH_LONG).show()
                            // 필요 시 여기서 바로 Splash로 이동해 재판단해도 OK
                        }
                        .onFailure { e ->
                            Toast.makeText(requireContext(), e.message ?: "회원가입 실패", Toast.LENGTH_LONG).show()
                        }
                }
            }.show(parentFragmentManager, "phone_input")
        }
    }

    private fun goHome(role: UserRole) {
        val intent = when (role) {
            UserRole.HQ -> Intent(requireContext(), HqActivity::class.java)
            UserRole.BRANCH -> Intent(requireContext(), BranchActivity::class.java)
            UserRole.UNKNOWN -> null  // 승인 전
        }?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        if (intent != null) {
            startActivity(intent)
            requireActivity().finish()
        } else {
            toPending()
        }
    }

    private fun toPending() {
        startActivity(
            Intent(requireContext(), PendingApprovalActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        requireActivity().finish()
    }
}