package com.songdosamgyeop.order.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 세로 리스트 아이템 간격을 주는 데코레이션.
 * @param spacePx 아이템 아래쪽 여백(px)
 */
class SpacingItemDecoration(private val spacePx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
        val pos = parent.getChildAdapterPosition(v)
        if (pos != RecyclerView.NO_POSITION) outRect.bottom = spacePx
    }
}