package org.wordpress.android.util.audio

data class RecordingParams(
    val maxDuration: Int, // seconds
    val maxFileSize: Long, // bytes
)
