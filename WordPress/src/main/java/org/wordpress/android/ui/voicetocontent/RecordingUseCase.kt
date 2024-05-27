package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.util.audio.IAudioRecorder
import org.wordpress.android.util.audio.RecordingUpdate
import java.io.File
import javax.inject.Inject

class RecordingUseCase @Inject constructor(
    private val audioRecorder: IAudioRecorder
) {
    fun startRecording() {
        audioRecorder.startRecording()
    }

    @Suppress("ReturnCount")
    suspend fun stopRecording(): File? {
        val recordingPath = audioRecorder.stopRecording()
        // Return null if the recording path is invalid
        if (recordingPath.isNullOrEmpty()) return null
        val recordingFile = File(recordingPath)
        // Return null if the file does not exist, is not a file, or is empty
        if (!recordingFile.exists() || !recordingFile.isFile || recordingFile.length() == 0L) return null
        return recordingFile
    }

    fun pauseRecording() {
        audioRecorder.pauseRecording()
    }

    fun resumeRecording() {
        audioRecorder.resumeRecording()
    }

    fun recordingUpdates(): Flow<RecordingUpdate> {
        return audioRecorder.recordingUpdates()
    }
}

