package org.wordpress.android.util.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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

    private var isPausedRecording = false

    private val _recordingUpdates = MutableStateFlow(RecordingUpdate())
    private val recordingUpdates: StateFlow<RecordingUpdate> get() = _recordingUpdates.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

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
                    startRecordingUpdates()
                    _isRecording.value = true
                    _isPaused.value = false
                }
            } catch (e: IOException) {
                val errorMessage = "Error preparing MediaRecorder: ${e.message}"
                Log.e(TAG, errorMessage)
                onRecordingFinished(Error(errorMessage))
            } catch (e: IllegalStateException) {
                val errorMessage = "Illegal state when starting recording: ${e.message}"
                Log.e(TAG, errorMessage)
                onRecordingFinished(Error(errorMessage))
            } catch (e: SecurityException) {
                val errorMessage = "Security exception when starting recording: ${e.message}"
                Log.e(TAG, errorMessage)
                onRecordingFinished(Error(errorMessage))
            }
        } else {
            val errorMessage = "Permission to record audio not granted"
            Log.e(TAG, errorMessage)
            onRecordingFinished(Error(errorMessage))
        }
    }

    override fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
        } finally {
            recorder = null
            stopRecordingUpdates()
            _isPaused.value = false
            _isRecording.value = false
        }
        // return filePath
        onRecordingFinished(Success(filePath))
    }

    override fun pauseRecording() {
        try {
            recorder?.pause()
            _isPaused.value = true
            stopRecordingUpdates()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error pausing recording: ${e.message}")
        } catch (e: UnsupportedOperationException) {
            Log.e(TAG, "Pause not supported on this device: ${e.message}")
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
                    Log.e(TAG, "Error resuming recording")
                }
            }
        }
    }

    override fun recordingUpdates(): Flow<RecordingUpdate> = recordingUpdates

    @Suppress("MagicNumber")
    private fun startRecordingUpdates() {
        recordingJob = coroutineScope.launch {
            var elapsedTimeInSeconds = 0
            while (recorder != null) {
                delay(RECORDING_UPDATE_INTERVAL)
                elapsedTimeInSeconds += (RECORDING_UPDATE_INTERVAL / 1000).toInt()
                val fileSize = File(filePath).length()
                _recordingUpdates.value = RecordingUpdate(
                    elapsedTime = elapsedTimeInSeconds,
                    fileSize = fileSize,
                    fileSizeLimitExceeded = fileSize >= recordingStrategy.maxFileSize,
                )

                if ( maxFileSizeExceeded(fileSize) || maxDurationExceeded(elapsedTimeInSeconds) ) {
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
     * Checks if the recording duration has exceeded the specified maximum duration.
     *
     * @param elapsedTimeInSeconds The elapsed recording time in seconds.
     * @return `true` if the elapsed time has exceeded the maximum duration minus the threshold, `false` otherwise.
     *         If `recordingParams.maxDuration` is set to `-1`, this function always returns `false` indicating
     *         no limit.
     */
    private fun maxDurationExceeded(elapsedTimeInSeconds: Int): Boolean = when {
        recordingStrategy.maxDuration == -1 -> false
        else -> elapsedTimeInSeconds >= recordingStrategy.maxDuration - DURATION_THRESHOLD
    }

    private fun stopRecordingUpdates() {
        recordingJob?.cancel()
    }

    companion object {
        private const val TAG = "AudioRecorder"
        private const val RECORDING_UPDATE_INTERVAL = 1000L // in milliseconds
        private const val RESUME_DELAY = 500L // in milliseconds
        private const val FILE_SIZE_THRESHOLD = 100000L
        private const val DURATION_THRESHOLD = 1
    }
}
