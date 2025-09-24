package com.songdosamgyeop.order.ui.hq

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.songdosamgyeop.order.R
import com.songdosamgyeop.order.notify.TokenUploader
import com.songdosamgyeop.order.notify.TopicSubscriber
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HqActivity : AppCompatActivity(R.layout.activity_hq) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navController = findNavController(R.id.hq_nav_host)
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 하단 네비와 NavController 연결
        bottom.setupWithNavController(navController)

        // (선택) 탭 재선택 시 현재 리스트 상단으로 스크롤 같은 UX를 원하면 여기에 구현
        bottom.setOnItemReselectedListener { /* no-op for now */ }

        TokenUploader.refreshAndUpload()
        TopicSubscriber.subscribeHq()
    }
}