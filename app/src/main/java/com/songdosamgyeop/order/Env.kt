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

    // ✅ KG이니시스 테스트 자격
    const val INICIS_MID = "INIpayTestMid"       // ← 테스트 MID로 교체
    const val INICIS_SIGN_KEY = "TEST_SIGN_KEY"  // ← 테스트 signKey로 교체

    // TODO 확인 해야됌
    // 서버 검증(Cloud Functions onRequest HTTP 엔드포인트; callable가 아니라 URL 호출형일 수도)
    // 여기서는 기존 구조를 재사용하기 위해 HTTPS Callable 함수명 사용
    const val FUNCTIONS_VERIFY_INICIS = "verifyInicisPayment"

    // 결제 UI 시뮬레이터 옵션 (FUNCTIONS_ENABLED=false일 때만 사용)
    const val PAYMENT_SIMULATE_SUCCESS: Boolean = true
}