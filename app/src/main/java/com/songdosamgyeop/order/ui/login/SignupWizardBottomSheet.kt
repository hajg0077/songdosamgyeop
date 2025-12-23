package com.songdosamgyeop.order.ui.login

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.RegistrationAddress

class SignupWizardBottomSheet(
    private val onConfirm: (
        email: String,
        password: String,
        phone: String,
        branchName: String,
        address: RegistrationAddress
    ) -> Unit
) : BottomSheetDialogFragment() {

    private var step = 1 // 1=account, 2=phone, 3=branch

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottomsheet_signup_wizard, null, false)
        dialog.setContentView(v)

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@setOnShowListener

            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = true
            behavior.peekHeight = resources.displayMetrics.heightPixels
        }

        val tvStepTitle = v.findViewById<TextView>(R.id.tvStepTitle)

        // ✅ 레이아웃에 이 3개 step 컨테이너가 있어야 함
        val stepAccount = v.findViewById<View>(R.id.stepAccount)
        val stepPhone = v.findViewById<View>(R.id.stepPhone)
        val stepBranch = v.findViewById<View>(R.id.stepBranch)

        // ✅ account inputs (레이아웃에 추가 필요)
        val etEmail = v.findViewById<TextInputEditText>(R.id.etEmail)
        val etPw = v.findViewById<TextInputEditText>(R.id.etPassword)

        // existing
        val etPhone = v.findViewById<TextInputEditText>(R.id.etPhone)
        val etBranchName = v.findViewById<TextInputEditText>(R.id.etBranchName)
        val etRoadAddr = v.findViewById<TextInputEditText>(R.id.etRoadAddr)
        val etZipNo = v.findViewById<TextInputEditText>(R.id.etZipNo)
        val etDetail = v.findViewById<TextInputEditText>(R.id.etDetail)

        val btnBack = v.findViewById<View>(R.id.btnBack)
        val btnNext = v.findViewById<View>(R.id.btnNext)
        val btnSubmit = v.findViewById<View>(R.id.btnSubmit)
        val btnCancel = v.findViewById<View>(R.id.btnCancel)

        fun render() {
            when (step) {
                1 -> {
                    tvStepTitle.text = "계정 입력"
                    stepAccount.visibility = View.VISIBLE
                    stepPhone.visibility = View.GONE
                    stepBranch.visibility = View.GONE
                    btnBack.visibility = View.GONE
                    btnNext.visibility = View.VISIBLE
                    btnSubmit.visibility = View.GONE
                }
                2 -> {
                    tvStepTitle.text = "전화번호 입력"
                    stepAccount.visibility = View.GONE
                    stepPhone.visibility = View.VISIBLE
                    stepBranch.visibility = View.GONE
                    btnBack.visibility = View.VISIBLE
                    btnNext.visibility = View.VISIBLE
                    btnSubmit.visibility = View.GONE
                }
                else -> {
                    tvStepTitle.text = "지사 정보 입력"
                    stepAccount.visibility = View.GONE
                    stepPhone.visibility = View.GONE
                    stepBranch.visibility = View.VISIBLE
                    btnBack.visibility = View.VISIBLE
                    btnNext.visibility = View.GONE
                    btnSubmit.visibility = View.VISIBLE
                }
            }
        }

        fun validAccount(): Boolean {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pw = etPw.text?.toString()?.trim().orEmpty()

            if (email.isBlank() || !email.contains("@")) {
                Toast.makeText(requireContext(), "이메일을 확인하세요.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (pw.length < 6) {
                Toast.makeText(requireContext(), "비밀번호는 6자 이상 입력하세요.", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        fun validPhone(): Boolean {
            val p = etPhone.text?.toString()?.trim().orEmpty()
            if (p.isBlank()) {
                Toast.makeText(requireContext(), "전화번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        fun submitIfValid() {
            if (!validAccount()) return
            if (!validPhone()) return

            val branchName = etBranchName.text?.toString()?.trim().orEmpty()
            val roadAddr = etRoadAddr.text?.toString()?.trim().orEmpty()
            val zipNo = etZipNo.text?.toString()?.trim().orEmpty()
            val detail = etDetail.text?.toString()?.trim().orEmpty()

            if (branchName.isBlank()) {
                Toast.makeText(requireContext(), "지사명을 입력하세요.", Toast.LENGTH_SHORT).show()
                return
            }
            if (roadAddr.isBlank() || zipNo.length != 5) {
                Toast.makeText(requireContext(), "주소/우편번호(5자리)를 확인하세요.", Toast.LENGTH_SHORT).show()
                return
            }

            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pw = etPw.text?.toString()?.trim().orEmpty()
            val phone = etPhone.text?.toString()?.trim().orEmpty()

            onConfirm(email, pw, phone, branchName, RegistrationAddress(roadAddr, zipNo, detail))
            dismiss()
        }

        btnNext.setOnClickListener {
            when (step) {
                1 -> if (validAccount()) { step = 2; render() }
                2 -> if (validPhone()) { step = 3; render() }
            }
        }

        btnBack.setOnClickListener {
            step = when (step) {
                2 -> 1
                3 -> 2
                else -> 1
            }
            render()
        }

        btnSubmit.setOnClickListener { submitIfValid() }
        btnCancel.setOnClickListener { dismiss() }

        render()
        return dialog
    }
}