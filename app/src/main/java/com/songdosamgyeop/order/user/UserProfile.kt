package com.songdosamgyeop.order.user

data class UserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val role: String,        // "HQ" | "BRANCH"
    val branchId: String?,
    val branchName: String?
)