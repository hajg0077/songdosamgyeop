package com.songdosamgyeop.order.ui.branch.waiting

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BranchWaitingFragment : Fragment(R.layout.fragment_branch_waiting) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private var job: Job? = null

    override fun onResume() {
        super.onResume()
        job = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val user = auth.currentUser ?: return@launch

                val role = runCatching {
                    val token = user.getIdToken(true).await()
                    token.claims["role"] as? String ?: ""
                }.getOrDefault("")

                if (role == "BRANCH" || role == "HQ") {
                    findNavController().navigate(
                        R.id.splashFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.branchWaitingFragment) { inclusive = true }
                        }
                    )
                    return@launch
                }

                delay(2000)
            }
        }
    }

    override fun onPause() {
        job?.cancel()
        job = null
        super.onPause()
    }
}