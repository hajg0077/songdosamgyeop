package com.songdosamgyeop.order.ui.hq.registrationlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.songdosamgyeop.order.data.model.Registration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistrationAdapter(
    private val onClick: (id: String, reg: Registration) -> Unit
) : ListAdapter<Pair<String, Registration>, RegistrationAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Pair<String, Registration>>() {
        override fun areItemsTheSame(o: Pair<String, Registration>, n: Pair<String, Registration>) = o.first == n.first
        override fun areContentsTheSame(o: Pair<String, Registration>, n: Pair<String, Registration>) = o == n
    }

    inner class VH(val b: ItemRegistrationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return VH(ItemRegistrationBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (id, reg) = getItem(position)
        val b = holder.b

        // 상단: 지점명(코드)
        b.tvBranch.text = "${reg.branchName} (${reg.branchCode})"

        // 상태칩
        b.chipStatus.text = reg.status // "PENDING"/"APPROVED"/"REJECTED"
        when (reg.status) {
            "PENDING" -> { /* 기본 색상 유지 or 강조 색 적용을 원하면 여기서 */ }
            "APPROVED" -> { /* b.chipStatus.setChipBackgroundColorResource(R.color.xxx) */ }
            "REJECTED" -> { /* ... */ }
        }

        // 담당자 이름 · 이메일
        b.tvNameEmail.text = "${reg.name} · ${reg.email}"

        // 생성일 표기 (yyyy-MM-dd HH:mm)
        val date = Date(reg.createdAt)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        b.tvCreatedAt.text = fmt.format(date)

        // 클릭
        b.card.setOnClickListener { onClick(id, reg) }
    }
}