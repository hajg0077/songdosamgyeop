// data/model/Branch.kt
package com.songdosamgyeop.order.data.model

import com.google.firebase.Timestamp

data class Branch(
    val id: String = "",         // docId = branchCode
    val name: String = "",
    val active: Boolean = true,
    val contactName: String? = null,
    val phone: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)