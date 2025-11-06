package org.wordpress.android.support.he.model

import android.net.Uri

data class AttachmentState(
    val acceptedUris: List<Uri> = emptyList(),
    val rejectedUris: List<Uri> = emptyList(),
    val currentTotalSizeBytes: Long = 0L,
    val rejectedTotalSizeBytes: Long = 0L
)
