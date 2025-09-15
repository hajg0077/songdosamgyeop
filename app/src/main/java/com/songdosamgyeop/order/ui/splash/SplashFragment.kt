// com/songdosamgyeop/order/ui/splash/SplashFragment.kt
package com.songdosamgyeop.order.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.ui.branch.BranchActivity
import com.songdosamgyeop.order.ui.hq.HqActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {
    private val vm: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.route.observe(viewLifecycleOwner) { dest ->
            when (dest) {
                SplashViewModel.Destination.ToLogin -> {
                    // 로그인 프래그먼트로 이동 (기존 네브 유지)
                    findNavController().navigate(R.id.action_splash_to_login)
                }
                SplashViewModel.Destination.ToBranchHome -> {
                    startActivity(
                        Intent(requireContext(), BranchActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    requireActivity().finish()
                }
                SplashViewModel.Destination.ToHqHome -> {
                    startActivity(
                        Intent(requireContext(), HqActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    requireActivity().finish()
                }
            }
        }
        vm.decideRoute()
    }
}