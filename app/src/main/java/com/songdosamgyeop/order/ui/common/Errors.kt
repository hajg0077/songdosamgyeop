package com.songdosamgyeop.order.ui.common

class Errors {
    fun Throwable.userMessage(): String {
        val m = message ?: return "처리에 실패했습니다."
        return when {
            m.contains("permission-denied", true) -> "권한이 없습니다(HQ 계정 확인)."
            m.contains("transition not allowed", true) -> "허용되지 않는 상태 전이입니다."
            m.contains("not-found", true) -> "대상 주문을 찾을 수 없습니다."
            else -> m
        }
    }
}