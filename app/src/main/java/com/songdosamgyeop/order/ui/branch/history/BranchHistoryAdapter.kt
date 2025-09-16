package com.songdosamgyeop.order.ui.branch.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.data.model.OrderHeader
import com.songdosamgyeop.order.databinding.ItemBranchHistoryOrderBinding
import com.songdosamgyeop.order.ui.common.applyOrderStatusChip
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
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply { timeZone = TimeZone.getDefault() }

    fun bind(item: OrderHeader) = with(binding) {
        txtBranch.text = item.branchName ?: item.branchId ?: "-"
        txtDate.text = item.placedAt?.toDate()?.let(sdf::format) ?: "-"
        txtAmount.text = money.format(item.totalAmount ?: 0L)
        txtCount.text = item.itemsCount?.toString() ?: "-"
        applyOrderStatusChip(badgeStatus, item.status)

        root.setOnClickListener { onClick(item) }
        btnCopy.setOnClickListener { item.id?.let(onCopyId) }
    }
}