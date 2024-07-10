package org.wordpress.android.util.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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
import org.wordpress.android.util.AppLog
import java.io.File
import java.io.IOException
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult.Success
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult.Error

class AudioRecorder(
    private val applicationContext: Context,
    private val recordingStrategy: RecordingStrategy
) : IAudioRecorder {
    private var onRecordingFinished: (AudioRecorderResult) -> Unit = {}

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

    private val amplitudeList = mutableListOf<Float>()
    private var remainingTimeInSeconds: Int = 0

    private val _recordingUpdates = MutableStateFlow(RecordingUpdate())
    private val recordingUpdates: StateFlow<RecordingUpdate> get() = _recordingUpdates.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    private val isRecording = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    private val isPaused = _isPaused.asStateFlow()

    @Suppress("DEPRECATION")
    override fun startRecording(onRecordingFinished: (AudioRecorderResult) -> Unit) {
        this.onRecordingFinished = onRecordingFinished
        if (applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(applicationContext)
                } else {
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(filePath)

                    prepare()
                    start()
                    remainingTimeInSeconds = recordingStrategy.maxDuration
                    amplitudeList.clear()
                    startRecordingUpdates()
                    _isRecording.value = true
                    _isPaused.value = false
                }
            } catch (e: IOException) {
                onRecordingFinished(Error("Error preparing MediaRecorder: ${e.message}"))
            } catch (e: IllegalStateException) {
                onRecordingFinished(Error("Illegal state when starting recording: ${e.message}"))
            } catch (e: SecurityException) {
                onRecordingFinished(Error("Security exception when starting recording: ${e.message}"))
            }
        } else {
            onRecordingFinished(Error("Permission to record audio not granted"))
        }
    }

    private fun clearResources() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: IllegalStateException) {
            AppLog.w(AppLog.T.UTILS, "$TAG Error stopping recording: ${e.message}")
        } finally {
            recorder = null
            stopRecordingUpdates()
            _isPaused.value = false
            _isRecording.value = false
        }
    }

    override fun stopRecording() {
        clearResources()
        // return the filePath
        onRecordingFinished(Success(filePath))
    }

    override fun pauseRecording() {
        try {
            recorder?.pause()
            _isPaused.value = true
            stopRecordingUpdates()
        } catch (e: IllegalStateException) {
            AppLog.w(AppLog.T.UTILS, "$TAG Error pausing recording: ${e.message}")
        } catch (e: UnsupportedOperationException) {
            AppLog.w(AppLog.T.UTILS, "$TAG Pause not supported on this device: ${e.message}")
        }
    }

    override fun resumeRecording() {
        if (_isPaused.value) {
            coroutineScope.launch {
                try {
                    recorder?.resume()
                    _isPaused.value = false
                    val lastRecordingUpdate = recordingUpdates.value
                    _recordingUpdates.value = lastRecordingUpdate.copy(
                        amplitudes = amplitudeList.toList() // Continue using the existing list
                    )
                    startRecordingUpdates()
                } catch (e: IllegalStateException) {
                    AppLog.w(AppLog.T.UTILS, "$TAG Error resuming recording ${e.message}")
                }
            }
        }
    }

    override fun endRecordingSession() {
        clearResources()
    }

    override fun recordingUpdates(): Flow<RecordingUpdate> = recordingUpdates
    override fun isRecording(): StateFlow<Boolean> = isRecording
    override fun isPaused(): StateFlow<Boolean> = isPaused

    @Suppress("MagicNumber")
    private fun startRecordingUpdates() {
        var lastUpdateTime = System.currentTimeMillis()
        recordingJob = coroutineScope.launch {
            while (recorder != null && !_isPaused.value) {
                delay(RECORDING_UPDATE_INTERVAL)
                val currentTime = System.currentTimeMillis()
                val elapsedTimeInMillis = currentTime - lastUpdateTime

                if (elapsedTimeInMillis >= 1000) {
                    remainingTimeInSeconds -= (elapsedTimeInMillis / 1000).toInt()
                    lastUpdateTime += (elapsedTimeInMillis / 1000) * 1000 // Reset last update time accurately
                }

                val fileSize = File(filePath).length()
                val amplitude = recorder?.maxAmplitude?.toFloat() ?: 0f
                amplitudeList.add(amplitude)
                // Keep the list to a manageable size (e.g., last 1000 samples)
                if (amplitudeList.size > 1000) {
                    amplitudeList.removeAt(0)
                }
                _recordingUpdates.value = RecordingUpdate(
                    remainingTimeInSeconds = remainingTimeInSeconds,
                    fileSize = fileSize,
                    fileSizeLimitExceeded = fileSize >= recordingStrategy.maxFileSize,
                    amplitudes = amplitudeList.toList()
                )

                if ( maxFileSizeExceeded(fileSize) || durationExceeded(remainingTimeInSeconds) ) {
                    stopRecording()
                    break
                }
            }
        }
    }

    /**
     * Checks if the recorded file size has exceeded the specified maximum file size.
     *
     * @param fileSize The current size of the recorded file in bytes.
     * @return `true` if the file size has exceeded the maximum file size minus the threshold, `false` otherwise.
     *         If `recordingParams.maxFileSize` is set to `-1L`, this function always returns `false` indicating
     *         no limit.
     */
    private fun maxFileSizeExceeded(fileSize: Long): Boolean = when {
        recordingStrategy.maxFileSize == -1L -> false
        else -> fileSize >= recordingStrategy.maxFileSize - FILE_SIZE_THRESHOLD
    }

    /**
     * Checks if the recording duration has exceeded the limit.
     *
     * @param elapsedTimeInSeconds The elapsed recording time in seconds.
     * @return `true` if the elapsed time has reached zero, `false` otherwise.
     *         If `recordingParams.maxDuration` is set to `-1`, this function always returns `false` indicating
     *         no limit.
     */
    private fun durationExceeded(elapsedTimeInSeconds: Int): Boolean = when {
        recordingStrategy.maxDuration == -1 -> false
        else -> elapsedTimeInSeconds <= 0
    }

    private fun stopRecordingUpdates() {
        recordingJob?.cancel()
    }

    companion object {
        private const val TAG = "AudioRecorder"
        private const val RECORDING_UPDATE_INTERVAL = 75L // in milliseconds
        private const val FILE_SIZE_THRESHOLD = 100000L
    }
}
