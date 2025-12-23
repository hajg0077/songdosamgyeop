package com.songdosamgyeop.order.ui.hq.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqHomeBinding
import com.songdosamgyeop.order.ui.hq.orders.HqOrdersViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class HqHomeFragment : Fragment(R.layout.fragment_hq_home) {

    private val vm: HqHomeViewModel by viewModels()

    companion object {
        /** 홈 → 목적지(모니터링/신청서)로 초기 필터를 전달하는 키 */
        private const val KEY_INIT_FILTER = "KEY_INIT_FILTER"
    }

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
            // 홈 → 모니터링 탭으로 이동하면서 필터 조건(PENDING) 전달
            findNavController().currentBackStackEntry?.savedStateHandle?.set(
                "KEY_INIT_FILTER",
                bundleOf("screen" to "orders", "status" to "PENDING")
            )
            findNavController().navigate(R.id.menu_monitoring)
        }

        b.cardActiveOrders.setOnClickListener {
            // 모니터링을 APPROVED (출고 대기)로 열기 예시
            sendInitFilterAndNavigate(
                screen = "orders",
                status = "APPROVED",
                destId = R.id.menu_monitoring
            )
        }

        b.cardPendingRegs.setOnClickListener {
            // 신청서 탭을 PENDING 필터로 열기
            sendInitFilterAndNavigate(
                screen = "registrations",
                status = "PENDING",
                destId = R.id.menu_registrations
            )
        }

        // 진입 시 데이터 로드
        vm.load()
    }

    /** 카드 로딩 토글 헬퍼 */
    private fun toggleCardLoading(content: View, progress: View, loading: Boolean) {
        content.visibility = if (loading) View.GONE else View.VISIBLE
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    /**
     * 홈 → 목적지로 초기 필터 신호를 보낸 뒤 이동
     * SavedStateHandle 경로:
     *  - currentBackStackEntry.savedStateHandle.set(KEY_INIT_FILTER, Bundle)
     *  - 목적지 프래그먼트에서 previousBackStackEntry.savedStateHandle.getLiveData(...)로 수신
     */
    private fun sendInitFilterAndNavigate(screen: String, status: String, destId: Int) {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(
            KEY_INIT_FILTER,
            bundleOf("screen" to screen, "status" to status)
        )
        findNavController().navigate(destId)
    }
}