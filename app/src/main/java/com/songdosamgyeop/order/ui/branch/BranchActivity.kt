package com.songdosamgyeop.order.ui.branch

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.color.DynamicColors
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.databinding.ActivityBranchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BranchActivity : AppCompatActivity() {

    private lateinit var b: ActivityBranchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        b = ActivityBranchBinding.inflate(layoutInflater)
        setContentView(b.root)

        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        val nav = host.navController
        NavigationUI.setupWithNavController(b.bottomNav, nav)

        // 하단탭에서 같은 탭 재선택 시 최상위로 pop
        b.bottomNav.setOnItemReselectedListener { item ->
            val destId = when (item.itemId) {
                R.id.branchProductsFragment -> R.id.branchProductsFragment
                R.id.branchCartFragment -> R.id.branchCartFragment
                R.id.branchOrderHistoryFragment -> R.id.branchOrderHistoryFragment
                else -> null
            }
            destId?.let { nav.popBackStack(it, false) }
        }

        // TODO: 실제 로그인 사용자 정보/지점정보에서 branchId를 가져오세요.
        val branchId = /* user.branchId */ "BR001"

        TokenUploader.refreshAndUpload()
        TopicSubscriber.subscribeBranch(branchId)
    }
}
