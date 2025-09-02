package com.songdosamgyeop.order.ui.hq.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentSimplePlaceholderBinding

/** HQ 설정(플레이스홀더) */
class HqSettingsFragment : Fragment(R.layout.fragment_simple_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSimplePlaceholderBinding.bind(view)
        b.tvTitle.text = "설정"
        b.tvDesc.text = "추후: 알림/권한/로그아웃 등"
    }
}