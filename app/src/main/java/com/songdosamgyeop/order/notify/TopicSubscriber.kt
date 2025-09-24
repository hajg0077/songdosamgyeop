package com.songdosamgyeop.order.notify

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object TopicSubscriber {

    fun subscribeHq() {
        FirebaseMessaging.getInstance().subscribeToTopic("hq")
            .addOnSuccessListener { Log.d("TopicSubscriber", "subscribed: hq") }
    }

    fun subscribeBranch(branchId: String) {
        val topic = "branch_${branchId}"
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnSuccessListener { Log.d("TopicSubscriber", "subscribed: $topic") }
    }

    fun unsubscribeBranch(branchId: String) {
        val topic = "branch_${branchId}"
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnSuccessListener { Log.d("TopicSubscriber", "unsubscribed: $topic") }
    }
}