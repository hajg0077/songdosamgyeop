package com.songdosamgyeop.order.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PendingActivity : AppCompatActivity(R.layout.activity_pending) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private var pollJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 로그인 자체가 없으면 로그인 화면으로 보내고 종료
        if (auth.currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
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
}