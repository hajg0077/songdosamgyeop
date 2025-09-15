// com/songdosamgyeop/order/ui/login/LoginFragment.kt
package com.songdosamgyeop.order.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.UserRole
import com.songdosamgyeop.order.ui.branch.BranchActivity
import com.songdosamgyeop.order.ui.hq.HqActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val vm: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnHq: Button = view.findViewById(R.id.btnLoginHq)
        val btnBranch: Button = view.findViewById(R.id.btnLoginBranch)

        fun goHome(role: UserRole) {
            val intent = when (role) {
                UserRole.HQ -> Intent(requireContext(), HqActivity::class.java)
                UserRole.BRANCH -> Intent(requireContext(), BranchActivity::class.java)
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            requireActivity().finish()
        }

        btnHq.setOnClickListener {
            lifecycleScope.launch {
                runCatching { vm.signInDummy(UserRole.HQ) }
                    .onSuccess {
                        Toast.makeText(requireContext(), "HQ로 로그인 완료(더미)", Toast.LENGTH_SHORT).show()
                        goHome(UserRole.HQ)
                    }
                    .onFailure { e ->
                        Toast.makeText(requireContext(), e.message ?: "로그인 실패", Toast.LENGTH_LONG).show()
                    }
            }
        }

        btnBranch.setOnClickListener {
            lifecycleScope.launch {
                runCatching { vm.signInDummy(UserRole.BRANCH) }
                    .onSuccess {
                        Toast.makeText(requireContext(), "지사로 로그인 완료(더미)", Toast.LENGTH_SHORT).show()
                        goHome(UserRole.BRANCH)
                    }
                    .onFailure { e ->
                        Toast.makeText(requireContext(), e.message ?: "로그인 실패", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}