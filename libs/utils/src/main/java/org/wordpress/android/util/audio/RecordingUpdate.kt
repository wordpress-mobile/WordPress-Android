package org.wordpress.android.util.audio

data class RecordingUpdate(
    val remainingTimeInSeconds: Int = -1,
    val fileSize: Long = 0L, // in bytes
    val fileSizeLimitExceeded: Boolean = false,
    val amplitudes: List<Float> = emptyList()
)
