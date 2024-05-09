package org.wordpress.android.ui.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.wordpress.android.viewmodel.ContextProvider
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executors
import javax.inject.Inject


class AudioFileSpeechRecognition @Inject constructor(private val contextProvider: ContextProvider) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null
    private var audioFileInputStream: FileInputStream? = null
    private val executor = Executors.newSingleThreadExecutor()

    fun recognizeFromFile(audioFilePath: String) {
        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(contextProvider.getContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(javaClass.simpleName, "***=> Audio permission not given")
            // Request the permission if not granted
//            ActivityCompat.requestPermissions(
//                contextProvider.getContext(),
//                arrayOf(Manifest.permission.RECORD_AUDIO),
//                REQUEST_RECORD_AUDIO_PERMISSION
//            )
            return
        }

        // Check if SpeechRecognizer is available
        if (!SpeechRecognizer.isRecognitionAvailable(contextProvider.getContext())) {
            // Speech recognition is not available on this device
            return
        }

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(contextProvider.getContext())
        recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(recognitionListener)

        // Open the audio file for reading
        try {
            audioFileInputStream = FileInputStream(audioFilePath)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        // Start streaming audio data from the file for recognition
        executor.execute {
            val audioBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioData = ByteArray(audioBufferSize)
            var bytesRead: Int
            while (true) {
                try {
                    bytesRead = audioFileInputStream?.read(audioData) ?: -1
                    if (bytesRead == -1) {
                        break
                    }
                    if (bytesRead > 0) {
                        // Feed audio data to SpeechRecognizer
                        speechRecognizer?.startListening(
                            RecognizerIntent.getVoiceDetailsIntent(contextProvider.getContext()).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            }
                        )
                       // todo: annmarie this method doesn't exist  speechRecognizer?.writeAudio(audioData, 0, bytesRead)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun stopRecognition() {
        // Stop SpeechRecognizer and close the audio file input stream
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        audioFileInputStream?.close()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val SAMPLE_RATE = 16000 // Sample rate in Hz
    }
}
