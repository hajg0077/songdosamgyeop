package com.songdosamgyeop.order.ui.branch.checkout

import android.content.Context
import android.content.Intent
import com.songdosamgyeop.order.ui.payment.PortOnePaymentActivity

object CheckoutNavigator {
    fun goPayment(
        context: Context,
        orderId: String,
        title: String,
        amount: Long,
        buyerName: String?,
        buyerEmail: String?,
        buyerTel: String?,
        payMethod: String = "card" // "card" | "trans" | "vbank"
    ) {
        context.startActivity(
            Intent(context, PortOnePaymentActivity::class.java).apply {
                putExtra("orderId", orderId)
                putExtra("title", title)
                putExtra("amount", amount)
                putExtra("buyerName", buyerName)
                putExtra("buyerEmail", buyerEmail)
                putExtra("buyerTel", buyerTel)
                putExtra("payMethod", payMethod)
            }
        )
    }
}
