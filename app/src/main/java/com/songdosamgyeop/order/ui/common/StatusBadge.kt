package com.songdosamgyeop.order.ui.common

import android.content.res.ColorStateList
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.songdosamgyeop.order.R

/** 주문 상태 -> 칩 스타일 적용 */
object StatusBadge {
    enum class OrderStatus { DRAFT, PLACED, PREPARING, SHIPPED, CANCELED }

    fun apply(chip: Chip, status: OrderStatus) {
        val ctx = chip.context
        val (containerAttr, textAttr, label) = when (status) {
            OrderStatus.DRAFT     -> Triple(
                com.google.android.material.R.attr.colorSurfaceVariant,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                ctx.getString(R.string.badge_draft)
            )
            OrderStatus.PLACED    -> Triple(
                com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                ctx.getString(R.string.badge_placed)
            )
            OrderStatus.PREPARING -> Triple(
                com.google.android.material.R.attr.colorSecondaryContainer,
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                ctx.getString(R.string.badge_preparing)
            )
            OrderStatus.SHIPPED   -> Triple(
                com.google.android.material.R.attr.colorTertiaryContainer,
                com.google.android.material.R.attr.colorOnTertiaryContainer,
                ctx.getString(R.string.badge_shipped)
            )
            OrderStatus.CANCELED  -> Triple(
                com.google.android.material.R.attr.colorErrorContainer,
                com.google.android.material.R.attr.colorOnErrorContainer,
                ctx.getString(R.string.badge_canceled)
            )
        }
        val bg = MaterialColors.getColor(chip, containerAttr)
        val fg = MaterialColors.getColor(chip, textAttr)

        chip.text = label
        chip.chipBackgroundColor = ColorStateList.valueOf(bg)
        chip.setTextColor(fg)
        chip.isClickable = false
        chip.isCheckable = false
    }
}