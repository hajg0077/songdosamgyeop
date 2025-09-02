package com.songdosamgyeop.order.ui.hq.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.songdosamgyeop.order.BuildConfig
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

/** HQ 설정: 계정/앱 정보, 피드백, 로그아웃 */
@AndroidEntryPoint
class HqSettingsFragment : Fragment(R.layout.fragment_hq_settings) {

    private val vm: HqSettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqSettingsBinding.bind(view)

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
            // 메일 클라이언트만 뜨도록
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("메일 앱이 없습니다. support@songdosamgyeop.com 으로 보내주세요.")
                    .setPositiveButton("확인", null)
                    .show()
            }
        }

        // 로그아웃 확인 다이얼로그
        b.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("로그아웃")
                .setMessage("로그아웃하시겠어요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("로그아웃") { _, _ -> vm.logout() }
                .show()
        }
    }
}