package org.wordpress.android.ui.voicetocontent

sealed class VoiceToContentUiState {
    abstract val headerText: Int
    abstract val onClose: () -> Unit

    data class Initializing(
        override val headerText: Int,
        val labelText: Int,
        val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState()

    data class ReadyToRecord(
        override val headerText: Int,
        val labelText: Int,
        val subLabelText: Int,
        val onMicTap: () -> Unit,
        val onCloseAction: () -> Unit,
        val hasPermission: Boolean,
        val onRequestPermission: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState()

    data class Recording(
        override val headerText: Int,
        val elapsedTime: String,
        val onStopTap: () -> Unit,
        val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState()

    data class Processing(
        override val headerText: Int,
        val onCloseAction: () -> Unit,
        override val onClose: () -> Unit = onCloseAction
    ) : VoiceToContentUiState()
}

