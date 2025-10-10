package com.songdosamgyeop.order.ui.branch.checkout

import android.content.Context
import android.content.Intent
import com.songdosamgyeop.order.payment.InicisPaymentActivity

object CheckoutNavigator {
    fun goPayment(context: Context, orderId: String, title: String, amount: Long, buyerName: String?, buyerEmail: String?, buyerTel: String) {
        context.startActivity(
            Intent(context, InicisPaymentActivity::class.java).apply {
                putExtra("orderId", orderId)
                putExtra("title", title)
                putExtra("amount", amount)
                putExtra("buyerName", buyerName)
                putExtra("buyerEmail", buyerEmail)
                putExtra("buyerTel", buyerTel)
            }
        )
    }
}