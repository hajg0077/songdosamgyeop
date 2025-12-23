package com.songdosamgyeop.order.ui.branch

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.ActivityBranchBinding
import com.songdosamgyeop.order.notify.TokenUploader
import com.songdosamgyeop.order.notify.TopicSubscriber
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class BranchActivity : AppCompatActivity() {

    private lateinit var b: ActivityBranchBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        b = ActivityBranchBinding.inflate(layoutInflater)
        setContentView(b.root)

        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val nav = host.navController

        // ✅ nav_branch.xml의 destination id == menu item id 로 통일되어 있어야 함
        nav.setGraph(R.navigation.nav_branch)

        // ✅ 그래프 세팅 후 연결
        b.bottomNav.setupWithNavController(nav)

        // 같은 탭 재선택 시 해당 탭 루트로 pop
        b.bottomNav.setOnItemReselectedListener { item ->
            nav.popBackStack(item.itemId, false)
        }

        // --- 알림 토큰/토픽 구독 (branchId 실제값으로) ---
        scope.launch {
            val uid = auth.currentUser?.uid ?: return@launch

            // ✅ 예시: users/{uid}.branchId 로 저장되어 있다고 가정
            val branchId = runCatching {
                val snap = db.collection("users").document(uid).get().await()
                snap.getString("branchId") ?: ""
            }.getOrDefault("")

            TokenUploader.refreshAndUpload()

            if (branchId.isNotBlank()) {
                TopicSubscriber.subscribeBranch(branchId)
            }
        }
    }
}