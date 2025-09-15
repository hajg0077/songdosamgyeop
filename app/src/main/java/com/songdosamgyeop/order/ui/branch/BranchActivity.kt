package com.songdosamgyeop.order.ui.branch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.songdosamgyeop.order.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BranchActivity : AppCompatActivity(R.layout.activity_branch) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController(R.id.branch_nav_host)
        findViewById<BottomNavigationView>(R.id.bottomNav)
            .setupWithNavController(navController)
    }
}