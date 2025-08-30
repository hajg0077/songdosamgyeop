package com.songdosamgyeop.order.ui.hq.registrationdetail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqRegistrationDetailBinding
import com.songdosamgyeop.order.ui.hq.registrationlist.HqRegistrationActionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * HQ 신청서 상세 화면 (MVVM 준수: 데이터는 VM, Fragment는 표시/입력만).
 */
@AndroidEntryPoint
class HqRegistrationDetailFragment
    : Fragment(R.layout.fragment_hq_registration_detail) {

    private val vm: HqRegistrationDetailViewModel by viewModels()
    private val actionsVm: HqRegistrationActionsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqRegistrationDetailBinding.bind(view)

        // Toolbar back
        b.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // UI 상태 구독: ViewModel이 로드한 데이터를 바인딩
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {
                vm.uiState.collectLatest { state ->
                    when (state) {
                        is HqRegistrationDetailViewModel.UiState.Loading -> {
                            // 필요 시 로딩 표시
                        }
                        is HqRegistrationDetailViewModel.UiState.Success -> {
                            val d = state.reg
                            b.tvBranchName.text = d.branchName
                            b.tvBranchCode.text = d.branchCode ?: "-"
                            b.tvName.text = d.name
                            b.tvEmail.text = d.email
                            b.tvPhone.text = d.phone ?: "-"
                            b.tvMemo.text = d.memo ?: "-"
                        }
                        HqRegistrationDetailViewModel.UiState.NotFound -> {
                            Snackbar.make(b.root, "신청서를 찾을 수 없습니다.", Snackbar.LENGTH_LONG).show()
                            findNavController().popBackStack()
                        }
                        is HqRegistrationDetailViewModel.UiState.Error -> {
                            Snackbar.make(b.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        // 승인
        b.btnApprove.setOnClickListener {
            showConfirmApproveDialog {
                setButtonsEnabled(b, false)
                actionsVm.approve(vm.docId)
            }
        }

        // 반려
        b.btnReject.setOnClickListener {
            showRejectDialog { reason ->
                if (reason == null) return@showRejectDialog
                setButtonsEnabled(b, false)
                actionsVm.reject(vm.docId, reason)
            }
        }

        // 처리 결과
        actionsVm.message.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Snackbar.make(b.root, it, Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure { e ->
                setButtonsEnabled(b, true)
                Snackbar.make(b.root, e.message ?: "처리에 실패했습니다.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /** 승인 확인 다이얼로그 */
    private fun showConfirmApproveDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("승인 처리")
            .setMessage("정말 승인하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("승인") { d, _ -> d.dismiss(); onConfirm() }
            .show()
    }

    /** 반려 사유 입력 다이얼로그 */
    private fun showRejectDialog(onResult: (reason: String?) -> Unit) {
        val input = TextInputEditText(requireContext()).apply {
            hint = "반려 사유 (선택)"
            setPadding(48, 24, 48, 0)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("신청서 반려")
            .setView(input)
            .setNegativeButton("취소") { d, _ -> d.dismiss(); onResult(null) }
            .setPositiveButton("반려") { d, _ ->
                d.dismiss(); onResult(input.text?.toString().orEmpty())
            }
            .show()
    }

    /** 승인/반려 버튼 활성/비활성 토글 */
    private fun setButtonsEnabled(b: FragmentHqRegistrationDetailBinding, enabled: Boolean) {
        b.btnApprove.isEnabled = enabled
        b.btnReject.isEnabled = enabled
    }
}
