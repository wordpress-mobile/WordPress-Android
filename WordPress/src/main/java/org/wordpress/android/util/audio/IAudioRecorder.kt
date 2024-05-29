package org.wordpress.android.util.audio

import android.Manifest
import kotlinx.coroutines.flow.Flow

interface IAudioRecorder {
    fun startRecording(onRecordingFinished: (String) -> Unit)
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()
    fun recordingUpdates(): Flow<RecordingUpdate>
    fun setRecordingParams(params: RecordingParams)

    companion object {
        val REQUIRED_RECORDING_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }
}

