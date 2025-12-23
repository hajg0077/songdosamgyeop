package com.songdosamgyeop.order.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.BuildConfig
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PendingActivity : AppCompatActivity(R.layout.activity_pending) {
    private val functions by lazy { FirebaseFunctions.getInstance("asia-northeast3") }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var pollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 로그인 자체가 없으면 로그인 화면으로 보내고 종료
        if (auth.currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        //setupDebugBootstrapUi()
    }

    override fun onResume() {
        super.onResume()
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                val user = auth.currentUser ?: break

                // ✅ claims 반영 지연 대응: 강제 토큰 갱신
                val role = runCatching {
                    val token = user.getIdToken(true).await()
                    token.claims["role"] as? String ?: ""
                }.getOrDefault("")

                if (BuildConfig.DEBUG) {
                    Toast.makeText(this@PendingActivity, "role=$role", Toast.LENGTH_SHORT).show()
                }

                if (role == "BRANCH" || role == "HQ") {
                    // ✅ 권한 생겼으면 메인으로 복귀 (MainActivity가 그래프 분기)
                    startActivity(
                        Intent(this@PendingActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    finish()
                    return@launch
                }

                delay(2000)
            }
        }
    }

    override fun onPause() {
        pollJob?.cancel()
        pollJob = null
        super.onPause()
    }

//    private fun setupDebugBootstrapUi() {
//        val btnBootstrap = findViewById<Button>(R.id.btnDebugBootstrap)
//
//        // Release에서는 숨김
//        if (!BuildConfig.DEBUG) {
//            return
//        }
//
//        btnBootstrap.setOnClickListener {
//            val uid = ""
//            val secret = ""
//
//            lifecycleScope.launch {
//                runCatching {
//                    // 1) callable 호출
//                    val data = hashMapOf("uid" to uid, "secret" to secret)
//                    functions
//                        .getHttpsCallable("bootstrapHqAdmin")
//                        .call(data)
//                        .await()
//
//                    // 2) 토큰 강제 갱신 (claims 반영)
//                    auth.currentUser?.getIdToken(true)?.await()
//                }.onSuccess {
//                    Toast.makeText(this@PendingActivity, "HQ 부트스트랩 성공! 토큰 갱신 완료", Toast.LENGTH_LONG).show()
//
//                    // 3) 메인으로 복귀 → MainActivity가 role=HQ로 분기
//                    startActivity(
//                        Intent(this@PendingActivity, MainActivity::class.java)
//                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                    )
//                    finish()
//                }.onFailure { e ->
//                    android.util.Log.e("BOOTSTRAP", "bootstrap failed", e)
//
//                    val msg = when (e) {
//                        is com.google.firebase.functions.FirebaseFunctionsException ->
//                            "부트스트랩 실패: ${e.code} / ${e.message}\n${e.details ?: ""}"
//                        else ->
//                            "부트스트랩 실패: ${e::class.java.simpleName} / ${e.message}"
//                    }
//
//                    Toast.makeText(this@PendingActivity, msg, Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
}