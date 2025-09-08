// data/remote/HqFunctionsDataSource.kt
package com.songdosamgyeop.order.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class HqFunctionsDataSource(
    private val fns: FirebaseFunctions,
    private val functionsEnabled: Boolean
) {
    private fun checkEnabled() {
        if (!functionsEnabled) error("Functions is disabled by Env")
    }

    suspend fun approveRegistration(docId: String): Result<Unit> = runCatching {
        checkEnabled()
        fns.getHttpsCallable("hqApproveRegistration")
            .call(mapOf("docId" to docId)).await()
        Unit
    }

    suspend fun rejectRegistration(docId: String, reason: String?): Result<Unit> = runCatching {
        checkEnabled()
        fns.getHttpsCallable("hqRejectRegistration")
            .call(mapOf("docId" to docId, "reason" to (reason ?: ""))).await()
        Unit
    }

    suspend fun resetRegistration(docId: String): Result<Unit> = runCatching {
        checkEnabled()
        fns.getHttpsCallable("hqResetRegistration")
            .call(mapOf("docId" to docId)).await()
        Unit
    }
}