package com.songdosamgyeop.order.ui.branch.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchSettingsBinding
import com.songdosamgyeop.order.ui.SplashActivity // TODO: 실제 패키지 경로 확인
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BranchSettingsFragment : Fragment(R.layout.fragment_branch_settings) {

    private var _b: FragmentBranchSettingsBinding? = null
    private val b get() = _b!!

    @Inject lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentBranchSettingsBinding.bind(view)

        // TODO: users/{uid}에서 불러와서 채우기 (현재는 auth 정보만 표시)
        val user = auth.currentUser
        b.tvEmail.text = user?.email ?: getString(R.string.not_available)
        b.tvBranch.text = "송도 (BR001)" // TODO: 실제 branchName/branchId 바인딩
        b.tvRole.text = "BRANCH"       // TODO: users.role
        b.tvVersion.text = getString(R.string.app_version_fmt, BuildConfig.VERSION_NAME)

        b.btnLogout.setOnClickListener {
            auth.signOut()
            Snackbar.make(b.root, R.string.logged_out, Snackbar.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }

        b.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}