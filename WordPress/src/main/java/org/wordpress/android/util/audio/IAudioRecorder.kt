package org.wordpress.android.util.audio

import android.Manifest
import kotlinx.coroutines.flow.Flow

interface IAudioRecorder {
    fun startRecording(onRecordingFinished: (AudioRecorderResult) -> Unit)
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()
    fun recordingUpdates(): Flow<RecordingUpdate>
    fun endRecordingSession()

    sealed class AudioRecorderResult {
        data class Success(val recordingPath: String) : AudioRecorderResult()
        data class Error(val errorMessage: String) : AudioRecorderResult()
    }

    companion object {
        val REQUIRED_RECORDING_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }
}



