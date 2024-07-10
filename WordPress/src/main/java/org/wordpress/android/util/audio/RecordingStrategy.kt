package org.wordpress.android.util.audio

import javax.inject.Qualifier

@Suppress("MagicNumber")
sealed class RecordingStrategy {
    abstract val maxFileSize: Long
    abstract val maxDuration: Int
    abstract val storeInMemory: Boolean
    abstract val recordingFileName: String

    data class VoiceToContentRecordingStrategy(
        override val maxFileSize: Long = 1000000L * 25, // 25MB
        override val maxDuration: Int = 60 * 5, // 5 minutes
        override val recordingFileName: String = "voice_recording.mp4",
        override val storeInMemory: Boolean = true
    ) : RecordingStrategy()
}

// Declare here your custom annotation for each RecordingStrategy so it can be provided by Dagger
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VoiceToContentStrategy


