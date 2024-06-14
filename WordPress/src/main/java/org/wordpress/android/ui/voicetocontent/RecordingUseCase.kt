package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.flow.Flow
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
}
