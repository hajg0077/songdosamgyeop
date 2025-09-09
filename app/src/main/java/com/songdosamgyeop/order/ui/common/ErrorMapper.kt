package com.songdosamgyeop.order.ui.common

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctionsException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {
    fun toUserMessage(t: Throwable): String {
        // Firebase Functions 에러 코드 맵핑
        if (t is FirebaseFunctionsException) {
            return when (t.code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                    "권한이 없습니다. (HQ 전용 기능)"
                FirebaseFunctionsException.Code.NOT_FOUND ->
                    "대상을 찾을 수 없습니다."
                FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    "요청을 처리할 수 없는 상태입니다. (이미 처리되었을 수 있어요)"
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요."
                else -> t.message ?: "처리에 실패했습니다."
            }
        }

        // 네트워크 계열
        if (t is FirebaseNetworkException ||
            t is UnknownHostException ||
            t is SocketTimeoutException
        ) {
            return "네트워크 상태가 불안정합니다. 연결을 확인해 주세요."
        }

        // 그 외
        return t.message ?: "알 수 없는 오류가 발생했습니다."
    }
}