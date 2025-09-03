package com.songdosamgyeop.order.ui.hq.registrationlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.songdosamgyeop.order.data.repo.Registration
import com.songdosamgyeop.order.databinding.ItemRegistrationBinding

class RegistrationAdapter(
    private val onClick: (docId: String, reg: Registration) -> Unit
) : ListAdapter<Pair<String, Registration>, RegistrationAdapter.VH>(DIFF) {

    class VH(
        val binding: ItemRegistrationBinding,
        private val onClickAt: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.setOnClickListener { onClickAt(adapterPosition) } }

        fun bind(item: Pair<String, Registration>) {
            val (_, reg) = item
            binding.tvBranchName.text = reg.branchName
            binding.tvApplicant.text = "${reg.name} · ${reg.email}"
            val opt = listOfNotNull(reg.branchCode, reg.phone).joinToString(" · ")
            if (opt.isNotBlank()) {
                binding.tvOptional.visibility = View.VISIBLE
                binding.tvOptional.text = opt
            } else {
                binding.tvOptional.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRegistrationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding) { pos ->
            if (pos != RecyclerView.NO_POSITION) {
                val (id, reg) = getItem(pos)
                onClick(id, reg)
            }
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<String, Registration>>() {
            override fun areItemsTheSame(a: Pair<String, Registration>, b: Pair<String, Registration>) =
                a.first == b.first
            override fun areContentsTheSame(a: Pair<String, Registration>, b: Pair<String, Registration>) =
                a == b
        }
    }
}
