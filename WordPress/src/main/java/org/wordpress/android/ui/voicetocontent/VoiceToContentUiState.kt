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
    val timeElapsed: String = "00:00:00",
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
    @StringRes val message: Int = R.string.voice_to_content_not_eligible_for_feature,
    val urlMessage: String = "",
    val urlLink: String = "",
    val onLinkTap: ((String) -> Unit)? = null,
    @StringRes val actionLabel: Int
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
    val recordingPanel: RecordingPanelUIModel? = null
)
