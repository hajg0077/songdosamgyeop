package com.songdosamgyeop.order.ui.branch.orders

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Timestamp
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.FragmentBranchOrdersBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.TimeZone

/** 브랜치 주문 히스토리 리스트 화면 */
@AndroidEntryPoint
class BranchOrdersFragment : Fragment(R.layout.fragment_branch_orders) {

    private val vm: BranchOrdersViewModel by viewModels()
    private lateinit var adapter: BranchOrdersAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentBranchOrdersBinding.bind(view)
        adapter = BranchOrdersAdapter { orderId ->
            findNavController().navigate(
                R.id.action_branchOrders_to_branchOrderDetail,
                bundleOf("orderId" to orderId)
            )
        }
        b.recycler.adapter = adapter

        // 기간 선택
        b.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("조회 기간 선택")
                .build()
            picker.addOnPositiveButtonClickListener { range ->
                val start = range.first ?: return@addOnPositiveButtonClickListener
                val end   = range.second ?: return@addOnPositiveButtonClickListener
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = end
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                vm.setDateRange(
                    Timestamp(start / 1000, ((start % 1000) * 1_000_000).toInt()),
                    Timestamp(cal.timeInMillis / 1000, 0)
                )
            }
            picker.show(parentFragmentManager, "dateRange")
        }
        b.btnClearDate.setOnClickListener { vm.setDateRange(null, null) }

        // 구독
        vm.list.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            b.tvEmpty.visibility = if (rows.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
}