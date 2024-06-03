package org.wordpress.android.ui.voicetocontent

import androidx.annotation.StringRes

sealed class VoiceToContentUiState(
    @StringRes open val header: Int,
    @StringRes open val subHeader: Int,
    open val isSubHeaderVisible: Boolean,
    open val isInitializingProgressIndicatorVisible: Boolean,
    open val isRequestsAvailableVisible: Boolean,
    open val requestsAvailable: Int,
    open val isCountDownVisible: Boolean,
    open val isMicButtonVisible: Boolean,
    open val isMicButtonEnabled: Boolean,
    open val isStopButtonVisible: Boolean,
    open val isStopButtonEnabled: Boolean,
    open val isWaveFormVisible: Boolean,
    @StringRes open val status: Int,
    open val isStatusVisible: Int,

    open val onClose: () -> Unit,
    open val onCloseAction: () -> Unit,
) {
    data class Initializing(
        @StringRes override val header: Int,
        @StringRes override val subHeader: Int,
        override val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState(header, onCloseAction, onClose)

    data class ReadyToRecord(
        @StringRes override val header: Int,
        @StringRes val labelText: Int,
        @StringRes val subLabelText: Int,
        val requestsAvailable: Int,
        val isEligibleForFeature: Boolean,
        val onMicTap: () -> Unit,
        override val onCloseAction: () -> Unit,
        val hasPermission: Boolean,
        val onRequestPermission: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState(header, onCloseAction, onClose)

    data class Recording(
        @StringRes override val header: Int,
        val elapsedTime: String,
        val onStopTap: () -> Unit,
        override val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState(header, onCloseAction, onClose)

    data class Processing(
        @StringRes override val header: Int,
        override val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState(header, onCloseAction, onClose)

    data class Finished(
        @StringRes override val header: Int,
        val content: String, // Adjust as needed
        override val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState(header, onCloseAction, onClose)

    data class Error(
        @StringRes override val header: Int,
        val message: String, // Adjust as needed
        override val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState(header, onCloseAction, onClose)
}

//sealed class VoiceToContentUiState {
//    abstract val headerText: Int
//    abstract val onClose: () -> Unit
//
//    data class Initializing(
//        override val headerText: Int,
//        val labelText: Int,
//        val onCloseAction: () -> Unit,
//        override val onClose: () -> Unit = onCloseAction
//    ) : VoiceToContentUiState()
//
//    data class ReadyToRecord(
//        override val headerText: Int,
//        val labelText: Int,
//        val subLabelText: Int,
//        val requestsAvailable: Int,
//        val isEligibleForFeature: Boolean,
//        val onMicTap: () -> Unit,
//        val onCloseAction: () -> Unit,
//        val hasPermission: Boolean,
//        val onRequestPermission: () -> Unit,
//        override val onClose: () -> Unit = onCloseAction
//    ) : VoiceToContentUiState()
//
//    data class Recording(
//        override val headerText: Int,
//        val elapsedTime: String,
//        val onStopTap: () -> Unit,
//        val onCloseAction: () -> Unit,
//        override val onClose: () -> Unit = onCloseAction
//    ) : VoiceToContentUiState()
//
//    data class Processing(
//        override val headerText: Int,
//        val onCloseAction: () -> Unit,
//        override val onClose: () -> Unit = onCloseAction
//    ) : VoiceToContentUiState()
//
//    data class Finished(
//        override val headerText: Int,
//        val content: String, // todo: this is wrong
//        val onCloseAction: () -> Unit,
//        override val onClose: () -> Unit = onCloseAction
//    ) : VoiceToContentUiState()
//
//    data class Error(
//        override val headerText: Int,
//        val message: String, // todo: this is wrong
//        val onCloseAction: () -> Unit,
//        override val onClose: () -> Unit = onCloseAction
//    ) : VoiceToContentUiState()
//}
