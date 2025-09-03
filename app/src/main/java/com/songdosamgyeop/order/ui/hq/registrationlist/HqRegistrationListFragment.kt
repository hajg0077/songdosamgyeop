package com.songdosamgyeop.order.ui.hq.registrationlist

import com.songdosamgyeop.order.R
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf // ✅ Safe Args 대신 번들 네비게이션
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.songdosamgyeop.order.core.model.RegistrationStatus
import com.songdosamgyeop.order.databinding.FragmentHqRegistrationListBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable
import com.songdosamgyeop.order.ui.common.SpacingItemDecoration

@AndroidEntryPoint
class HqRegistrationListFragment : Fragment(R.layout.fragment_hq_registration_list) {
    private val vm: HqRegistrationListViewModel by viewModels()
    private val actionsVm: HqRegistrationActionsViewModel by viewModels()

    private lateinit var adapter: RegistrationAdapter

    private data class LastAction(val docId: String, val type: ActionType)
    private enum class ActionType { APPROVE, REJECT }
    private var lastAction: LastAction? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqRegistrationListBinding.bind(view)

        adapter = RegistrationAdapter { id, reg ->
            // ✅ Safe Args 제거: 번들로 직접 전달
            val actionId = R.id.action_hqRegistrationListFragment_to_hqRegistrationDetailFragment
            val args = bundleOf(
                "id" to id,
                "email" to reg.email,
                "name" to reg.name,
                "branchName" to reg.branchName,
                "branchCode" to reg.branchCode,
                "phone" to reg.phone,
                "memo" to reg.memo
            )
            findNavController().navigate(actionId, args)
        }
        b.recycler.adapter = adapter

        // ❌ vm.pendingList ... (전부 삭제)
        // ✅ 통합된 목록 LiveData 사용
        vm.list.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // 스와이프
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val pos = holder.adapterPosition // ✅ bindingAdapterPosition → absoluteAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val item = adapter.currentList.getOrNull(pos)
                if (item == null) { adapter.notifyItemChanged(pos); return }
                val (docId, reg) = item

                if (direction == ItemTouchHelper.RIGHT) {
                    lastAction = LastAction(docId, ActionType.APPROVE)
                    actionsVm.approve(docId)
                    removeFromList(docId)
                    showUndoSnackbar(b, "승인 처리됨")
                } else {
                    showRejectDialog { reason ->
                        if (reason == null) {
                            adapter.notifyItemChanged(pos)
                        } else {
                            lastAction = LastAction(docId, ActionType.REJECT)
                            actionsVm.reject(docId, reason)
                            removeFromList(docId)
                            showUndoSnackbar(b, "반려 처리됨")
                        }
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
                val item = vh.itemView
                val ctx = item.context
                val height = item.bottom - item.top
                val iconMargin = height / 4

                if (dX > 0) {
                    val bg = MaterialColors.getColor(item, com.google.android.material.R.attr.colorPrimary).toDrawable()
                    bg.setBounds(item.left, item.top, item.left + dX.toInt(), item.bottom)
                    bg.draw(c)
                    val icon = ContextCompat.getDrawable(ctx, R.drawable.ic_check_24)
                    icon?.let {
                        val top = item.top + iconMargin
                        val bottom = item.bottom - iconMargin
                        val left = item.left + iconMargin
                        val right = left + (bottom - top)
                        it.setBounds(left, top, right, bottom)
                        it.draw(c)
                    }
                } else if (dX < 0) {
                    val bg = MaterialColors.getColor(item, com.google.android.material.R.attr.colorError).toDrawable()
                    bg.setBounds(item.right + dX.toInt(), item.top, item.right, item.bottom)
                    bg.draw(c)
                    val icon = ContextCompat.getDrawable(ctx, R.drawable.ic_close_24)
                    icon?.let {
                        val top = item.top + iconMargin
                        val bottom = item.bottom - iconMargin
                        val right = item.right - iconMargin
                        val left = right - (bottom - top)
                        it.setBounds(left, top, right, bottom)
                        it.draw(c)
                    }
                }
            }
        })
        touchHelper.attachToRecyclerView(b.recycler)

        // 결과 알림
        actionsVm.message.observe(viewLifecycleOwner) { res ->
            res.onSuccess {
                Snackbar.make(b.root, it, Snackbar.LENGTH_SHORT).show()
            }.onFailure {
                // 서버 실패 → 목록 복구
                vm.list.value?.let { current -> adapter.submitList(current) } // ✅ pendingList → list
                Snackbar.make(b.root, it.message ?: "처리에 실패했습니다.", Snackbar.LENGTH_LONG).show()
            }
        }

        // 검색창 연결
        b.etSearch.doOnTextChanged { text, _, _, _ ->
            vm.setQuery(text?.toString().orEmpty())
        }

        // 상태 칩 연결
        b.chipPending.setOnClickListener { vm.setStatus(RegistrationStatus.PENDING) }
        b.chipApproved.setOnClickListener { vm.setStatus(RegistrationStatus.APPROVED) }
        b.chipRejected.setOnClickListener { vm.setStatus(RegistrationStatus.REJECTED) }

        b.recycler.addItemDecoration(
            SpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.list_item_space))
        )
    }

    private fun removeFromList(docId: String) {
        val newList = adapter.currentList.filterNot { it.first == docId }
        adapter.submitList(newList)
    }

    private fun showUndoSnackbar(b: FragmentHqRegistrationListBinding, msg: String) {
        // ✅ 이상한 토큰 제거 (nvm - v)
        Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG)
            .setAction("되돌리기") {
                val a = lastAction ?: return@setAction
                when (a.type) {
                    ActionType.APPROVE -> actionsVm.reject(a.docId, "Undo via UI")
                    ActionType.REJECT -> actionsVm.approve(a.docId)
                }
                // 임시 복구: 최신 상태 재적용
                vm.list.value?.let { current -> adapter.submitList(current) } // ✅ pendingList → list
            }
            .show()
    }

    private fun showRejectDialog(onResult: (reason: String?) -> Unit) {
        val input = TextInputEditText(requireContext()).apply {
            hint = "반려 사유 (선택)"
            setPadding(48, 24, 48, 0)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("신청서 반려")
            .setView(input)
            .setNegativeButton("취소") { d, _ -> d.dismiss(); onResult(null) }
            .setPositiveButton("반려") { d, _ ->
                d.dismiss()
                onResult(input.text?.toString())
            }
            .show()
    }
}