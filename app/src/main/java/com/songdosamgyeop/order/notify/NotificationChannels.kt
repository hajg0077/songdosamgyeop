package com.songdosamgyeop.order.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.songdosamgyeop.order.R

object NotificationChannels {
    const val Orders = "orders"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                Orders,
                context.getString(R.string.channel_orders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_orders_desc)
            }
            nm.createNotificationChannel(ch)
        }
    }
}