package com.songdosamgyeop.order

object Env {
    const val USE_EMULATORS = true

    // PortOne
    const val PORTONE_USER_CODE = "impXXXXXXXX" // ← 대시보드의 userCode로 교체
    const val APP_SCHEME = "songdo-pay"         // 딥링크 스킴 (아래 Manifest 매칭)

    // 결제/검증 플래그
    const val FUNCTIONS_ENABLED: Boolean = false // 배포 전: false, 배포 후: true
    // Cloud Functions
    const val FUNCTIONS_REGION: String = "asia-northeast3"

    // PortOne 테스트 상점/채널 (실운영시 Secret은 Functions에서만 사용)
    const val PORTONE_STORE_ID: String = "store-test-abc123"
    const val PORTONE_CHANNEL_KEY: String = "channel-test-xyz789"

    // 결제 UI 시뮬레이터 옵션 (FUNCTIONS_ENABLED=false일 때만 사용)
    const val PAYMENT_SIMULATE_SUCCESS: Boolean = true
}