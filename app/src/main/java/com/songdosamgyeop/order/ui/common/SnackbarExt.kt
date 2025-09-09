package com.songdosamgyeop.order.ui.common

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.showInfo(msg: String, long: Boolean = false) {
    Snackbar.make(this, msg, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show()
}

fun View.showError(t: Throwable, long: Boolean = true) {
    val msg = ErrorMapper.toUserMessage(t)
    Snackbar.make(this, msg, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show()
}