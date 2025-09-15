package com.songdosamgyeop.order.ui.common
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

val moneyKo: NumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
val dateTimeKo: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}