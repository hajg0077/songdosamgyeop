package com.songdosamgyeop.order.ui.branch.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.core.model.OrderStatus
import com.songdosamgyeop.order.data.model.OrderHeader
import com.songdosamgyeop.order.databinding.ItemBranchHistoryOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BranchHistoryAdapter(
    private val onClick: (OrderHeader) -> Unit,
    private val onCopyId: (String) -> Unit
) : ListAdapter<OrderHeader, BranchHistoryVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchHistoryVH {
        val binding = ItemBranchHistoryOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BranchHistoryVH(binding, onClick, onCopyId)
    }

    override fun onBindViewHolder(holder: BranchHistoryVH, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderHeader>() {
            override fun areItemsTheSame(oldItem: OrderHeader, newItem: OrderHeader) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: OrderHeader, newItem: OrderHeader) = oldItem == newItem
        }
    }
}

class BranchHistoryVH(
    private val binding: ItemBranchHistoryOrderBinding,
    private val onClick: (OrderHeader) -> Unit,
    private val onCopyId: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val money = NumberFormat.getNumberInstance(Locale.KOREA)
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply {
        timeZone = TimeZone.getDefault()
    }

    fun bind(item: OrderHeader) = with(binding) {
        val whenStr = item.placedAt?.toDate()?.let(sdf::format) ?: "-"
        txtBranch.text = item.branchName ?: item.branchId ?: "-"
        txtDate.text = whenStr
        txtAmount.text = money.format(item.totalAmount ?: 0L)
        txtCount.text = item.itemsCount?.toString() ?: "-"

        // 상태 배지 간단 스타일링
        badgeStatus.text = item.status ?: OrderStatus.PLACED.name
        badgeStatus.setBackgroundResource(
            when (item.status?.let { OrderStatus.valueOf(it) } ?: OrderStatus.PLACED) {
                OrderStatus.PLACED -> com.google.android.material.R.drawable.mtrl_chip_background
                OrderStatus.APPROVED -> com.google.android.material.R.drawable.mtrl_chip_background
                OrderStatus.REJECTED -> com.google.android.material.R.drawable.mtrl_chip_background
                OrderStatus.SHIPPED -> com.google.android.material.R.drawable.mtrl_chip_background
                OrderStatus.DELIVERED -> com.google.android.material.R.drawable.mtrl_chip_background
            }
        )

        root.setOnClickListener { onClick(item) }
        btnCopy.setOnClickListener { item.id?.let(onCopyId) }
    }
}