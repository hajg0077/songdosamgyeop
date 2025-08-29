package com.songdosamgyeop.order.data.ui.branch.registration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.widget.doAfterTextChanged
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
    private var _b: FragmentBranchRegistrationBinding? = null
    private val b get() = _b!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentBranchRegistrationBinding.bind(view)

        // 입력 변화 → ViewModel로 반영 + 버튼 활성화 갱신
        listOf(
            b.inputEmail to vm.email,
            b.inputName to vm.name,
            b.inputBranchName to vm.branchName,
            b.inputBranchCode to vm.branchCode,
            b.inputPhone to vm.phone,
            b.inputMemo to vm.memo
        ).forEach { (edit, live) ->
            edit.doAfterTextChanged { s -> live.value = s?.toString().orEmpty(); updateSubmitEnabled() }
        }

        b.btnSubmit.setOnClickListener {
            b.progress.visibility = View.VISIBLE
            b.btnSubmit.isEnabled = false
            vm.submit()
        }

        vm.submitting.observe(viewLifecycleOwner) { loading ->
            b.progress.visibility = if (loading) View.VISIBLE else View.GONE
            if (!loading) updateSubmitEnabled()
        }

        vm.result.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Snackbar.make(b.root, getString(R.string.msg_submit_ok), Snackbar.LENGTH_LONG).show()
                findNavController().navigate(R.id.branchWaitingFragment)
            }.onFailure { t ->
                val msg = t.message ?: getString(R.string.msg_submit_fail)
                Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show()
            }
        }

        updateSubmitEnabled()
    }

    private fun updateSubmitEnabled() {
        b.btnSubmit.isEnabled = vm.isFormValid() && vm.submitting.value != true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}