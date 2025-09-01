package com.songdosamgyeop.order.ui

/** LiveData용 1회성 이벤트 래퍼 */
class Event<out T>(private val content: T) {
    private var handled = false
    fun getIfNotHandled(): T? =
        if (handled) null else { handled = true; content }
}