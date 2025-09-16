package com.songdosamgyeop.order.ui.hq.orders.detail

import android.os.Bundle
import android.view.* // onCreateOptionsMenu/ItemSelected/Prepare 용
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrderDetailBinding
import com.songdosamgyeop.order.ui.common.StatusBadge
import com.songdosamgyeop.order.ui.common.showError
import com.songdosamgyeop.order.ui.common.showInfo
import com.songdosamgyeop.order.ui.hq.orders.HqOrderActionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * HQ 주문 상세 화면
 * - 기존: 읽기 전용
 * - 이번 패치: 툴바 메뉴에서 상태 변경(Functions 호출) + 전이 규칙 기반 노출 제어
 */
@AndroidEntryPoint
class HqOrderDetailFragment : Fragment(R.layout.fragment_hq_order_detail) {

    companion object {
        private const val KEY_ORDER_UPDATED = "KEY_ORDER_UPDATED"
    }

    /** 현재 주문 상태 (메뉴 노출/비활성화 판단에 사용) */
    private var currentStatus: String? = null

    private val vm: HqOrderDetailViewModel by viewModels()
    private val actionsVm: HqOrderActionsViewModel by viewModels()

    private lateinit var adapter: HqOrderItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // 툴바 메뉴 사용
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqOrderDetailBinding.bind(view)
        adapter = HqOrderItemsAdapter()
        b.recycler.adapter = adapter

        // 툴바 뒤로가기
        b.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val nf = NumberFormat.getNumberInstance(Locale.KOREA)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }

        actionsVm.message.observe(viewLifecycleOwner) { res ->
            res.onSuccess { b.root.showInfo(it) }
                .onFailure { b.root.showError(it) }
            requireActivity().invalidateOptionsMenu()
        }
        actionsVm.busy.observe(viewLifecycleOwner) { refreshMenu() }

        // 헤더 구독
        vm.header.observe(viewLifecycleOwner) { h ->
            if (h == null) {
                currentStatus = null
                requireActivity().invalidateOptionsMenu()

                b.tvBranch.text = getString(R.string.msg_order_not_found)
                b.chipStatus.visibility = View.GONE
                b.tvWhen.text = "-"
                b.tvSummary.text = "-"
                return@observe
            }

            // 상태 보관 → 메뉴 업데이트
            currentStatus = h.status
            requireActivity().invalidateOptionsMenu()

            // 지점 정보 (필요하면 branchName으로 교체)
            b.tvBranch.text = h.branchId

            // 상태 칩
            applyStatusChip(b.chipStatus, h.status)

            // 주문 시각
            val whenStr = formatWhen(sdf, h.placedAt, h.createdAt)
            b.tvWhen.text = whenStr

            // 합계/아이템 개수/짧은 ID
            val amount = h.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
            val count  = h.itemsCount?.let { "${it}개" } ?: "—"
            val shortId = h.id?.take(8) ?: "—"
            b.tvSummary.text = getString(R.string.hq_order_detail_summary, amount, count, shortId)
        }

        // 아이템 구독
        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    // 메뉴 생성 (승인/반려/출고/배송완료)
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_hq_order_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // 메뉴 노출/비활성화 제어 (현재 상태 + 진행 여부 반영)
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        // 1) 전이 규칙에 따라 보여줄 액션 결정
        val visible = visibleActions(currentStatus)

        // 2) 보임/숨김
        menu.findItem(R.id.action_approve)?.isVisible = "APPROVED" in visible
        menu.findItem(R.id.action_reject )?.isVisible = "REJECTED" in visible
        menu.findItem(R.id.action_ship   )?.isVisible = "SHIPPED"  in visible
        menu.findItem(R.id.action_deliver)?.isVisible = "DELIVERED" in visible

        // 3) 진행 중 비활성화
        val disabled = (actionsVm.busy.value == true)
        menu.findItem(R.id.action_approve)?.isEnabled = !disabled
        menu.findItem(R.id.action_reject )?.isEnabled = !disabled
        menu.findItem(R.id.action_ship   )?.isEnabled = !disabled
        menu.findItem(R.id.action_deliver)?.isEnabled = !disabled
    }

    // 메뉴 클릭 시 Cloud Functions 호출
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val orderId = vm.header.value?.id ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_approve -> { actionsVm.updateStatus(orderId, "APPROVED"); true }
            R.id.action_reject  -> { actionsVm.updateStatus(orderId, "REJECTED"); true }
            R.id.action_ship    -> { actionsVm.updateStatus(orderId, "SHIPPED");  true }
            R.id.action_deliver -> { actionsVm.updateStatus(orderId, "DELIVERED"); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** 주문 상태 문자열 -> 배지 스타일 적용 */
    private fun applyStatusChip(chip: Chip, status: String?) {
        // 내부 enum 이름이 State라고 가정 (프로젝트 내 StatusBadge 파일 확인 필수!)
        val mapped = when (status?.uppercase(Locale.ROOT)) {
            "APPROVED"  -> StatusBadge.State.PREPARING
            "REJECTED"  -> StatusBadge.State.CANCELED
            "SHIPPED"   -> StatusBadge.State.SHIPPED
            "DELIVERED" -> runCatching {
                java.lang.Enum.valueOf(StatusBadge.State::class.java, "DELIVERED")
            }.getOrDefault(StatusBadge.State.SHIPPED) // 없으면 SHIPPED로 대체
            "PENDING"   -> StatusBadge.State.PLACED
            else        -> StatusBadge.State.DRAFT
        }
        StatusBadge.apply(chip, mapped)
        chip.visibility = View.VISIBLE
    }

    /** 전이 규칙: 서버와 동일 (PENDING→APPROVED/REJECTED, APPROVED→SHIPPED, SHIPPED→DELIVERED) */
    private fun visibleActions(current: String?): Set<String> = when (current?.uppercase(Locale.ROOT)) {
        "PENDING"  -> setOf("APPROVED", "REJECTED")
        "APPROVED" -> setOf("SHIPPED")
        "SHIPPED"  -> setOf("DELIVERED")
        else       -> emptySet() // REJECTED/DELIVERED/기타 완료 상태
    }

    private fun refreshMenu() {
        requireActivity().invalidateOptionsMenu()
    }

    private fun formatWhen(
        sdf: SimpleDateFormat,
        placedAt: Any?,
        createdAt: Any?
    ): String {
        val ts = placedAt ?: createdAt ?: return "-"
        return when (ts) {
            is com.google.firebase.Timestamp -> sdf.format(ts.toDate())
            is java.util.Date -> sdf.format(ts)
            is Long -> sdf.format(java.util.Date(ts))
            is String -> ts // 이미 포맷된 문자열인 경우
            else -> "-"
        }
    }
}