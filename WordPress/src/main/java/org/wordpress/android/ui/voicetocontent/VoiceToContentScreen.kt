package org.wordpress.android.ui.voicetocontent

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun VoiceToContentScreen(
    viewModel: VoiceToContentViewModel
) {
    val state by viewModel.state.collectAsState()
    val amplitudes by viewModel.amplitudes.observeAsState(initial = listOf())
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val bottomSheetHeight = screenHeight * 0.6f  // Set to 60% of screen height - but how can it be dynamic?

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomSheetHeight),
        color = MaterialTheme.colors.surface
    ) {
        VoiceToContentView(state, amplitudes)
    }
}

@Composable
fun VoiceToContentView(state: VoiceToContentUiState, amplitudes: List<Float>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colors.surface) // Use theme-aware background color
    ) {
        when (state.uiStateType) {
            VoiceToContentUIStateType.PROCESSING -> ProcessingView(state)
            VoiceToContentUIStateType.ERROR -> ErrorView(state)
            else -> {
                Header(state.header)
                SecondaryHeader(state.secondaryHeader)
                RecordingPanel(state.recordingPanel, amplitudes)
            }
        }
    }
}

@Composable
fun ProcessingView(model: VoiceToContentUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Header(model.header)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.size(100.dp) // size the progress indicator
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ErrorView(model: VoiceToContentUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Header(model.header)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Unable to use Voice to Content at the moment, please try again later")
    }
}

@Composable
fun Header(model: HeaderUIModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = model.label), style = headerStyle)
        IconButton(onClick = model.onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
    }
}

@Composable
fun SecondaryHeader(model: SecondaryHeaderUIModel?) {
    model?.let {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = model.label), style = secondaryHeaderStyle)
            Spacer(modifier = Modifier.width(8.dp)) // Add space between text and progress
            if (model.isProgressIndicatorVisible) {
                Box(
                    modifier = Modifier.size(20.dp) // size the progress indicator
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text(text = model.requestsAvailable, style = secondaryHeaderStyle)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RecordingPanel(model: RecordingPanelUIModel?, amplitudes: List<Float>) {
    model?.let {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent) // todo: annmarie double check if this is needed
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp) // Adjust padding as needed
            ) {
                if (model.isEligibleForFeature) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .padding(48.dp)
                    ) {
                        WaveformVisualizer(
                            amplitudes = amplitudes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(16.dp),
                            color = MaterialTheme.colors.primary
                        )
                    }
                } else {
                    Text(text = stringResource(id = model.message), style = errorMessageStyle)
                    Text(text = model.urlLink, style = errorUrlLinkCTA)
                }
                MicToStopIcon(model)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = model.actionLabel),
                    style = actionLabelStyle,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private val headerStyle: TextStyle
    @Composable
    get() = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.high)
    )

private val secondaryHeaderStyle: TextStyle
    @Composable
    get() = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
    )

private val actionLabelStyle: TextStyle
    @Composable
    get() = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.high)
    )

private val errorMessageStyle: TextStyle
    @Composable
    get() = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.high)
    )

private val errorUrlLinkCTA: TextStyle
    @Composable
    get() = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = MaterialTheme.colors.primary
    )

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewInitializingView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.INITIALIZING,
            header = HeaderUIModel(label = R.string.voice_to_content_base_header_label, onClose = { }),
            secondaryHeader = SecondaryHeaderUIModel(
                label = R.string.voice_to_content_secondary_header_label,
                isProgressIndicatorVisible = true
            ),
            recordingPanel = RecordingPanelUIModel(
                actionLabel = R.string.voice_to_content_begin_recording_label,
                isEnabled = false,
                hasPermission = false
            )
        )
        VoiceToContentView(state = state, amplitudes = listOf())
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewReadyToRecordView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.READY_TO_RECORD,
            header = HeaderUIModel(label = R.string.voice_to_content_ready_to_record_label, onClose = { }),
            secondaryHeader = SecondaryHeaderUIModel(label = R.string.voice_to_content_secondary_header_label),
            recordingPanel = RecordingPanelUIModel(
                actionLabel = R.string.voice_to_content_begin_recording_label,
                isEnabled = true,
                onMicTap = {},
                onStopTap = {},
                onRequestPermission = {},
                isEligibleForFeature = true
            )
        )
        VoiceToContentView(state = state, amplitudes = listOf())
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewNotEligibleToRecordView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.INELIGIBLE_FOR_FEATURE,
            header = HeaderUIModel(label = R.string.voice_to_content_ready_to_record_label, onClose = { }),
            secondaryHeader = SecondaryHeaderUIModel(label = R.string.voice_to_content_secondary_header_label),
            recordingPanel = RecordingPanelUIModel(
                actionLabel = R.string.voice_to_content_begin_recording_label,
                isEnabled = false,
                isEligibleForFeature = false,
                urlLink = "https://www.wordpress.com"
            )
        )
        VoiceToContentView(state = state, amplitudes = listOf())
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewRecordingView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.RECORDING,
            header = HeaderUIModel(label = R.string.voice_to_content_recording_label, onClose = { }),
            secondaryHeader = SecondaryHeaderUIModel(label = R.string.voice_to_content_secondary_header_label),
            recordingPanel = RecordingPanelUIModel(
                actionLabel = R.string.voice_to_content_begin_recording_label,
                isEnabled = true,
                hasPermission = true,
                onMicTap = {},
                onStopTap = {},
                onRequestPermission = {},
                isEligibleForFeature = true
            )
        )
        VoiceToContentView(
            state = state,
            amplitudes = listOf(
                1.1f,
                2.2f,
                3.3f,
                4.4f,
                2.2f,
                3.3f,
                1.1f
            )
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewProcessingView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.PROCESSING,
            header = HeaderUIModel(label = R.string.voice_to_content_processing_label, onClose = { })
        )
        VoiceToContentView(state = state, amplitudes = listOf())
    }
}
