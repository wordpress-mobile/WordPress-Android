package org.wordpress.android.util.audio

import android.Manifest
import kotlinx.coroutines.flow.Flow

interface IAudioRecorder {
    fun startRecording()
    fun stopRecording(): String
    fun pauseRecording()
    fun resumeRecording()
    fun recordingUpdates(): Flow<RecordingUpdate>

    companion object {
        val REQUIRED_RECORDING_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            // todo pantelis: do we need this?
            // Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

