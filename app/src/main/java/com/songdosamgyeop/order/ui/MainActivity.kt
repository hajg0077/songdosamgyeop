package com.songdosamgyeop.order.ui

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.NavigationRes
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.songdosamgyeop.order.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 앱의 유일한 Activity: 로그인/역할(HQ/BRANCH)에 따라 적절한 그래프/메뉴를 로드한다. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 최초 구성
        configureForCurrentUser()

        // 로그인/로그아웃 감지 시 재구성
        auth.addAuthStateListener { configureForCurrentUser() }
    }

    /** 현재 사용자 상태를 읽어서 적절한 네비게이션 그래프/메뉴를 적용한다. */
    private fun configureForCurrentUser() {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val nav = navHost.navController
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        val user = auth.currentUser
        if (user == null) {
            // 미로그인 → 로그인 그래프
            bottom.visibility = View.GONE
            nav.setGraph(R.navigation.nav_auth)
            return
        }

        // 로그인됨 → Firestore에서 role 조회 후 분기
        lifecycleScope.launch {
            val role = withContext(Dispatchers.IO) { fetchRole(user.uid) }
            when (role) {
                "HQ" -> {
                    bottom.visibility = View.VISIBLE
                    bottom.menu.clear()
                    bottom.inflateMenu(R.menu.menu_hq_bottom)
                    nav.setGraph(R.navigation.nav_hq)
                    bottom.setupWithNavController(nav) // 메뉴-그래프 연결
                }
                "BRANCH" -> {
                    bottom.visibility = View.VISIBLE
                    bottom.menu.clear()
                    bottom.inflateMenu(R.menu.menu_branch_bottom)
                    nav.setGraph(R.navigation.nav_branch)
                    bottom.setupWithNavController(nav)
                }
                else -> {
                    // 알 수 없는 역할 → 로그인으로 회귀(정합성 깨짐 방지)
                    auth.signOut()
                    bottom.visibility = View.GONE
                    nav.setGraph(R.navigation.nav_auth)
                }
            }
        }
    }

    /** users/{uid}.role 을 읽어 HQ/BRANCH 를 반환한다. (없으면 "") */
    private suspend fun fetchRole(uid: String): String {
        val snap = db.collection("users").document(uid).get().await()
        return snap.getString("role") ?: ""
    }
}