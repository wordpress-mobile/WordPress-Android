package org.wordpress.android.util.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AudioRecorder(
    private val applicationContext: Context
) : IAudioRecorder {
    // default recording params
    private var recordingParams: RecordingParams = RecordingParams(
        maxDuration = 60 * 5, // 5 minutes
        maxFileSize = 1000000L * 25 // 25MB
    )

    private var onRecordingFinished: (String) -> Unit = {}

    private val storeInMemory = true
    private val filePath by lazy {
        if (storeInMemory) {
            applicationContext.cacheDir.absolutePath + "/recording.mp4"
        } else {
            applicationContext.getExternalFilesDir(null)?.absolutePath + "/recording.mp4"
        }
    }

    private var recorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isPausedRecording = false

    private val _recordingUpdates = MutableStateFlow(RecordingUpdate())
    private val recordingUpdates: StateFlow<RecordingUpdate> get() = _recordingUpdates.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    @Suppress("DEPRECATION")
    override fun startRecording(onRecordingFinished: (String) -> Unit) {
        this.onRecordingFinished = onRecordingFinished
        if (applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filePath)

                try {
                    prepare()
                    start()
                    startRecordingUpdates()
                    _isRecording.value = true
                    _isPaused.value = false
                } catch (e: IOException) {
                    // Use a logging framework like Timber
                    Log.e("AudioRecorder", "Error starting recording")
                }
            }
        } else {
            // Handle permission not granted case, e.g., throw an exception or show a message
            Log.e("AudioRecorder","Permission to record audio not granted")
        }
    }

    override fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: IllegalStateException) {
            Log.e("AudioRecorder", "Error stopping recording")
        } finally {
            recorder = null
            stopRecordingUpdates()
            _isPaused.value = false
            _isRecording.value = false
        }
        // return filePath
        onRecordingFinished(filePath)
    }

    override fun pauseRecording() {
        if (recorder != null) {
            try {
                recorder?.pause()
                _isPaused.value = true
                stopRecordingUpdates()
            } catch (e: IllegalStateException) {
                Log.e("AudioRecorder", "Error pausing recording")
            }
        } else {
            Log.e("AudioRecorder","Pause not supported on this device")
        }
    }

    override fun resumeRecording() {
        if (isPausedRecording) {
            coroutineScope.launch {
                try {
                    delay(RESUME_DELAY)
                    recorder?.resume()
                    _isPaused.value = false
                    isPausedRecording = false
                    startRecordingUpdates()
                } catch (e: IllegalStateException) {
                    Log.e("AudioRecorder", "Error resuming recording")
                }
            }
        }
    }

    override fun recordingUpdates(): Flow<RecordingUpdate> = recordingUpdates

    override fun setRecordingParams(params: RecordingParams) {
        recordingParams = params
    }

    private fun startRecordingUpdates() {
        recordingJob = coroutineScope.launch {
            var elapsedTime = 0
            while (recorder != null) {
                delay(RECORDING_UPDATE_INTERVAL)
                elapsedTime++
                val fileSize = File(filePath).length()
                _recordingUpdates.value = RecordingUpdate(
                    elapsedTime = elapsedTime,
                    fileSize = fileSize,
                    fileSizeLimitExceeded = fileSize >= recordingParams.maxFileSize,
                )

                if (fileSize >= recordingParams.maxFileSize
                    || elapsedTime >= recordingParams.maxDuration) {
                    stopRecording()
                }
            }
        }
    }

    private fun stopRecordingUpdates() {
        recordingJob?.cancel()
    }

    companion object {
        private const val RECORDING_UPDATE_INTERVAL = 1000L
        private const val RESUME_DELAY = 500L
    }
}
