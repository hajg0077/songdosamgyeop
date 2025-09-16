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
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 앱의 유일한 Activity: 로그인/역할(HQ/BRANCH)에 따라 그래프/메뉴 로드 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+: Splash API 적용 (pre-12는 테마만)
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configureForCurrentUser()
        auth.addAuthStateListener { configureForCurrentUser() }
    }

    /** 현재 사용자 상태에 맞게 네비 그래프/하단 메뉴 구성 */
    private fun configureForCurrentUser() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val nav = navHost.navController
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        val user = auth.currentUser
        if (user == null) {
            bottom.visibility = View.GONE
            nav.setGraph(R.navigation.nav_auth)
            return
        }

        lifecycleScope.launch {
            val role = withContext(Dispatchers.IO) { fetchRole(user.uid) }
            when (role) {
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
                else -> {
                    auth.signOut()
                    bottom.visibility = View.GONE
                    nav.setGraph(R.navigation.nav_auth)
                }
            }
        }
    }

    /** users/{uid}.role → "HQ"/"BRANCH" (없으면 "") */
    private suspend fun fetchRole(uid: String): String {
        val snap = db.collection("users").document(uid).get().await()
        return snap.getString("role") ?: ""
    }
}