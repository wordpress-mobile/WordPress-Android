package org.wordpress.android.ui.audiorecorder

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class AudioRecorderViewModel @Inject constructor(
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val contextProvider: ContextProvider,
    private val jetpackAIStore: JetpackAIStore) : ScopedViewModel(mainDispatcher) {
        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording

        private val _elapsedRecordingTime = MutableStateFlow(0L)
        val elapsedRecordingTime: StateFlow<Long> = _elapsedRecordingTime

        private var mediaRecorder: MediaRecorder? = null
        private var recordingJob: Job? = null
        private lateinit var audioFile: File
        private var siteModel: SiteModel? = null

        fun setSite(siteModel: SiteModel) {
            this.siteModel = siteModel
        }

        @Suppress("DEPRECATION")
        fun startRecording() {
            if (mediaRecorder == null) {
                val outputFile = File(contextProvider.getContext().externalCacheDir, "audiorecord.mp4")
                // Ensure that the file is created or overwritten
                outputFile.createNewFile()

                mediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    // todo: annmarie Do I need to worry about overwriting the file
                    audioFile = outputFile
//                        File(contextProvider.getContext().externalCacheDir?.absolutePath + "/audiorecord.mp4")
                    setOutputFile(audioFile.absolutePath)
                    // setOutputFile(audioFile.absolutePath)
                    prepare()
                }
            }

            mediaRecorder?.start()
            _isRecording.value = true
            _elapsedRecordingTime.value = 0L

            recordingJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _elapsedRecordingTime.value += 1
                    if (_elapsedRecordingTime.value >= 300 || audioFile.length() >= 25 * 1024 * 1024) {
                        stopRecording()
                    }
                }
            }
        }

        fun stopRecording() {
            Log.i(javaClass.simpleName, "***=> stopRecording")
            transitionToProcessingState()

            if (_isRecording.value) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                _isRecording.value = false
                recordingJob?.cancel()
                recordingJob = null
                // Transition to processing state
            }
        }

        fun discardRecording() {
            Log.i(javaClass.simpleName, "***=> discardRecording")
            if (_isRecording.value) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                _isRecording.value = false
                recordingJob?.cancel()
                recordingJob = null
            }
            audioFile.delete()
        }

    private fun transitionToProcessingState() {
        Log.i(javaClass.simpleName, "***=> Made it to transitionToProcessingState")
        siteModel?.let {
            // todo: annmarie use the right scope. lol
            launch(bgDispatcher) {
                Log.i(javaClass.simpleName, "***=> Right before making the BE call")
                when (val response = jetpackAIStore.fetchVoiceToContent(site = it, audioFile = audioFile)) {
                    is JetpackAIRestClient.VoiceToContentResponse.Success -> Log.i(javaClass.simpleName, "***=> Success Results = ${response.content}")
                    is JetpackAIRestClient.VoiceToContentResponse.Error -> Log.i(javaClass.simpleName, "***=> Error Results = ${response.message}")
                }

            }
        }
    }
}
