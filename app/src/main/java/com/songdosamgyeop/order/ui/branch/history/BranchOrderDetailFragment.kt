// app/src/main/java/com/songdosamgyeop/order/ui/branch/history/BranchOrderDetailFragment.kt
package com.songdosamgyeop.order.ui.branch.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.data.model.OrderHeader
import com.songdosamgyeop.order.databinding.FragmentBranchOrderDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class BranchOrderDetailFragment : Fragment() {

    private var _b: FragmentBranchOrderDetailBinding? = null
    private val b get() = _b!!

    @Inject lateinit var db: FirebaseFirestore

    private val money = NumberFormat.getNumberInstance(Locale.KOREA)
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply {
        timeZone = TimeZone.getDefault()
    }

    private lateinit var adapter: BranchOrderLinesAdapter
    private var orderId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentBranchOrderDetailBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        orderId = requireArguments().getString("orderId")
        b.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = BranchOrderLinesAdapter()
        b.recycler.adapter = adapter

        load(orderId ?: return)
    }

    private fun load(orderId: String) {
        b.tvEmpty.isVisible = false
        b.recycler.isVisible = false

        db.collection("orders").document(orderId).get()
            .addOnSuccessListener { doc ->
                val header = doc.toObject(OrderHeader::class.java)
                b.tvTitle.text = getString(R.string.order_title_fmt, orderId)
                b.chipStatus.text = header?.status ?: "-"
                b.tvWhen.text = header?.placedAt?.toDate()?.let(sdf::format) ?: "-"
                b.tvSummary.text = getString(
                    R.string.order_summary_fmt,
                    money.format(header?.totalAmount ?: 0L),
                    header?.itemsCount ?: 0
                )

                // 타이틀 클릭 → ID 복사
                b.tvTitle.setOnClickListener {
                    val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("orderId", orderId))
                    com.google.android.material.snackbar.Snackbar.make(b.root, R.string.copied_order_id, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                }

                db.collection("orders").document(orderId).collection("items")
                    .get()
                    .addOnSuccessListener { snap ->
                        val items = snap.documents.map { d ->
                            OrderLineUI(
                                name = d.getString("name") ?: d.id,
                                unit = d.getString("unit") ?: "",
                                qty = (d.getLong("qty") ?: 0L).toInt(),
                                price = d.getLong("price") ?: 0L
                            )
                        }
                        adapter.submitList(items)
                        b.recycler.isVisible = items.isNotEmpty()
                        b.tvEmpty.isVisible = items.isEmpty()
                    }
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

data class OrderLineUI(
    val name: String,
    val unit: String,
    val qty: Int,
    val price: Long
)