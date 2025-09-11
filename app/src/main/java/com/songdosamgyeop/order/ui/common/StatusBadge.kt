package com.songdosamgyeop.order.ui.common

import android.content.res.ColorStateList
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.OrderStatus

/** 주문 상태 -> 칩 스타일 적용 (Material3 팔레트) */
object StatusBadge {
    fun apply(chip: Chip, status: OrderStatus) {
        val (containerAttr: Int, textAttr: Int, labelRes: Int) = when (status) {
            OrderStatus.PENDING -> Triple(
                com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                R.string.badge_pending
            )
            OrderStatus.APPROVED -> Triple(
                com.google.android.material.R.attr.colorSecondaryContainer,
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                R.string.badge_approved
            )
            OrderStatus.REJECTED -> Triple(
                com.google.android.material.R.attr.colorErrorContainer,
                com.google.android.material.R.attr.colorOnErrorContainer,
                R.string.badge_rejected
            )
            OrderStatus.SHIPPED -> Triple(
                com.google.android.material.R.attr.colorTertiaryContainer,
                com.google.android.material.R.attr.colorOnTertiaryContainer,
                R.string.badge_shipped
            )
            OrderStatus.DELIVERED -> Triple(
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                com.google.android.material.R.attr.colorOnSurface,
                R.string.badge_delivered
            )
        }
        val bg = MaterialColors.getColor(chip, containerAttr)
        val fg = MaterialColors.getColor(chip, textAttr)
        chip.text = chip.context.getString(labelRes)
        chip.chipBackgroundColor = ColorStateList.valueOf(bg)
        chip.setTextColor(fg)
        chip.isClickable = false
        chip.isCheckable = false
    }
}

// 문자열 -> enum 변환 헬퍼 (어댑터에서 사용)
fun applyOrderStatusChip(chip: Chip, status: String?) {
    val enumVal = runCatching { OrderStatus.valueOf(status?.uppercase() ?: "") }.getOrNull()
        ?: OrderStatus.PENDING
    StatusBadge.apply(chip, enumVal)
}