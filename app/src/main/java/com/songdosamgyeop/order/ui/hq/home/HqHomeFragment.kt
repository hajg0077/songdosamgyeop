package com.songdosamgyeop.order.ui.hq.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqHomeBinding
import com.songdosamgyeop.order.ui.common.NavKeys
import com.songdosamgyeop.order.ui.hq.orders.HqOrdersViewModel
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

        // --- 상태 구독: 로딩/에러/데이터 표시 + 카드별 인디케이터 토글 ---
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collectLatest { s ->
                // 상단 상태/에러 안내
                val showStatus = s.loading || s.error != null
                b.tvStatus.visibility = if (showStatus) View.VISIBLE else View.GONE
                b.tvStatus.text = when {
                    s.loading -> "로딩 중…"
                    s.error != null -> "오류: ${s.error}"
                    else -> ""
                }

                // 카드별 로딩 토글 (로딩 시 인디케이터, 평소에는 콘텐츠)
                toggleCardLoading(
                    content = b.boxTodayOrdersContent,
                    progress = b.progTodayOrders,
                    loading = s.loading
                )
                toggleCardLoading(
                    content = b.boxPendingRegsContent,
                    progress = b.progPendingRegs,
                    loading = s.loading
                )
                toggleCardLoading(
                    content = b.boxActiveOrdersContent,
                    progress = b.progActiveOrders,
                    loading = s.loading
                )

                if (!s.loading && s.error == null) {
                    b.tvTodayOrders.text =
                        "오늘 주문: ${nf.format(s.todayOrdersCount)}건 / ${nf.format(s.todayOrdersSum)}원"
                    b.tvPendingRegs.text =
                        "승인 대기 신청서: ${nf.format(s.pendingRegistrations)}건"
                    b.tvActiveOrders.text =
                        "진행 중 주문: ${nf.format(s.activeOrders)}건"
                }
            }
        }

        // --- 카드 클릭: 초기 필터 신호 세팅 → 목적지 이동 ---
        // nav_hq 그래프의 목적지 ID가 아래와 일치해야 합니다:
        // - R.id.nav_monitoring : HQ 주문 모니터링(HqOrdersFragment)
        // - R.id.nav_registrations : 신청서 목록(HqRegistrationListFragment)

        b.cardTodayOrders.setOnClickListener {
            findNavController().currentBackStackEntry?.savedStateHandle?.set(
                NavKeys.INIT_FILTER,
                bundleOf("screen" to "orders", "status" to "PENDING")
            )

            // ✅ 탭 이동은 navigate() 말고 bottomNav 선택으로
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.menu_monitoring
        }

        b.cardActiveOrders.setOnClickListener {
            findNavController().currentBackStackEntry?.savedStateHandle?.set(
                NavKeys.INIT_FILTER,
                bundleOf("screen" to "orders", "status" to "PENDING")
            )

            // ✅ 탭 이동은 navigate() 말고 bottomNav 선택으로
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.menu_monitoring
        }

        b.cardPendingRegs.setOnClickListener {
            findNavController().currentBackStackEntry?.savedStateHandle?.set(
                NavKeys.INIT_FILTER,
                bundleOf("screen" to "registrations", "status" to "PENDING")
            )

            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.menu_registrations
        }

        // 진입 시 데이터 로드
        vm.load()
    }

    /** 카드 로딩 토글 헬퍼 */
    private fun toggleCardLoading(content: View, progress: View, loading: Boolean) {
        content.visibility = if (loading) View.GONE else View.VISIBLE
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun openRegistrations(status: String) {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(NavKeys.REG_STATUS, status)
        findNavController().navigate(R.id.menu_registrations)
    }

    private fun openMonitoringWithStatus(status: String) {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(
            NavKeys.ORDERS_FILTER_STATUS,
            status
        )
        findNavController().navigate(R.id.menu_monitoring)
    }
}