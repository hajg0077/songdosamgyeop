package com.songdosamgyeop.order.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.ui.branch.BranchActivity
import com.songdosamgyeop.order.ui.hq.HqActivity
import com.songdosamgyeop.order.ui.login.PendingActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val vm: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vm.route.observe(viewLifecycleOwner) { dest ->
            when (dest) {
                SplashViewModel.Destination.ToLogin -> {
                    findNavController().navigate(
                        R.id.action_splash_to_login,
                        null,
                        navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    )
                }

                SplashViewModel.Destination.ToBranchHome -> {
                    // ✅ BranchActivity로
                    startActivity(Intent(requireContext(), BranchActivity::class.java))
                    requireActivity().finish()
                }

                SplashViewModel.Destination.ToHqHome -> {
                    // ✅ HqActivity로
                    startActivity(Intent(requireContext(), HqActivity::class.java))
                    requireActivity().finish()
                }

                SplashViewModel.Destination.ToBranchWaiting -> {
                    // ✅ PendingActivity(승인 대기)로 (또는 fragment로 유지하고 싶으면 nav로)
                    startActivity(Intent(requireContext(), PendingActivity::class.java))
                    requireActivity().finish()

                    // 만약 Pending을 Fragment로 유지하고 싶다면 아래로 대체:
                    // findNavController().navigate(
                    //     R.id.action_splash_to_branchWaiting,
                    //     null,
                    //     navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    // )
                }
            }
        }
        vm.decideRoute()
    }
}