package com.songdosamgyeop.order.ui.hq.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.BuildConfig
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

/** HQ 설정: 계정/앱 정보, 피드백, 로그아웃 */
@AndroidEntryPoint
class HqSettingsFragment : Fragment(R.layout.fragment_hq_settings) {

    private val vm: HqSettingsViewModel by viewModels()
    private lateinit var b: FragmentHqSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b = FragmentHqSettingsBinding.bind(view)

        // 계정/역할 라벨 바인딩
        vm.email.observe(viewLifecycleOwner) { b.tvEmail.text = it }
        vm.roleBranch.observe(viewLifecycleOwner) { b.tvRoleBranch.text = it }

        // 버전 표기
        val buildType = if (BuildConfig.DEBUG) "debug" else "release"
        b.tvVersion.text = "버전 ${BuildConfig.VERSION_NAME} ($buildType)"

        // 피드백 메일: 기기/앱 정보 자동 포함
        b.btnFeedback.setOnClickListener {
            val body = buildString {
                appendLine("[Feedback]")
                appendLine()
                appendLine("---")
                appendLine("App: ${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME} ($buildType)")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            }
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@songdosamgyeop.com"))
                putExtra(Intent.EXTRA_SUBJECT, "[송도삼겹 HQ] 앱 피드백")
                putExtra(Intent.EXTRA_TEXT, body)
            }
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("메일 앱이 없습니다. support@songdosamgyeop.com 으로 보내주세요.")
                    .setPositiveButton("확인", null)
                    .show()
            }
        }

        // 로그아웃 버튼
        b.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("로그아웃")
                .setMessage("로그아웃하시겠어요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("로그아웃") { _, _ -> vm.logout() }
                .show()
        }

        // 로그아웃 상태 구독
        vm.logoutState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LogoutState.Idle -> Unit
                is LogoutState.Loading -> {
                    b.btnLogout.isEnabled = false
                    b.progress.visibility = View.VISIBLE
                }
                is LogoutState.Success -> {
                    b.progress.visibility = View.GONE
                    navigateToAuthAndClearBackstack()
                }
                is LogoutState.Error -> {
                    b.progress.visibility = View.GONE
                    b.btnLogout.isEnabled = true
                    Snackbar.make(b.root, state.message ?: "로그아웃 실패", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToAuthAndClearBackstack() {
        val nav = findNavController()
        val dest = R.id.loginFragment
        val opts = NavOptions.Builder()
            .setPopUpTo(nav.graph.id, true) // 전체 백스택 제거
            .build()
        nav.navigate(dest, null, opts)
    }
}