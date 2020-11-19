package org.wordpress.android.fluxc.model.activity

import java.util.Date

data class BackupDownloadStatusModel(
    val downloadId: Long,
    val rewindId: String,
    val backupPoint: Date,
    val startedAt: Date,
    val progress: Int?,
    val downloadCount: Int?,
    val validUntil: Date?,
    val url: String?
)
