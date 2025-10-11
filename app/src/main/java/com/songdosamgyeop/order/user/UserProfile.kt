package com.songdosamgyeop.order.user

data class UserProfile(
    val uid: String,
    val email: String,
    val role: String,        // "HQ" | "BRANCH"
    val branchId: String?,
    val branchName: String?,
    val branchTel: String?
)