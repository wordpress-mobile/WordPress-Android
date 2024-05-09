package org.wordpress.android.ui.audio

import AudioRecordingControl
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@AndroidEntryPoint
class AudioRecorderActivity : AppCompatActivity() {
    private val viewModel: AudioRecorderViewModel  by viewModels()
    @Inject lateinit var contextProvider: ContextProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AudioRecorderApp(viewModel)
            }
        }
        handlePermissions()
    }

    private fun handlePermissions() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 123
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        // Call your main composable function here for preview
        AudioRecorderApp(viewModel = AudioRecorderViewModel(contextProvider))
    }
}

@Composable
fun AudioRecorderApp(viewModel: AudioRecorderViewModel) {
    AudioRecorderContent(viewModel)
}

@Composable
fun AudioRecorderContent(viewModel: AudioRecorderViewModel) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val transcription by viewModel.transcription.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AudioRecordingControl(
            onStartClick = { viewModel.startRecording(context) },
            onStopClick = { viewModel.stopRecording() },
            onPauseClick = { viewModel.pauseRecording() },
            onResumeClick = { viewModel.resumeRecording() },
            isRecording = isRecording,
            isPaused = isPaused
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isRecording) {
            Log.i("AudioRecorderActivity", "***=> isRecording")
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 8.dp)
            ) {
                // You can use a different icon or background color here
                // For example, Icon(Icons.Default.Mic, contentDescription = "Recording")
                Surface(
                    color = Color.Blue, //MaterialTheme.colors.primary,
                    shape = CircleShape
                ) {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }
        }


        Text("Transcription:", modifier = Modifier.padding(bottom = 8.dp))

        // Use `observeAsState()` to observe changes in the State<Boolean?>
        Text(text = transcription ?: "", modifier = Modifier.padding(16.dp))

// Recording indicator

    }
}


//@Preview(showBackground = true)
//@Composable
//fun AudioRecorderContentPreview() {
//    // Call your content composable function here for preview
//    AudioRecorderContent(viewModel = AudioRecorderViewModel())
//}
