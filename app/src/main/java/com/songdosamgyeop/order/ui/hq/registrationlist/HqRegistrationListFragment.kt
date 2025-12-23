package com.songdosamgyeop.order.ui.hq.registrationlist

import android.R.attr.colorError
import android.R.attr.colorPrimary
import com.songdosamgyeop.order.R
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
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
import com.songdosamgyeop.order.ui.common.SpacingItemDecoration
import com.songdosamgyeop.order.ui.common.showError
import com.songdosamgyeop.order.ui.common.showInfo
import android.content.DialogInterface
import com.songdosamgyeop.order.data.repo.Registration as RepoRegistration  // ✅ alias 로 통일
import android.graphics.drawable.ColorDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager

@AndroidEntryPoint
class HqRegistrationListFragment : Fragment(R.layout.fragment_hq_registration_list) {
    companion object {
        private const val KEY_INIT_FILTER = "KEY_INIT_FILTER"
    }
    private val vm: HqRegistrationListViewModel by viewModels()
    private val actionsVm: HqRegistrationActionsViewModel by viewModels()

    private lateinit var adapter: RegistrationAdapter

    private data class LastAction(val docId: String, val type: ActionType)
    private enum class ActionType { APPROVE, REJECT }
    private var lastAction: LastAction? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHqRegistrationListBinding.bind(view)

        adapter = RegistrationAdapter { id, reg ->
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
            requireActivity()
                .findNavController(R.id.hq_nav_host)
                .navigate(R.id.hqRegistrationDetailFragment, args)
        }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        // 🔧 여기서 repo 타입으로 맞춤
        vm.list.observe(viewLifecycleOwner) { list: List<Pair<String, RepoRegistration>> ->
            adapter.submitList(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val pos = holder.adapterPosition
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
                        if (reason.isNullOrBlank()) {
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
                    val colorPrimary = MaterialColors.getColor(item, colorPrimary)
                    val bg = ColorDrawable(colorPrimary)
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
                    val colorError = MaterialColors.getColor(item, colorError)
                    val bgErr = ColorDrawable(colorError)
                    bgErr.setBounds(item.right + dX.toInt(), item.top, item.right, item.bottom)
                    bgErr.draw(c)
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

        actionsVm.message.observe(viewLifecycleOwner) { res ->
            res.onSuccess { b.root.showInfo(it) }
                .onFailure {
                    vm.list.value?.let { current -> adapter.submitList(current) }
                    b.root.showError(it)
                }
        }

        b.etSearch.doOnTextChanged { text, _, _, _ ->
            vm.setQuery(text?.toString())
        }

        b.chipPending.setOnClickListener { vm.setStatus(RegistrationStatus.PENDING) }
        b.chipApproved.setOnClickListener { vm.setStatus(RegistrationStatus.APPROVED) }
        b.chipRejected.setOnClickListener { vm.setStatus(RegistrationStatus.REJECTED) }

        b.recycler.addItemDecoration(
            SpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.list_item_space))
        )

        val nav: NavController = requireActivity().findNavController(R.id.hq_nav_host)

        val fromHome = nav.previousBackStackEntry?.savedStateHandle
        fromHome?.getLiveData<Bundle>(KEY_INIT_FILTER)
            ?.observe(viewLifecycleOwner) { payload ->
                if (payload.getString("screen") == "registrations") {
                    payload.getString("status")?.let { statusStr ->
                        vm.setStatus(RegistrationStatus.valueOf(statusStr))
                    }
                }
                // ✅ remove가 없을 수 있으니 null로 지워버리기
                fromHome.set(KEY_INIT_FILTER, null)
            }
    }

    private fun removeFromList(docId: String) {
        val newList: List<Pair<String, RepoRegistration>> =
            adapter.currentList.filterNot { it.first == docId }
        adapter.submitList(newList)
    }

    private fun showUndoSnackbar(b: FragmentHqRegistrationListBinding, msg: String) {
        Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG)
            .setAction("되돌리기") { _: View ->
                val a = lastAction ?: return@setAction
                actionsVm.reset(a.docId)
                vm.list.value?.let { current -> adapter.submitList(current) }
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
            .setNegativeButton("취소") { dialog: DialogInterface, _ ->
                dialog.dismiss(); onResult(null)
            }
            .setPositiveButton("반려") { dialog: DialogInterface, _ ->
                dialog.dismiss()
                onResult(input.text?.toString())
            }
            .show()
    }
}