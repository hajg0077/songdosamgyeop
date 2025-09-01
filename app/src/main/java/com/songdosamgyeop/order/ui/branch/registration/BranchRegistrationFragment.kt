package com.songdosamgyeop.order.ui.branch.registration

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BranchRegistrationFragment : Fragment(R.layout.fragment_branch_registration) {

    private val vm: BranchRegistrationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentBranchRegistrationBinding.bind(view)

        // 저장 버튼
        b.btnSubmit.setOnClickListener {
            vm.submit(
                email = b.etEmail.text?.toString().orEmpty(),
                name = b.etName.text?.toString().orEmpty(),
                branchName = b.etBranchName.text?.toString().orEmpty(),
                branchCode = b.etBranchCode.text?.toString(),
                phone = b.etPhone.text?.toString(),
                memo = b.etMemo.text?.toString()
            )
        }

        // 로딩 표시(예: 버튼 비활성화)
        vm.loading.observe(viewLifecycleOwner) { loading ->
            b.btnSubmit.isEnabled = !loading
            b.progress.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // 결과 처리
        vm.submitResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { docId ->
                Snackbar.make(b.root, "신청 완료! (id=$docId)", Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(
                    R.id.action_branchRegistration_to_branchWaiting
                )
            }.onFailure { e ->
                Snackbar.make(b.root, e.message ?: "저장에 실패했습니다.", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}