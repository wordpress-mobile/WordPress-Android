package org.wordpress.android.ui.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject


@HiltViewModel
class AudioRecorderViewModel @Inject constructor(
    private val contextProvider: ContextProvider
): ViewModel() {
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _transcription = MutableStateFlow<String?>(null)
    val transcription: StateFlow<String?> = _transcription

    private var storeInMemory = true

   // private var isPaused = false
    private var isPausedRecording = false

    init {
        if (contextProvider.getContext().
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            initializeSpeechRecognizer()
        }
    }

    @Suppress("DEPRECATION")
    fun startRecording(context: Context) {
        Log.i(javaClass.simpleName, "***=> startRecording")
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                val filePath = if (storeInMemory) {
                    context.cacheDir.absolutePath + "/recording.3gp"
                } else {
                    context.getExternalFilesDir(null)?.absolutePath + "/recording.3gp"
                }
                setOutputFile(filePath)
                try {
                    prepare()
                    start()
                    Log.i(javaClass.simpleName, "***=> created MediaRecorder")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _isRecording.value = true
            _isPaused.value = false
            isPausedRecording = false
        }
    }

    fun pauseRecording() {
        Log.i(javaClass.simpleName, "***=> pauseRecording")
        mediaRecorder?.apply {
            pause()
            isPausedRecording = true
            _isPaused.value = true
        }
        speechRecognizer?.stopListening()
    }

    fun resumeRecording() {
        Log.i(javaClass.simpleName, "***=> resumeRecording")
        if (isPausedRecording) {
            startSpeechRecognizer()
            CoroutineScope(Dispatchers.Main).launch {
                // Do something before delay
                delay(500) // Delay for 1000 milliseconds (1 second)
                // Do something after delay
                mediaRecorder?.resume()
                _isPaused.value = false
                isPausedRecording = false
            }
        }
    }

    fun stopRecording() {
        Log.i(javaClass.simpleName, "***=> stopRecording")
        mediaRecorder?.apply {
            stop()
            release()
        }
        speechRecognizer?.destroy()
        mediaRecorder?.release()
        mediaRecorder = null
        speechRecognizer = null
        _isPaused.value = false
        _isRecording.value = false
    }

    private fun initializeSpeechRecognizer() {
        Log.i(javaClass.simpleName, "***=> initializeSpeechRecognizer")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(contextProvider.getContext())
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onReadyForSpeech")
            }
            override fun onBeginningOfSpeech() {
                Log.i(javaClass.simpleName, "***=> onBeginningOfSpeech")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {
                Log.i(javaClass.simpleName, "***=> onBufferReceived")
            }
            override fun onEndOfSpeech() {
                Log.i(javaClass.simpleName, "***=> onEndOfSpeech")
            }
            override fun onError(error: Int) {
                Log.i(javaClass.simpleName, "***=> onError $error")
            }
            override fun onResults(results: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onResults")
                results?.let {
                    Log.i(javaClass.simpleName, "***=> onResults with some stuff maybe")
                    val matches = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    _transcription.value = matches?.get(0)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onPartialResults")
                partialResults?.let {
                    Log.i(javaClass.simpleName, "***=> onPartialResults with some stuff maybe")
                    val data = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val unstableData = it.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
                    _transcription.value = data?.get(0) + unstableData?.get(0)
                }
//                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                _transcription.value = matches?.get(0)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onEvent $eventType")
            }
        }

        speechRecognizer?.setRecognitionListener(listener)
        startSpeechRecognizer()
    }

    private fun startSpeechRecognizer() {
        Log.i(javaClass.simpleName, "***=> startSpeechRecognizer")
        speechRecognizer?.let {
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(EXTRA_PARTIAL_RESULTS, true)
               // API 33+ ONLY putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, MediaRecorder.AudioSource.MIC)
            }
            it.startListening(recognizerIntent)
            Log.i(javaClass.simpleName, "***=> started listening")
        }
    }

    override fun onCleared() {
        Log.i(javaClass.simpleName, "***=> onCleared")
        super.onCleared()
        speechRecognizer?.destroy()
        mediaRecorder?.release()
    }
}
