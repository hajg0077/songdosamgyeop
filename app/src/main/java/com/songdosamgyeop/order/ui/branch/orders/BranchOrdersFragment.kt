package com.songdosamgyeop.order.ui.branch.orders

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentSimplePlaceholderBinding

/** 브랜치 주문 히스토리(플레이스홀더) */
class BranchOrdersFragment : Fragment(R.layout.fragment_simple_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSimplePlaceholderBinding.bind(view)
        b.tvTitle.text = "주문 히스토리"
        b.tvDesc.text = "추후: 내 주문 목록/상세"
    }
}