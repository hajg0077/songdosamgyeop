// com/songdosamgyeop/order/ui/login/LoginFragment.kt
package com.songdosamgyeop.order.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.UserRole
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 로그인 화면.
 * 개발 단계에서는 더미(익명) 로그인 2가지(HQ/BRANCH) 버튼만 제공한다.
 * 실제 출시는 이메일/비번, 커스텀 토큰 등으로 대체.
 */
@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val vm: LoginViewModel by viewModels()

    /**
     * 버튼 클릭 리스너를 설정하고, 성공 시 스플래시로 돌아가서 분기를 재실행한다.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnHq: Button = view.findViewById(R.id.btnLoginHq)
        val btnBranch: Button = view.findViewById(R.id.btnLoginBranch)

        // 본사 더미 로그인
        btnHq.setOnClickListener {
            lifecycleScope.launch {
                vm.signInDummy(UserRole.HQ) // 역할을 HQ로 세팅
                Toast.makeText(requireContext(), "HQ로 로그인 완료(더미)", Toast.LENGTH_SHORT).show()
                // 스플래시로 돌아가서 역할 기반 라우팅 재수행
                findNavController().navigate(R.id.action_login_to_splash)
            }
        }

        // 지사 더미 로그인
        btnBranch.setOnClickListener {
            lifecycleScope.launch {
                vm.signInDummy(UserRole.BRANCH) // 역할을 BRANCH로 세팅
                Toast.makeText(requireContext(), "지사로 로그인 완료(더미)", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_login_to_splash)
            }
        }
    }
}