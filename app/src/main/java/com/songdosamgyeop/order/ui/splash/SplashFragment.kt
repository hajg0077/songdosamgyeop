package com.songdosamgyeop.order.ui.splash

import com.songdosamgyeop.order.R
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {
    private val vm: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vm.route.observe(viewLifecycleOwner) { dest ->
            when (dest) {
                SplashViewModel.Destination.ToLogin ->
                    findNavController().navigate(R.id.action_splash_to_login)
                SplashViewModel.Destination.ToBranchHome ->
                    findNavController().navigate(R.id.action_splash_to_branchHome)
                SplashViewModel.Destination.ToHqHome ->
                    findNavController().navigate(R.id.action_splash_to_hqHome)
            }
        }
        vm.decideRoute()
    }
}