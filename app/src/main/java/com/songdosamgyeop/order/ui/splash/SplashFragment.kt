package com.songdosamgyeop.order.ui.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.songdosamgyeop.order.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val vm: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vm.route.observe(viewLifecycleOwner) { dest ->
            val nav = findNavController()

            when (dest) {
                SplashViewModel.Destination.ToLogin -> {
                    nav.navigate(
                        R.id.action_splash_to_login,
                        null,
                        navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    )
                }

                SplashViewModel.Destination.ToBranchHome -> {
                    nav.navigate(
                        R.id.action_splash_to_branchHome,
                        null,
                        navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    )
                }

                SplashViewModel.Destination.ToHqHome -> {
                    nav.navigate(
                        R.id.action_splash_to_hqHome,
                        null,
                        navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    )
                }

                // ✅ 추가: 승인 대기 화면
                SplashViewModel.Destination.ToBranchWaiting -> {
                    nav.navigate(
                        R.id.action_splash_to_branchWaiting,
                        null,
                        navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    )
                }
            }
        }

        vm.decideRoute()
    }
}