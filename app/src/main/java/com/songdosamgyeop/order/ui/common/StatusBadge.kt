package com.songdosamgyeop.order.ui.common

import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

/**
 * 주문 상태를 머티리얼 Chip으로 예쁘게 표시한다.
 * - PLACED: 확정(PrimaryContainer)
 * - DRAFT:  초안(SurfaceVariant)
 * - 기타:   보조(SecondaryContainer)
 */
fun applyOrderStatusChip(chip: Chip, status: String?) {
    val s = status ?: "UNKNOWN"
    val ctx = chip.context

    fun set(containerAttr: Int, onContainerAttr: Int, label: String) {
        chip.text = label
        chip.isCheckable = false
        chip.isClickable = false
        val color = MaterialColors.getColor(
            chip, containerAttr
        )
        chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
        chip.setTextColor(MaterialColors.getColor(chip, onContainerAttr))
    }

    when (s.uppercase()) {
        "PLACED" -> set(
            com.google.android.material.R.attr.colorPrimaryContainer,
            com.google.android.material.R.attr.colorOnPrimaryContainer,
            "확정"
        )
        "DRAFT" -> set(
            com.google.android.material.R.attr.colorSurfaceVariant,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            "초안"
        )
        else -> set(
            com.google.android.material.R.attr.colorSecondaryContainer,
            com.google.android.material.R.attr.colorOnSecondaryContainer,
            s
        )
    }
}