package com.songdosamgyeop.order.ui.branch.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.databinding.FragmentBranchHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class BranchOrderHistoryFragment : Fragment() {

    private var _binding: FragmentBranchHistoryBinding? = null
    private val binding get() = _binding!!

    private val vm: BranchOrderHistoryViewModel by viewModels()
    private lateinit var adapter: BranchHistoryAdapter

    private val moneyFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBranchHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        adapter = BranchHistoryAdapter(
            onClick = { order ->
                findNavController().navigate(
                    R.id.action_branchOrderHistory_to_branchOrderDetail,
                    Bundle().apply { putString("orderId", order.id) }
                )
            },
            onCopyId = { orderId ->
                Snackbar.make(binding.root, getString(R.string.copied_order_id), Snackbar.LENGTH_SHORT).show()
                requireContext().getSystemService(android.content.ClipboardManager::class.java)
                    ?.setPrimaryClip(android.content.ClipData.newPlainText("orderId", orderId))
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        // 검색 입력 디바운스 트리거
        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vm.setQuery(s?.toString().orEmpty())
            }
        })
        binding.btnClearSearch.setOnClickListener { binding.searchEdit.setText("") }

        // 무한 스크롤
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lm = rv.layoutManager as LinearLayoutManager
                val last = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (total - last <= 5) vm.loadNext()
            }
        })

        // 기간 필터
        val periodItems = listOf(
            getString(R.string.period_none),
            getString(R.string.period_today),
            getString(R.string.period_7days),
            getString(R.string.period_this_month)
        )
        binding.periodSpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, periodItems))
        binding.periodSpinner.setText(periodItems[1], false) // 기본: 오늘
        binding.periodSpinner.setOnItemClickListener { _, _, position, _ -> vm.setPeriod(position) }

        // 상태 칩 (PENDING 포함)
        fun bindChip(id: Int, status: OrderStatus) {
            binding.root.findViewById<Chip>(id)
                .setOnCheckedChangeListener { _, checked -> vm.setStatusEnabled(status, checked) }
        }
        bindChip(R.id.chipPlaced, OrderStatus.PENDING)
        bindChip(R.id.chipApproved, OrderStatus.APPROVED)
        bindChip(R.id.chipRejected, OrderStatus.REJECTED)
        bindChip(R.id.chipShipped, OrderStatus.SHIPPED)
        bindChip(R.id.chipDelivered, OrderStatus.DELIVERED)

        // 새로고침
        binding.swipe.setOnRefreshListener { vm.refresh() }

        vm.uiState.observe(viewLifecycleOwner) { s ->
            binding.swipe.isRefreshing = s.loading
            binding.empty.isVisible = !s.loading && s.items.isEmpty()
            adapter.submitList(s.items)

            // 합계 푸터
            binding.footerCount.text = getString(R.string.order_count_fmt, s.items.size)
            val total = s.items.sumOf { it.totalAmount ?: 0L }
            binding.footerAmount.text = getString(R.string.order_amount_fmt, moneyFormat.format(total))

            // 로딩/끝/메모검색 안내
            binding.loadMore.isVisible = s.loadingMore
            binding.endBadge.isVisible = !s.loading && !s.loadingMore && s.endReached && s.items.isNotEmpty()
            binding.noteSearchBadge.isVisible = s.noteSearchActive
            if (s.noteSearchActive) {
                binding.noteSearchBadge.text = getString(R.string.note_search_hint_status_disabled)
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}