package com.songdosamgyeop.order.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.songdosamgyeop.order.R

object NotificationHelper {

    fun notify(
        context: Context,
        channel: String,
        title: String,
        message: String,
        pendingIntent: PendingIntent? = null,
        id: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    ) {
        NotificationChannels.ensure(context)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notify) // vector 제공
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}