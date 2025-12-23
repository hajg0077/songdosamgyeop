package com.songdosamgyeop.order.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.ui.branch.BranchActivity
import com.songdosamgyeop.order.ui.hq.HqActivity
import com.songdosamgyeop.order.ui.login.PendingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 앱의 유일한 Activity: 로그인/역할(HQ/BRANCH/PENDING)에 따라 그래프/메뉴 로드 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private var configureJob: Job? = null
    private var lastAppliedKey: String? = null // "uid:role"
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 초기 1회
        configureForCurrentUser(reason = "onCreate")

        // AuthStateListener 1개만 유지
        authListener = FirebaseAuth.AuthStateListener {
            configureForCurrentUser(reason = "authStateChanged")
        }.also { auth.addAuthStateListener(it) }
    }

    override fun onDestroy() {
        authListener?.let { auth.removeAuthStateListener(it) }
        authListener = null
        configureJob?.cancel()
        super.onDestroy()
    }

    private fun navController(): NavController {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        return navHost.navController
    }

    /** 현재 사용자 상태에 맞게 네비 그래프/하단 메뉴 구성 (중복/경합 방지) */
    private fun configureForCurrentUser(reason: String) {
        configureJob?.cancel()

        val nav = navController()
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        val user = auth.currentUser
        if (user == null) {
            lastAppliedKey = null
            bottom.visibility = View.GONE
            nav.setGraph(R.id.loginFragment)
            return
        }

        configureJob = lifecycleScope.launch {
            // ✅ 로딩 느린 주범: forceRefresh=true는 매번 네트워크 탐.
            // 1) 먼저 캐시 토큰으로 빠르게 role 시도
            var role = withContext(Dispatchers.IO) { fetchRoleFromClaims(forceRefresh = false) }

            // 2) role이 비었을 때만 1회 강제 갱신
            if (role.isBlank()) {
                role = withContext(Dispatchers.IO) { fetchRoleFromClaims(forceRefresh = true) }
            }

            val resolvedRole = if (role.isBlank()) "PENDING" else role
            val key = "${user.uid}:$resolvedRole"
            if (key == lastAppliedKey) return@launch
            lastAppliedKey = key

            when (resolvedRole) {
                "HQ" -> {
                    startActivity(
                        Intent(this@MainActivity, HqActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }

                "BRANCH" -> {
                    startActivity(
                        Intent(this@MainActivity, BranchActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }

                "PENDING" -> {
                    startActivity(
                        Intent(this@MainActivity, PendingActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }

                else -> {
                    auth.signOut()
                    bottom.visibility = View.GONE
                    nav.setGraph(R.id.loginFragment)
                }
            }
        }
    }

    /**
     * ✅ 핵심:
     * - setGraph() 먼저
     * - 그 다음 menu inflate
     * - 그 다음 setupWithNavController()
     * 순서가 틀리면 "클릭 안 먹음" / "선택 상태 안 바뀜" 같은 증상이 나옴
     */
    private fun applyBottomNav(
        bottom: BottomNavigationView,
        nav: NavController,
        menuRes: Int,
        graphRes: Int
    ) {
        bottom.visibility = View.VISIBLE

        // 1) 그래프 먼저 교체
        nav.setGraph(graphRes)

        // 2) 메뉴 갈아끼우기
        bottom.menu.clear()
        bottom.inflateMenu(menuRes)

        // 3) 연결
        bottom.setupWithNavController(nav)

        // 4) 같은 탭 다시 누르면 루트로
        bottom.setOnItemReselectedListener {
            nav.popBackStack(nav.graph.startDestinationId, false)
        }
    }

    /** Custom Claims에서 role 읽기 */
    private suspend fun fetchRoleFromClaims(forceRefresh: Boolean): String {
        val user = auth.currentUser ?: return ""
        val tokenResult = user.getIdToken(forceRefresh).await()
        return tokenResult.claims["role"] as? String ?: ""
    }
}