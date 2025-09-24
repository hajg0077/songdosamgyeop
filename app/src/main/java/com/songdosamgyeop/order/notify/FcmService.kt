package com.songdosamgyeop.order.notify

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.songdosamgyeop.order.ui.MainActivity

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FcmService", "onNewToken=$token")
        TokenUploader.upload(token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val data = msg.data
        val title = msg.notification?.title ?: data["title"] ?: "알림"
        val body = msg.notification?.body ?: data["body"] ?: "새 소식이 있습니다."

        val type = data["type"] // e.g., ORDER_PAID / ORDER_STATUS
        val orderId = data["orderId"]

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("deep_link_type", type)
            putExtra("orderId", orderId)
        }
        val pi = PendingIntent.getActivity(
            this, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper.notify(
            context = this,
            channel = NotificationChannels.Orders,
            title = title,
            message = body,
            pendingIntent = pi
        )
    }
}