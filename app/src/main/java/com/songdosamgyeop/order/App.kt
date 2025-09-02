package com.songdosamgyeop.order

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 시스템 다크 모드 신호를 무시하고 항상 라이트
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        // 만약 전에 DynamicColors.applyToActivitiesIfAvailable(this) 넣었다면 주석 처리(브랜드 컬러 고정 목적)
    }
}