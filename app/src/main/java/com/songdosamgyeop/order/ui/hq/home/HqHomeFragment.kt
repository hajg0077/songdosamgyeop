package com.songdosamgyeop.order.ui.hq.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class HqHomeFragment : Fragment(R.layout.fragment_hq_home) {

    private val vm: HqHomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqHomeBinding.bind(view)
        val nf = NumberFormat.getNumberInstance(Locale.KOREA)




        // UI 상태 구독
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                b.tvStatus.visibility = if (s.loading || s.error != null) View.VISIBLE else View.GONE
                // inside onViewCreated -> collectLatest { s -> ... }
                val showLoading = s.loading
// Today
                b.progTodayOrders.visibility = if (showLoading) View.VISIBLE else View.GONE
                b.boxTodayOrdersContent.visibility = if (showLoading) View.GONE else View.VISIBLE
// Pending Regs
                b.progPendingRegs.visibility = if (showLoading) View.VISIBLE else View.GONE
                b.boxPendingRegsContent.visibility = if (showLoading) View.GONE else View.VISIBLE
// Active Orders
                b.progActiveOrders.visibility = if (showLoading) View.VISIBLE else View.GONE
                b.boxActiveOrdersContent.visibility = if (showLoading) View.GONE else View.VISIBLE

                if (!s.loading && s.error == null) {
                    b.tvTodayOrders.text = "오늘 주문: ${s.todayOrdersCount}건 / ${nf.format(s.todayOrdersSum)}원"
                    b.tvPendingRegs.text = "승인 대기 신청서: ${nf.format(s.pendingRegistrations)}건"
                    b.tvActiveOrders.text = "진행 중 주문: ${nf.format(s.activeOrders)}건"
                }
            }
        }

        // 카드 클릭 → 해당 탭으로 이동
        // nav_hq 그래프에서 목적지 id: nav_registrations, nav_monitoring 이라고 가정
        b.cardTodayOrders.setOnClickListener {
            findNavController().navigate(R.id.nav_monitoring)
        }
        b.cardActiveOrders.setOnClickListener {
            findNavController().navigate(R.id.nav_monitoring)
        }
        b.cardPendingRegs.setOnClickListener {
            findNavController().navigate(R.id.nav_registrations)
        }

        // 진입 시 로드
        vm.load()

        // 홈에서 온 초기 필터 신호 수신
        val handleFromHome = findNavController().previousBackStackEntry?.savedStateHandle
        handleFromHome?.getLiveData<Bundle>(KEY_INIT_FILTER)
            ?.observe(viewLifecycleOwner) { payload ->
                val screen = payload.getString("screen")
                if (screen == "orders") {
                    val status = payload.getString("status")
                    status?.let { vm.setStatus(it) }
                }
                // 받았으면 소비
                handleFromHome.remove<Bundle>(KEY_INIT_FILTER)
            }
    }
}