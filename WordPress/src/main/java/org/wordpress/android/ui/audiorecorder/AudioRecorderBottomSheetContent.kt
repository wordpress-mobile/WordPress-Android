package org.wordpress.android.ui.audiorecorder

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.wordpress.android.R

@Composable
fun AudioRecorderBottomSheetContent(
    viewModel: AudioRecorderViewModel = viewModel()
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val elapsedRecordingTime by viewModel.elapsedRecordingTime.collectAsState()

    BottomSheetContent(
        isRecording = isRecording,
        elapsedRecordingTime = elapsedRecordingTime,
        onMicrophoneClick = {
            if (!isRecording) {
                viewModel.startRecording()
            }
        },
        onStopClick = { viewModel.stopRecording() },
        onDiscardClick = { viewModel.discardRecording() }
    )
}

@Composable
fun BottomSheetContent(
    isRecording: Boolean,
    elapsedRecordingTime: Long,
    onMicrophoneClick: () -> Unit,
    onStopClick: () -> Unit,
    onDiscardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!isRecording) {
            InitialView(onMicrophoneClick = onMicrophoneClick)
        } else {
            RecordingView(
                elapsedRecordingTime = elapsedRecordingTime,
                onStopClick = onStopClick,
                onDiscardClick = onDiscardClick
            )
        }
    }
}

@Composable
fun InitialView(onMicrophoneClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Ready to Record", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            painterResource(id = R.drawable.ic_mic_white_24dp),
            contentDescription = "Microphone",
            modifier = Modifier
                .size(64.dp)
                .clickable { onMicrophoneClick() }
        )
    }
}

@Composable
fun RecordingView(
    elapsedRecordingTime: Long,
    onStopClick: () -> Unit,
    onDiscardClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Recording...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Elapsed Time: ${elapsedRecordingTime}s", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        // Placeholder for waveform image representation
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onStopClick) {
                Icon(
                    painterResource(id = R.drawable.gb_ic_redo),
                    contentDescription = "Stop Recording"
                )
            }
            IconButton(onClick = onDiscardClick) {
                Icon(
                    painterResource(id = R.drawable.gb_ic_tool),
                    contentDescription = "Discard Recording"
                )
            }
            IconButton(onClick = onDiscardClick) {
                Icon(
                    painterResource(id = R.drawable.gb_ic_trash),
                    contentDescription = "Discard Recording"
                )
            }
        }
    }
}

@Composable
fun ProcessingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Processing Audio", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            painterResource(id = R.drawable.ic_gridicons_checkmark_circle),
            contentDescription = "Processing"
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun InitialViewPreview() {
    val onClick = {}
    InitialView(onClick)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecordingViewPreview() {
    val onClick = {}
    RecordingView(10, onClick, onClick)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProcessingViewPreview() {
    ProcessingView()
}
