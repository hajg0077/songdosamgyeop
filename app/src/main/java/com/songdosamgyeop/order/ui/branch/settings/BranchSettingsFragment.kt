package com.songdosamgyeop.order.ui.branch.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchSettingsBinding
import com.songdosamgyeop.order.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BranchSettingsFragment : Fragment(R.layout.fragment_branch_settings) {

    private var _b: FragmentBranchSettingsBinding? = null
    private val b get() = _b!!

    @Inject lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentBranchSettingsBinding.bind(view)

        // TODO: users/{uid}에서 실제 프로필 로드
        val user = auth.currentUser
        b.tvEmail.text = user?.email ?: getString(R.string.not_available)
        b.tvBranch.text = "송도 (BR001)" // TODO: branchName/branchId 바인딩
        b.tvRole.text = "BRANCH"       // TODO: users.role 바인딩
        b.tvVersion.text = getString(R.string.app_version_fmt, BuildConfig.VERSION_NAME)

        b.btnLogout.setOnClickListener {
            auth.signOut()
            Snackbar.make(b.root, R.string.logged_out, Snackbar.LENGTH_SHORT).show()

            // ✅ SplashFragment로 네비게이션 + 백스택 정리
            val nav = findNavController()
            // 그래프의 시작 목적지까지 모두 제거
            nav.popBackStack(nav.graph.startDestinationId, true)
            // 스플래시로 이동 (ID는 실제 그래프의 splash 프래그먼트 ID로 맞추세요)
            nav.navigate(R.id.splashFragment)
        }

        b.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}