package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.util.audio.IAudioRecorder
import org.wordpress.android.util.audio.RecordingUpdate
import org.wordpress.android.util.audio.VoiceToContentStrategy
import javax.inject.Inject
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult

class RecordingUseCase @Inject constructor(
    @VoiceToContentStrategy private val audioRecorder: IAudioRecorder
) {
    fun startRecording(onRecordingFinished: (AudioRecorderResult) -> Unit) {
        audioRecorder.startRecording(onRecordingFinished)
    }

    fun stopRecording() {
       audioRecorder.stopRecording()
    }

    fun recordingUpdates(): Flow<RecordingUpdate> {
        return audioRecorder.recordingUpdates()
    }

    fun endRecordingSession() {
        audioRecorder.endRecordingSession()
    }

    fun isRecording(): StateFlow<Boolean> = audioRecorder.isRecording()
    fun isPaused(): StateFlow<Boolean> = audioRecorder.isPaused()


    fun pauseRecording() {
        audioRecorder.pauseRecording()
    }

    fun resumeRecording() {
        audioRecorder.resumeRecording()
    }
}
