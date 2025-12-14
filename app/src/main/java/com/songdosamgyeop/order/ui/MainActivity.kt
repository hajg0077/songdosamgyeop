package com.songdosamgyeop.order.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
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

    // ✅ 중복 호출/경합 방지용
    private var configureJob: Job? = null
    private var lastAppliedKey: String? = null // "uid:role"
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ 초기 1회만 호출
        configureForCurrentUser(reason = "onCreate")

        // ✅ 리스너는 1개만 등록
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

    /** 현재 사용자 상태에 맞게 네비 그래프/하단 메뉴 구성 (중복/경합 방지) */
    private fun configureForCurrentUser(reason: String) {
        // ✅ 이전 실행이 있으면 취소하고 최신 상태만 반영
        configureJob?.cancel()

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val nav = navHost.navController
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        val user = auth.currentUser
        if (user == null) {
            lastAppliedKey = null
            bottom.visibility = View.GONE
            nav.setGraph(R.navigation.nav_auth)
            return
        }

        configureJob = lifecycleScope.launch {
            // ✅ claims는 즉시 반영 안 될 수 있으니 일단 forceRefresh=true로 한번 갱신
            val role = withContext(Dispatchers.IO) { fetchRoleFromClaims(forceRefresh = true) }

            // role이 없으면 "승인 대기(PENDING)"로 보냄
            val resolvedRole = if (role.isBlank()) "PENDING" else role

            val key = "${user.uid}:$resolvedRole"
            if (key == lastAppliedKey) return@launch // ✅ 같은 상태면 재적용 안 함
            lastAppliedKey = key

            when (resolvedRole) {
                "HQ" -> {
                    bottom.visibility = View.VISIBLE
                    bottom.menu.clear()
                    bottom.inflateMenu(R.menu.menu_hq_bottom)
                    nav.setGraph(R.navigation.nav_hq)
                    bottom.setupWithNavController(nav)
                    bottom.setOnItemReselectedListener { nav.popBackStack(nav.graph.startDestinationId, false) }
                }
                "BRANCH" -> {
                    bottom.visibility = View.VISIBLE
                    bottom.menu.clear()
                    bottom.inflateMenu(R.menu.menu_branch_bottom)
                    nav.setGraph(R.navigation.nav_branch)
                    bottom.setupWithNavController(nav)
                    bottom.setOnItemReselectedListener { nav.popBackStack(nav.graph.startDestinationId, false) }
                }
                "PENDING" -> {
                    // ✅ 승인대기 화면(또는 그래프)로 보내기
                    bottom.visibility = View.GONE
                    nav.setGraph(R.navigation.nav_pending)
                }
                else -> {
                    // 알 수 없는 role이면 로그아웃 처리
                    auth.signOut()
                    bottom.visibility = View.GONE
                    nav.setGraph(R.navigation.nav_auth)
                }
            }
        }
    }

    /** Custom Claims에서 role 읽기 */
    private suspend fun fetchRoleFromClaims(forceRefresh: Boolean): String {
        val user = auth.currentUser ?: return ""
        val tokenResult = user.getIdToken(forceRefresh).await()
        return tokenResult.claims["role"] as? String ?: ""
    }
}