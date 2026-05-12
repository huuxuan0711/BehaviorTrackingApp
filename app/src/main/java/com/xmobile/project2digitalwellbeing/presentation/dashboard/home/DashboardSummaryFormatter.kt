package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import com.xmobile.project2digitalwellbeing.domain.usage.model.HourlyUsage

fun DashboardUiState.toInsightSummaryText(): String {
    return when {
        errorMessage != null ->
            errorMessage

        else ->
            insightSummaryText
    }
}

fun List<HourlyUsage>.toLateNightRatioText(): String {
    val total = sumOf { it.totalTimeMillis }
    if (total <= 0L) return "0%"

    // Để tránh "tính thừa" khi cửa sổ 24h giao thoa với 2 chu kỳ đêm (ví dụ: 4h sáng hôm qua và 1h sáng hôm nay),
    // chúng ta chỉ tính chu kỳ đêm gần nhất với thời điểm hiện tại.
    
    // Tìm index của giờ đêm cuối cùng trong danh sách (thường là gần hiện tại nhất)
    val lastNightIndex = findLastIndex { it.hourOfDay >= 22 || it.hourOfDay < 6 }
    if (lastNightIndex == -1) return "0%"

    // Quét ngược lại để lấy trọn vẹn một khối đêm liên tục (ví dụ: từ 3h sáng ngược về 22h đêm qua)
    var nightUsage = 0L
    var i = lastNightIndex
    while (i >= 0) {
        val hour = this[i].hourOfDay
        if (hour >= 22 || hour < 6) {
            nightUsage += this[i].totalTimeMillis
            i--
        } else {
            // Đã thoát khỏi khối đêm này
            break
        }
    }

    return "${((nightUsage.toDouble() / total.toDouble()) * 100).toInt()}%"
}

private inline fun <T> List<T>.findLastIndex(predicate: (T) -> Boolean): Int {
    for (i in indices.reversed()) {
        if (predicate(this[i])) return i
    }
    return -1
}
