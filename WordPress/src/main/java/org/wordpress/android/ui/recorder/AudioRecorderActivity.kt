package org.wordpress.android.ui.recorder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AudioRecorderActivity : AppCompatActivity() {
    private val viewModel: AudioRecordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioRecorderScreen(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
      //  viewModel.resumeIfNeeded() // Resume or start listening when the activity resumes
    }

    override fun onPause() {
        super.onPause()
       //  viewModel.pauseIfNeeded() // Stop listening to prevent capturing audio when the app is not in the foreground
    }

    override fun onDestroy() {
        super.onDestroy()
       // viewModel.stopRecording() // Cleanup resources
    }
}

@Composable
fun AudioRecorderScreen(viewModel: AudioRecordViewModel) {
    val isRecording = viewModel.isRecording.collectAsState()
    val translatedText = viewModel.translatedText.collectAsState()

    Column {
        Button(onClick = {
            if (isRecording.value) viewModel.stopRecording() else viewModel.startRecording()
        }) {
            Text(if (isRecording.value) "Stop Recording" else "Start Recording")
        }
        Button(onClick = {
            if (isRecording.value) viewModel.pauseRecording() else viewModel.resumeRecording()
        }) {
            Text(if (isRecording.value) "Pause" else "Resume")
        }
        Text("Translation: ${translatedText.value}")
    }
}
