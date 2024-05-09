package org.wordpress.android.ui.audio

import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class OSAudioRecorder @Inject constructor(
    private val contextProvider: ContextProvider
) : AudioRecorder
{
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null

    @Suppress("DEPRECATION")
    private fun createRecorder() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(contextProvider.getContext())
        } else {
            MediaRecorder()
        }

    override fun start(audioOutputRequestType: AudioOutputRequestType) {
        Log.i(javaClass.simpleName, "***=> OSAudioRecorder being used ?")
        mediaRecorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) // OR AAC
                val filePath = if (audioOutputRequestType == AudioOutputRequestType.MEMORY) {
                    contextProvider.getContext().cacheDir.absolutePath + "/recording.3gp"
                } else {
                    contextProvider.getContext().getExternalFilesDir(null)?.absolutePath + "/recording.3gp"
                }
            // todo: annmarie should I use the FileOutputStream(filePath).fd ???
                setOutputFile(filePath)
                try {
                    prepare()
                    start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(contextProvider.getContext())
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
               // val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
               // todo: annmarie _transcription.value = matches?.get(0)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                // val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                // todo: annmarie _transcription.value = matches?.get(0)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(listener)
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun onCleared() {
        speechRecognizer?.destroy()
        mediaRecorder?.release()
    }

    override fun stop() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
       // todo: annmarie _isRecording.value = false
    }

}
