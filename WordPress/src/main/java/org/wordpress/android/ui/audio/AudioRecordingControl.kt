import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AudioRecordingControl(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    isRecording: Boolean,
    isPaused: Boolean
) {
    Surface {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartClick,
                enabled = !isRecording,
                content = { Text(text = "Start") }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onPauseClick,
                enabled = isRecording && !isPaused,
                content = { Text(text = "Pause") }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onResumeClick,
                enabled = isPaused,
                content = { Text(text = "Resume") }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onStopClick,
                enabled = isRecording,
                content = { Text(text = "Stop") }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AudioRecorderContentPreview() {
    // Call your content composable function here for preview
    AudioRecordingControl({}, {}, {}, {}, true, false)
}
