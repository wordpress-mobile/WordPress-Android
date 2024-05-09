package org.wordpress.android.ui.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@HiltViewModel
class AudioRecordViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
private val buildConfigWrapper: BuildConfigWrapper): ViewModel() {
    private var mediaRecorder: MediaRecorder? = null
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(contextProvider.getContext()) // context needs to be passed

    private val _translatedText = MutableStateFlow("")
    val translatedText = _translatedText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    init {
        setupSpeechRecognizer()
    }

    @Suppress("DEPRECATION")
    private fun setupMediaRecorder() {
        if (contextProvider.getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(contextProvider.getContext().cacheDir.absolutePath + "/recording.3gp")
                // setOutputFile("/dev/null")
                try {
                    prepare()
                    Log.i(javaClass.simpleName, "***=> created MediaRecorder")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupSpeechRecognizer() {
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
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    // Restart listening if there's no match or a timeout occurs
                    startSpeechRecognizer()
                } else {
                    // Handle other errors appropriately
                }
            }
            override fun onResults(results: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onResults")
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let {
                    if (it.isNotEmpty()) {
                        _translatedText.value = it[0]
                    }
                }
                // Restart listening after results are received
                startSpeechRecognizer()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onPartialResults")
//                partialResults?.let {
//                    Log.i(javaClass.simpleName, "***=> onPartialResults with some stuff maybe")
//                    val data = it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                    val unstableData = it.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
//                    Log.i(javaClass.simpleName, "***=> ${data?.get(0) + unstableData?.get(0)}")
//                }
//                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                _transcription.value = matches?.get(0)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.i(javaClass.simpleName, "***=> onEvent $eventType")
            }
        }
        speechRecognizer?.setRecognitionListener(listener)
    }

    fun startRecording() {
        if (!_isRecording.value) {
            setupMediaRecorder()
            mediaRecorder?.start()
            startSpeechRecognizer() // The intent setup for SpeechRecognizer should be defined
            _isRecording.value = true
        }
    }

    fun stopRecording() {
        if (_isRecording.value) {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            speechRecognizer.stopListening()
            _isRecording.value = false
        }
    }

    fun pauseRecording() {
        mediaRecorder?.pause()
        speechRecognizer.stopListening()
    }

    fun resumeRecording() {
        mediaRecorder?.resume()
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
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName())
                // API 33+ ONLY putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, MediaRecorder.AudioSource.MIC)
            }
            it.startListening(recognizerIntent)
            Log.i(javaClass.simpleName, "***=> started listening")
        }
    }
    override fun onCleared() {
        mediaRecorder?.release()
        speechRecognizer.destroy()
        super.onCleared()
    }

    private fun getPackageName(): String {
        val appSuffix = buildConfigWrapper.getApplicationId().split(".").last()
        val appPackage = if (appSuffix.isNotBlank() && !appSuffix.equals("ANDROID", ignoreCase = true)) {
            "${JETPACK_PACKAGE_NAME}.${appSuffix}"
        } else {
           JETPACK_PACKAGE_NAME
        }
        Log.i(javaClass.simpleName, "***=> The package name is (1) $appPackage and (2) ${contextProvider.getContext().packageName}")
        return appPackage
    }

    companion object{
        const val JETPACK_PACKAGE_NAME = "com.jetpack.android"
    }
}

