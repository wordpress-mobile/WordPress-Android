package org.wordpress.android.util.audio

data class RecordingUpdate(
    val elapsedTime: Int = 0, // in seconds
    val fileSize: Long = 0L, // in bytes
    val fileSizeLimitExceeded: Boolean = false,
    val amplitudes: List<Float> = emptyList()
)
