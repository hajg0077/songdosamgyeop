package com.songdosamgyeop.order.ui.hq

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.notify.TokenUploader
import com.songdosamgyeop.order.notify.TopicSubscriber
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@AndroidEntryPoint
class HqActivity : AppCompatActivity(R.layout.activity_hq) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ HQ는 시스템이 자동으로 바/제스처 영역 피하게(가장 안전)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val navHost = supportFragmentManager.findFragmentById(R.id.hq_nav_host) as NavHostFragment
        val navController = navHost.navController

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottom.setupWithNavController(navController)

        bottom.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottom.isVisible = destination.id in setOf(
                R.id.menu_hq_home,
                R.id.menu_registrations,
                R.id.menu_monitoring,
                R.id.menu_settings
            )
        }

        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val tokenResult = runCatching { user.getIdToken(true).await() }.getOrNull()
            android.util.Log.d("AUTH", "HQ token role=${tokenResult?.claims?.get("role")} claims=${tokenResult?.claims}")
        }

        TokenUploader.refreshAndUpload()
        TopicSubscriber.subscribeHq()
    }
}