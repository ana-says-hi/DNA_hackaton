package com.example.dna_mock

import android.graphics.Color

enum class NotificationRisk {
    SAFE,
    SUSPICIOUS,
    UNKNOWN
}

data class NotificationPreview(
    val key: String,
    val appName: String,
    val title: String,
    val content: String,
    val postedAt: Long,
    val risk: NotificationRisk = NotificationRisk.UNKNOWN
)