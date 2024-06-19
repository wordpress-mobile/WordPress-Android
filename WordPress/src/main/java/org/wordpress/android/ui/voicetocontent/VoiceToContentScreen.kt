package org.wordpress.android.ui.voicetocontent

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.Drawable
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.audio.RecordingUpdate
import java.util.Locale

@Composable
fun VoiceToContentScreen(
    viewModel: VoiceToContentViewModel
) {
    val state by viewModel.state.collectAsState()
    val recordingUpdate by viewModel.recordingUpdate.observeAsState(initial = RecordingUpdate())
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    // Adjust the bottom sheet height based on orientation
    val bottomSheetHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        screenHeight // Full height in landscape
    } else {
        screenHeight * 0.6f // 60% height in portrait
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomSheetHeight),
        color = MaterialTheme.colors.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection()) // Enable nested scrolling for the bottom sheet
                .verticalScroll(rememberScrollState()) // Enable vertical scrolling for the bottom sheet
        ) {
            VoiceToContentView(state, recordingUpdate)
        }
    }
}

@Composable
fun VoiceToContentView(state: VoiceToContentUiState, recordingUpdate: RecordingUpdate) {
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
                SecondaryHeader(state.secondaryHeader, recordingUpdate)
                RecordingPanel(state, recordingUpdate)
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
        Text(stringResource(id = model.errorPanel?.errorMessage?:R.string.voice_to_content_generic_error))
        if (model.errorPanel?.allowRetry == true) {
            IconButton(onClick = model.errorPanel.onRetryTap?:{}) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            }
        }
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
fun SecondaryHeader(model: SecondaryHeaderUIModel?, recordingUpdate: RecordingUpdate) {
    model?.let {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (model.isLabelVisible) {
                Text(text = stringResource(id = model.label), style = secondaryHeaderStyle)
                Spacer(modifier = Modifier.width(8.dp)) // Add space between text and progress
            }
            if (model.isProgressIndicatorVisible) {
                Box(
                    modifier = Modifier.size(20.dp) // size the progress indicator
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text(
                    text = if (model.isTimeElapsedVisible)
                        formatTime(recordingUpdate.remainingTimeInSeconds, model.timeMaxDurationInSeconds)
                    else model.requestsAvailable,
                    style = secondaryHeaderStyle
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun formatTime(remainingTimeInSeconds: Int, maxDurationInSeconds: Int): String {
    val default = getDefaultTimeString(maxDurationInSeconds)
    if (remainingTimeInSeconds == -1) return default

    val minutes = remainingTimeInSeconds / 60
    val seconds = remainingTimeInSeconds % 60

    val value = if (minutes == 1) default
        else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    return value
}

@Composable
fun getDefaultTimeString(maxDurationInSeconds: Int): String {
    if (maxDurationInSeconds <= 0) {
        return "00:00"
    }

    // Calculate minutes and seconds
    val minutes = (maxDurationInSeconds - 1) / 60
    val seconds = (maxDurationInSeconds - 1) % 60

    // Format and return the default time string
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}


@Composable
fun RecordingPanel(model: VoiceToContentUiState, recordingUpdate: RecordingUpdate) {
    model.recordingPanel?.let {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp) // Adjust padding as needed
            ) {
                if (it.isEligibleForFeature) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .padding(48.dp)
                    ) {
                        ScrollingWaveformVisualizer(recordingUpdate = recordingUpdate)
                    }
                } else if (model.uiStateType == VoiceToContentUIStateType.INELIGIBLE_FOR_FEATURE) {
                    InEligible(model = it)
                }
                MicToStopIcon(it)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = it.actionLabel),
                    style = if (it.isEnabled) actionLabelStyle else actionLabelStyleDisabled
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun InEligible(
    model: RecordingPanelUIModel,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(text = stringResource(id = model.ineligibleMessage), style = errorMessageStyle)
         if (model.upgradeUrl?.isNotBlank() == true) {
            ClickableTextViewWithLinkImage(
                text = stringResource(id = model.upgradeMessage),
                drawableRight = Drawable(R.drawable.ic_external_white_24dp),
                onClick = { model.onLinkTap?.invoke(model.upgradeUrl) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ClickableTextViewWithLinkImage(
    modifier: Modifier = Modifier,
    drawableRight: Drawable? = null,
    text: String,
    onClick: () -> Unit
) {
    ConstraintLayout(modifier = modifier
        .clickable { onClick.invoke() }) {
        val (buttonTextRef) = createRefs()
        Box(modifier = Modifier
            .constrainAs(buttonTextRef) {
                end.linkTo(parent.end, drawableRight?.iconSize ?: 0.dp)
                width = Dimension.wrapContent
            }
        ) {
            Text(
                text = text,
                style = errorUrlLinkCTA
            )
        }

        drawableRight?.let { drawable ->
            val (imageRight) = createRefs()
            Image(
                modifier = Modifier
                    .constrainAs(imageRight) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(buttonTextRef.end, margin = 0.dp)
                    }
                    .size(16.dp),
                painter = painterResource(id = drawable.resId),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                contentDescription = null
            )
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

private val actionLabelStyleDisabled: TextStyle
    @Composable
    get() = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
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
        VoiceToContentView(state = state, recordingUpdate = RecordingUpdate())
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewReadyToRecordView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.READY_TO_RECORD,
            header = HeaderUIModel(label = R.string.voice_to_content_base_header_label, onClose = { }),
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
        VoiceToContentView(state = state, recordingUpdate = RecordingUpdate())
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewNotEligibleToRecordView() {
    AppTheme {
        val state = VoiceToContentUiState(
            uiStateType = VoiceToContentUIStateType.INELIGIBLE_FOR_FEATURE,
            header = HeaderUIModel(label = R.string.voice_to_content_base_header_label, onClose = { }),
            secondaryHeader = SecondaryHeaderUIModel(label = R.string.voice_to_content_secondary_header_label),
            recordingPanel = RecordingPanelUIModel(
                actionLabel = R.string.voice_to_content_begin_recording_label,
                isEnabled = false,
                isEligibleForFeature = false,
                upgradeMessage = R.string.voice_to_content_upgrade,
                upgradeUrl = "https://www.wordpress.com"
            )
        )
        VoiceToContentView(state = state, recordingUpdate = RecordingUpdate())
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
        VoiceToContentView(state = state, recordingUpdate = RecordingUpdate())
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
        VoiceToContentView(state = state, recordingUpdate = RecordingUpdate())
    }
}
