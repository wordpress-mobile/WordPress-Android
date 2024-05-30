package org.wordpress.android.ui.voicetocontent

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun VoiceToContentScreen(
    viewModel: VoiceToContentViewModel
) {
    val state by viewModel.state.collectAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (val currentState = state) {
            is VoiceToContentUiState.Initializing -> InitializingView(currentState)
            is VoiceToContentUiState.ReadyToRecord -> ReadyToRecordView(currentState)
            is VoiceToContentUiState.Recording -> RecordingView(currentState)
            is VoiceToContentUiState.Processing -> ProcessingView(currentState)
        }
    }
}

@Composable
fun InitializingView(state: VoiceToContentUiState.Initializing) {
    Column {
        Header(state.headerText, state.onClose)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = state.labelText))
        CircularProgressIndicator()
    }
}

@Composable
fun ReadyToRecordView(
    state: VoiceToContentUiState.ReadyToRecord
) {
    Column {
        Header(state.headerText, state.onClose)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = state.labelText))
        Text(stringResource(id = state.subLabelText))
        Spacer(modifier = Modifier.height(16.dp))
        IconButton(
            enabled = true,
            onClick = if (state.hasPermission) {
                state.onMicTap
            } else {
                state.onRequestPermission
            }
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
        }
    }
}

@Composable
fun RecordingView(state: VoiceToContentUiState.Recording) {
    Column {
        Header(state.headerText, state.onClose)
        Spacer(modifier = Modifier.height(16.dp))
        Text(state.elapsedTime)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(imageVector = Icons.Default.Call, contentDescription = null)
        Spacer(modifier = Modifier.height(16.dp))
        IconButton(onClick = state.onStopTap, enabled = true) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
        }
    }
}

@Composable
fun ProcessingView(state: VoiceToContentUiState.Processing) {
    Column {
        Header(state.headerText, state.onClose)
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
    }
}


@Composable
fun Header(@StringRes headerText: Int, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = headerText), style = MaterialTheme.typography.h6)
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInitializingView() {
    AppTheme {
        InitializingView(VoiceToContentUiState.Initializing(
            headerText = R.string.voice_to_content_initializing,
            labelText = R.string.voice_to_content_preparing,
            onCloseAction = {}
        ))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReadyToRecordView() {
    AppTheme {
        ReadyToRecordView(VoiceToContentUiState.ReadyToRecord(
            headerText = R.string.voice_to_content_ready_to_record,
            labelText = R.string.voice_to_content_ready_to_record_label,
            subLabelText = R.string.voice_to_content_tap_to_start,
            onMicTap = {},
            onCloseAction = {},
            hasPermission = true,
            onRequestPermission = {}
        ))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecordingView() {
    AppTheme {
        RecordingView(VoiceToContentUiState.Recording(
            headerText = R.string.voice_to_content_recording,
            elapsedTime = "0 sec",
            onStopTap = {},
            onCloseAction = {}
        ))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewProcessingView() {
    AppTheme {
        ProcessingView(VoiceToContentUiState.Processing(
            headerText = R.string.voice_to_content_processing,
            onCloseAction = {}
        ))
    }
}
