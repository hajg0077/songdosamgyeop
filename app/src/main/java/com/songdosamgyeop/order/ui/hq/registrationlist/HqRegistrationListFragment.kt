package com.songdosamgyeop.order.ui.hq.registrationlist

import com.songdosamgyeop.order.R
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.songdosamgyeop.order.databinding.FragmentHqRegistrationListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HqRegistrationListFragment : Fragment(R.layout.fragment_hq_registration_list) {
    private val vm: HqRegistrationListViewModel by viewModels()
    private val actionsVm: HqRegistrationActionsViewModel by viewModels()

    private lateinit var adapter: RegistrationAdapter

    // 마지막 동작 기록 → 스낵바 “되돌리기” 시 반대 동작 수행
    private data class LastAction(val docId: String, val type: ActionType)
    private enum class ActionType { APPROVE, REJECT }
    private var lastAction: LastAction? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqRegistrationListBinding.bind(view)

        adapter = RegistrationAdapter { id, reg ->
            val action = HqRegistrationListFragmentDirections
                .actionHqRegistrationListFragmentToHqRegistrationDetailFragment(
                    id, reg.email, reg.name, reg.branchName, reg.branchCode, reg.phone, reg.memo
                )
            findNavController().navigate(action)
        }
        b.recycler.adapter = adapter

        vm.pendingList.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        // 스와이프
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val pos = holder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(pos)
                if (item == null) { adapter.notifyItemChanged(pos); return }
                val (docId, reg) = item

                if (direction == ItemTouchHelper.RIGHT) {
                    // 승인
                    lastAction = LastAction(docId, ActionType.APPROVE)
                    actionsVm.approve(docId)
                    // 낙관적 제거: PENDING 목록에서 사라지게 즉시 필터링
                    removeFromList(docId)
                    showUndoSnackbar(b, "승인 처리됨")
                } else {
                    // 반려(사유 입력)
                    showRejectDialog { reason ->
                        if (reason == null) {
                            adapter.notifyItemChanged(pos) // 취소
                        } else {
                            lastAction = LastAction(docId, ActionType.REJECT)
                            actionsVm.reject(docId, reason)
                            removeFromList(docId)
                            showUndoSnackbar(b, "반려 처리됨")
                        }
                    }
                }
            }

            // 배경 + 아이콘 그리기
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
                    // Approve: 초록 배경 + 체크
                    val bg = ColorDrawable(
                        MaterialColors.getColor(
                            item,
                            com.google.android.material.R.attr.colorPrimary
                        )
                    )
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
                    // Reject: 빨강 배경 + X
                    val bg = ColorDrawable(MaterialColors.getColor(item, com.google.android.material.R.attr.colorError))
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
                vm.pendingList.value?.let { current -> adapter.submitList(current) }
                Snackbar.make(b.root, it.message ?: "처리에 실패했습니다.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun removeFromList(docId: String) {
        val newList = adapter.currentList.filterNot { it.first == docId }
        adapter.submitList(newList)
    }

    private fun showUndoSnackbar(b: FragmentHqRegistrationListBinding, msg: String) {
        Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG)
            .setAction("되돌리기") {
                val a = lastAction ?: return@setAction
                // 반대 동작 호출
                when (a.type) {
                    ActionType.APPROVE -> actionsVm.reject(a.docId, "Undo via UI")
                    ActionType.REJECT -> actionsVm.approve(a.docId)
                }
                // 화면 목록도 반대 동작 가정 하에 복구: (서버가 다시 PENDING으로 만드는 게 아니라면, HQ용에서 보일 리스트는 데이터 소스 기준으로 재구독됨)
                // 임시로 전체 새로고침 유도
                vm.pendingList.value?.let { current -> adapter.submitList(current) }
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