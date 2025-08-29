package com.songdosamgyeop.order.data.model

data class Registration (
    val email: String = "",
    val name: String = "",
    val branchName: String = "",
    val branchCode: String = "",
    val phone: String = "",
    val memo: String = "",
    val status: String = "PENDING",          // PENDING / APPROVED / REJECTED
    val createdAt: Long = System.currentTimeMillis()
)