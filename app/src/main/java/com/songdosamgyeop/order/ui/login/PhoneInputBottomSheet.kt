package com.songdosamgyeop.order.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.songdosamgyeop.order.R

class PhoneInputBottomSheet(
    private val onConfirm: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottomsheet_phone_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val til = view.findViewById<TextInputLayout>(R.id.tilPhone)
        val et = view.findViewById<TextInputEditText>(R.id.etPhone)
        val btnOk = view.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        btnOk.setOnClickListener {
            val raw = et.text?.toString()?.trim().orEmpty()
            val normalized = normalizeKrPhone(raw)
            if (normalized == null) {
                til.error = "전화번호 형식을 확인해주세요"
                return@setOnClickListener
            }
            til.error = null
            onConfirm(normalized)
            dismiss()
        }
        btnCancel.setOnClickListener { dismiss() }
    }

    /** 한국 휴대폰 간단 정규화: 숫자만 추출 → 010/011/016/017/018/019, 10~11자리 */
    private fun normalizeKrPhone(input: String): String? {
        val digits = input.filter { it.isDigit() }
        if (digits.length !in 10..11) return null
        val prefix = digits.take(3)
        if (prefix !in listOf("010","011","016","017","018","019")) return null
        return if (digits.length == 11)
            "${digits.substring(0,3)}-${digits.substring(3,7)}-${digits.substring(7)}"
        else
            "${digits.substring(0,3)}-${digits.substring(3,6)}-${digits.substring(6)}"
    }
}