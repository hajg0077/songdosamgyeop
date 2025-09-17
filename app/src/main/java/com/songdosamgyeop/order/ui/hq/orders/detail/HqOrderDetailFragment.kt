package com.songdosamgyeop.order.ui.hq.orders.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentHqOrderDetailBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip   // ✅ 이 헬퍼 사용
import com.songdosamgyeop.order.ui.common.showError
import com.songdosamgyeop.order.ui.common.showInfo
import com.songdosamgyeop.order.ui.hq.orders.HqOrderActionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class HqOrderDetailFragment : Fragment(R.layout.fragment_hq_order_detail) {

    companion object {
        private const val KEY_ORDER_UPDATED = "KEY_ORDER_UPDATED"
    }

    private var currentStatus: String? = null

    private val vm: HqOrderDetailViewModel by viewModels()
    private val actionsVm: HqOrderActionsViewModel by viewModels()

    private lateinit var adapter: HqOrderItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqOrderDetailBinding.bind(view)
        adapter = HqOrderItemsAdapter()
        b.recycler.adapter = adapter

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

            currentStatus = h.status
            requireActivity().invalidateOptionsMenu()

            b.tvBranch.text = h.branchId

            // ✅ 상태 칩 적용 (헬퍼 사용)
            applyOrderStatusChip(b.chipStatus, h.status)

            // 주문 시각
            b.tvWhen.text = formatWhen(sdf, h.placedAt, h.createdAt)

            // 합계/아이템 개수/짧은 ID
            val amount = h.totalAmount?.let { "${nf.format(it)}원" } ?: "—"
            val count  = h.itemsCount?.let { "${it}개" } ?: "—"
            val shortId = h.id.take(8)         // id는 non-null
            b.tvSummary.text = getString(R.string.hq_order_detail_summary, amount, count, shortId)
        }

        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_hq_order_detail, menu)   // ✅ 메뉴 리소스 생성 필요 (아래 2번)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val visible = visibleActions(currentStatus)

        menu.findItem(R.id.action_approve)?.isVisible = "APPROVED" in visible
        menu.findItem(R.id.action_reject )?.isVisible = "REJECTED" in visible
        menu.findItem(R.id.action_ship   )?.isVisible = "SHIPPED"  in visible
        menu.findItem(R.id.action_deliver)?.isVisible = "DELIVERED" in visible

        val disabled = (actionsVm.busy.value == true)
        menu.findItem(R.id.action_approve)?.isEnabled = !disabled
        menu.findItem(R.id.action_reject )?.isEnabled = !disabled
        menu.findItem(R.id.action_ship   )?.isEnabled = !disabled
        menu.findItem(R.id.action_deliver)?.isEnabled = !disabled
    }

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

    /** 전이 규칙 */
    private fun visibleActions(current: String?): Set<String> = when (current?.uppercase(Locale.ROOT)) {
        "PENDING"  -> setOf("APPROVED", "REJECTED")
        "APPROVED" -> setOf("SHIPPED")
        "SHIPPED"  -> setOf("DELIVERED")
        else       -> emptySet()
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
            is String -> ts
            else -> "-"
        }
    }
}