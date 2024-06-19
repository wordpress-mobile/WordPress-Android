package org.wordpress.android.ui.voicetocontent

import androidx.annotation.StringRes
import org.wordpress.android.R

data class HeaderUIModel (
    @StringRes val label: Int,
    val onClose: () -> Unit,
)

data class SecondaryHeaderUIModel(
    @StringRes val label: Int,
    val isLabelVisible: Boolean = true,
    val isProgressIndicatorVisible: Boolean = false,
    val requestsAvailable: String = "0",
    val timeMaxDurationInSeconds: Int = 0,
    val isTimeElapsedVisible: Boolean = false
)

data class RecordingPanelUIModel(
    val onMicTap: (() -> Unit)? = null,
    val onStopTap: (() -> Unit)? = null,
    val isEligibleForFeature: Boolean = false,
    val hasPermission: Boolean = false,
    val onRequestPermission: (() -> Unit)? = null,
    val isRecordEnabled: Boolean = false,
    val isEnabled: Boolean = false,
    @StringRes val ineligibleMessage: Int = R.string.voice_to_content_ineligible,
    @StringRes val upgradeMessage: Int = R.string.voice_to_content_upgrade,
    val upgradeUrl: String? = null,
    val onLinkTap: ((String) -> Unit)? = null,
    @StringRes val actionLabel: Int
)

data class ErrorUiModel(
    @StringRes val errorMessage: Int? = null,
    val allowRetry: Boolean = false,
    val onRetryTap: (() -> Unit)? = null
)

enum class VoiceToContentUIStateType(val trackingName: String) {
    INITIALIZING("initializing"),
    READY_TO_RECORD("ready_to_record"),
    INELIGIBLE_FOR_FEATURE("ineligible_for_feature"),
    RECORDING("recording"),
    PROCESSING("processing"),
    ERROR("error")
}

data class VoiceToContentUiState(
    val uiStateType: VoiceToContentUIStateType,
    val header: HeaderUIModel,
    val secondaryHeader: SecondaryHeaderUIModel? = null,
    val recordingPanel: RecordingPanelUIModel? = null,
    val errorPanel: ErrorUiModel? = null
)
