package com.songdosamgyeop.order

object Env {
    const val FUNCTIONS_ENABLED = false  // 배포 전: false, 배포 후: true
    const val USE_EMULATORS = true
    const val FUNCTIONS_REGION = "asia-northeast3"

    // PortOne
    const val PORTONE_USER_CODE = "impXXXXXXXX" // ← 대시보드의 userCode로 교체
    const val APP_SCHEME = "songdo-pay"         // 딥링크 스킴 (아래 Manifest 매칭)
}