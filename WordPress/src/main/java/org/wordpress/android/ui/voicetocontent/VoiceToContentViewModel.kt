package org.wordpress.android.ui.voicetocontent

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.voicetocontent.VoiceToContentActionEvent.Dismiss
import org.wordpress.android.ui.voicetocontent.VoiceToContentActionEvent.LaunchEditPost
import org.wordpress.android.ui.voicetocontent.VoiceToContentActionEvent.LaunchExternalBrowser
import org.wordpress.android.ui.voicetocontent.VoiceToContentActionEvent.RequestPermission
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.ERROR
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.INELIGIBLE_FOR_FEATURE
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.INITIALIZING
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.PROCESSING
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.READY_TO_RECORD
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.RECORDING
import org.wordpress.android.util.audio.IAudioRecorder
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult.Error
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult.Success
import org.wordpress.android.util.audio.RecordingStrategy
import org.wordpress.android.util.audio.RecordingUpdate
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class VoiceToContentViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val voiceToContentFeatureUtils: VoiceToContentFeatureUtils,
    private val voiceToContentUseCase: VoiceToContentUseCase,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val recordingUseCase: RecordingUseCase,
    private val contextProvider: ContextProvider,
    private val prepareVoiceToContentUseCase: PrepareVoiceToContentUseCase,
    private val logger: VoiceToContentTelemetry
) : ScopedViewModel(mainDispatcher) {
    private val _recordingUpdate = MutableLiveData<RecordingUpdate>()
    val recordingUpdate = _recordingUpdate as LiveData<RecordingUpdate>

    private val _isCancelableOutsideTouch = MutableLiveData(true)
    val isCancelableOutsideTouch = _isCancelableOutsideTouch as LiveData<Boolean>

    private val _actionEvent = MutableLiveData<VoiceToContentActionEvent>()
    val actionEvent = _actionEvent as LiveData<VoiceToContentActionEvent>

    val isRecording: StateFlow<Boolean> = recordingUseCase.isRecording()
    val isPaused: StateFlow<Boolean> = recordingUseCase.isPaused()

    private var isStarted = false

    private val _state = MutableStateFlow(VoiceToContentUiState(
        uiStateType = INITIALIZING,
        header = HeaderUIModel(
            label = R.string.voice_to_content_base_header_label,
            onClose = ::onClose),
        secondaryHeader = SecondaryHeaderUIModel(
            label = R.string.voice_to_content_secondary_header_label,
            isLabelVisible = true,
            isProgressIndicatorVisible = true,
            isTimeElapsedVisible = false),
        recordingPanel = RecordingPanelUIModel(
            actionLabel = R.string.voice_to_content_begin_recording_label,
            isEnabled = false)
    ))
    val state: StateFlow<VoiceToContentUiState> = _state.asStateFlow()

    private fun isVoiceToContentEnabled() = voiceToContentFeatureUtils.isVoiceToContentEnabled()

    init {
        observeRecordingUpdates()
    }

    @Suppress("MagicNumber")
    fun start() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null || !isVoiceToContentEnabled()) return

        if (!isStarted) {
            logger.track(Stat.VOICE_TO_CONTENT_SHEET_SHOWN)
        }

        viewModelScope.launch {
            when (val result = prepareVoiceToContentUseCase.execute(site)) {
                is PrepareVoiceToContentResult.Success -> {
                    transitionToReadyToRecordOrIneligibleForFeature(result.model)
                }

                is PrepareVoiceToContentResult.Failure -> {
                    result.transitionToError()
                }
            }
        }

        isStarted = true
    }

    fun onBottomSheetClosed() {
        recordingUseCase.endRecordingSession()
    }

    // Recording
    private fun updateRecordingData(recordingUpdate: RecordingUpdate) {
        _recordingUpdate.value = recordingUpdate
    }

    private fun observeRecordingUpdates() {
        viewModelScope.launch {
            recordingUseCase.recordingUpdates().collect { update ->
                if (update.fileSizeLimitExceeded) {
                    stopRecording()
                } else {
                    updateRecordingData(update)
                }
            }
        }
    }

    private fun startRecording() {
        transitionToRecording()
        disableDialogCancelableOutsideTouch()
        recordingUseCase.startRecording { audioRecorderResult ->
            when (audioRecorderResult) {
                is Success -> {
                    transitionToProcessing()
                    val file = getRecordingFile(audioRecorderResult.recordingPath)
                    file?.let {
                        executeVoiceToContent(it)
                    } ?: run {
                        logger.logError("$VOICE_TO_CONTENT - unable to access audio file")
                        transitionToError(GenericFailureMsg)
                    }
                }
                is Error -> {
                   audioRecorderResult.transitionToError()
                }
            }
        }
    }

    private fun disableDialogCancelableOutsideTouch() {
        _isCancelableOutsideTouch.value = false
    }

    @Suppress("ReturnCount")
    private fun getRecordingFile(recordingPath: String): File? {
        if (recordingPath.isEmpty()) return null
        val recordingFile = File(recordingPath)
        // Return null if the file does not exist, is not a file, or is empty
        if (!recordingFile.exists() || !recordingFile.isFile || recordingFile.length() == 0L) return null
        return recordingFile
    }

    private fun stopRecording() {
        transitionToProcessing()
        recordingUseCase.stopRecording()
    }

    fun pauseRecording() {
        recordingUseCase.pauseRecording()
    }

    fun resumeRecording() {
        recordingUseCase.resumeRecording()
    }

    // Workflow
    private fun executeVoiceToContent(file: File) {
        val site = selectedSiteRepository.getSelectedSite() ?: run {
            transitionToError(GenericFailureMsg)
            return
        }

        viewModelScope.launch {
            when (val result = voiceToContentUseCase.execute(site, file)) {
                is VoiceToContentResult.Failure -> result.transitionToError()
                is VoiceToContentResult.Success -> {
                    _actionEvent.postValue(LaunchEditPost(site, result.content))
                }
            }
        }
    }

    // Permissions
    private fun onRequestPermission() {
        logger.track(Stat.VOICE_TO_CONTENT_BUTTON_START_RECORDING_TAPPED)
        _actionEvent.postValue(RequestPermission)
    }

    private fun hasAllPermissionsForRecording(): Boolean {
        return IAudioRecorder.REQUIRED_RECORDING_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                contextProvider.getContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun onPermissionGranted() {
        startRecording()
    }

    // user actions
    private fun onMicTap() {
        logger.track(Stat.VOICE_TO_CONTENT_BUTTON_START_RECORDING_TAPPED)
        startRecording()
    }

    private fun onStopTap() {
        logger.track(Stat.VOICE_TO_CONTENT_BUTTON_DONE_TAPPED)
        stopRecording()
    }

    private fun onClose() {
        logger.track(Stat.VOICE_TO_CONTENT_BUTTON_CLOSE_TAPPED)
        recordingUseCase.endRecordingSession()
        _actionEvent.postValue(Dismiss)
    }

    private fun onRetryTap() {
        transitionToInitializing()
        start()
    }

    private fun onLinkTap(url: String?) {
        logger.track(Stat.VOICE_TO_CONTENT_BUTTON_UPGRADE_TAPPED)
        url?.let {
            _actionEvent.postValue(LaunchExternalBrowser(it))
        }
    }

    // transitions
    private fun transitionToInitializing() {
        _state.value = VoiceToContentUiState(
            uiStateType = INITIALIZING,
            header = HeaderUIModel(
                label = R.string.voice_to_content_base_header_label,
                onClose = ::onClose),
            secondaryHeader = SecondaryHeaderUIModel(
                label = R.string.voice_to_content_secondary_header_label,
                isLabelVisible = true,
                isProgressIndicatorVisible = true,
                isTimeElapsedVisible = false),
            recordingPanel = RecordingPanelUIModel(
                actionLabel = R.string.voice_to_content_begin_recording_label,
                isEnabled = false),
            errorPanel = null
        )
    }

    private fun transitionToReadyToRecordOrIneligibleForFeature(model: JetpackAIAssistantFeature) {
        val isEligibleForFeature = voiceToContentFeatureUtils.isEligibleForVoiceToContent(model)
        if (!isEligibleForFeature) {
            logger.track(Stat.VOICE_TO_CONTENT_BUTTON_RECORDING_LIMIT_REACHED)
        }
        val requestsAvailable = voiceToContentFeatureUtils.getRequestLimit(model)
        val currentState = _state.value
        _state.value = currentState.copy(
            uiStateType = if (isEligibleForFeature) READY_TO_RECORD else INELIGIBLE_FOR_FEATURE,
            secondaryHeader = currentState.secondaryHeader?.copy(
                requestsAvailable = requestsAvailable.toString(),
                isProgressIndicatorVisible = false
            ),
            recordingPanel = currentState.recordingPanel?.copy(
                isEnabled = isEligibleForFeature,
                isEligibleForFeature = isEligibleForFeature,
                onMicTap = ::onMicTap,
                onRequestPermission = ::onRequestPermission,
                hasPermission = hasAllPermissionsForRecording(),
                upgradeUrl = model.upgradeUrl,
                onLinkTap = ::onLinkTap
            )
        )
    }

    private fun transitionToRecording() {
        val currentState = _state.value
        _state.value = currentState.copy(
            uiStateType = RECORDING,
            header = currentState.header.copy(label = R.string.voice_to_content_recording_label),
            secondaryHeader = currentState.secondaryHeader?.copy(
                isTimeElapsedVisible = true,
                timeMaxDurationInSeconds = MAX_DURATION,
                isLabelVisible = false
            ),
            recordingPanel = currentState.recordingPanel?.copy(
                onStopTap = ::onStopTap,
                hasPermission = true,
                actionLabel = R.string.voice_to_content_done_label,
                onResumeRecording = ::resumeRecording,
                onPauseRecording = ::pauseRecording)
        )
    }

    private fun transitionToProcessing() {
        val currentState = _state.value
        _state.value = currentState.copy(
            uiStateType = PROCESSING,
            header = currentState.header.copy(label = R.string.voice_to_content_processing),
            secondaryHeader = null,
            recordingPanel = null
        )
    }

    private fun VoiceToContentResult.Failure.transitionToError() {
        when (this) {
            VoiceToContentResult.Failure.NetworkUnavailable -> transitionToError(NetworkUnavailableMsg, true)
            VoiceToContentResult.Failure.RemoteRequestFailure -> transitionToError(GenericFailureMsg)
        }
    }

    private fun PrepareVoiceToContentResult.Failure.transitionToError() {
        when (this) {
            PrepareVoiceToContentResult.Failure.NetworkUnavailable -> transitionToError(NetworkUnavailableMsg, true)
            PrepareVoiceToContentResult.Failure.RemoteRequestFailure -> transitionToError(GenericFailureMsg)
        }
    }

    private fun Error.transitionToError() {
        logger.logError("$VOICE_TO_CONTENT - ${this.errorMessage}")
        transitionToError(GenericFailureMsg)
    }

    private fun transitionToError(errorMessage: Int, allowRetry: Boolean = false) {
        val currentState = _state.value
        _state.value = currentState.copy(
            uiStateType = ERROR,
            header = currentState.header.copy( label = R.string.voice_to_content_error_label),
            secondaryHeader = null,
            recordingPanel = null,
            errorPanel = ErrorUiModel(errorMessage = errorMessage, allowRetry = allowRetry, onRetryTap = ::onRetryTap)
        )
    }

    companion object {
        private val NetworkUnavailableMsg = R.string.error_network_connection
        private val GenericFailureMsg = R.string.voice_to_content_generic_error
        private const val VOICE_TO_CONTENT = "Voice to content"
        private val MAX_DURATION = RecordingStrategy.VoiceToContentRecordingStrategy().maxDuration
    }
}

