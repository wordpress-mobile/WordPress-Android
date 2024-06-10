package org.wordpress.android.ui.voicetocontent

import android.content.pm.PackageManager
import android.util.Log
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
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.audio.IAudioRecorder
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.INITIALIZING
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.READY_TO_RECORD
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.RECORDING
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.ERROR
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.INELIGIBLE_FOR_FEATURE
import org.wordpress.android.ui.voicetocontent.VoiceToContentUIStateType.PROCESSING

import java.io.File
import javax.inject.Inject
import javax.inject.Named
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult.Success
import org.wordpress.android.util.audio.IAudioRecorder.AudioRecorderResult.Error

@HiltViewModel
class VoiceToContentViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val voiceToContentFeatureUtils: VoiceToContentFeatureUtils,
    private val voiceToContentUseCase: VoiceToContentUseCase,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val recordingUseCase: RecordingUseCase,
    private val contextProvider: ContextProvider,
    private val prepareVoiceToContentUseCase: PrepareVoiceToContentUseCase
) : ScopedViewModel(mainDispatcher) {
    private val _requestPermission = MutableLiveData<Unit>()
    val requestPermission = _requestPermission as LiveData<Unit>

    private val _dismiss = MutableLiveData<Unit>()
    val dismiss = _dismiss as LiveData<Unit>

    private val _amplitudes = MutableLiveData<List<Float>>()
    val amplitudes: LiveData<List<Float>> get() = _amplitudes

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

        viewModelScope.launch {
            when (val result = prepareVoiceToContentUseCase.execute(site)) {
                is PrepareVoiceToContentResult.Success -> {
                    transitionToReadyToRecordOrIneligibleForFeature(result.model)
                }

                is PrepareVoiceToContentResult.Error -> {
                    transitionToError()
                }
            }
        }
    }

    // Recording
    // todo: This doesn't work as expected
    @Suppress("MagicNumber")
    private fun updateAmplitudes(newAmplitudes: List<Float>) {
        _amplitudes.value = listOf(1.1f, 2.2f, 4.4f, 3.2f, 1.1f, 2.2f, 1.0f, 3.5f)
        Log.d(javaClass.simpleName, "Update amplitudes: $newAmplitudes")
    }

    private fun observeRecordingUpdates() {
        viewModelScope.launch {
            recordingUseCase.recordingUpdates().collect { update ->
                if (update.fileSizeLimitExceeded) {
                    stopRecording()
                } else {
                    updateAmplitudes(update.amplitudes)
                    // todo: Handle other updates if needed when UI is ready, e.g., elapsed time and file size
                    Log.d("AudioRecorder", "Recording update: $update")
                }
            }
        }
    }

    private fun startRecording() {
        transitionToRecording()
        recordingUseCase.startRecording { audioRecorderResult ->
            when (audioRecorderResult) {
                is Success -> {
                    val file = getRecordingFile(audioRecorderResult.recordingPath)
                    file?.let {
                        executeVoiceToContent(it)
                    } ?: run {
                       transitionToError()
                    }
                }
                is Error -> {
                   transitionToError()
                }
            }
        }
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

    // Workflow
    private fun executeVoiceToContent(file: File) {
        val site = selectedSiteRepository.getSelectedSite() ?: run {
            transitionToError()
            return
        }

        viewModelScope.launch {
            val result = voiceToContentUseCase.execute(site, file)
            Log.i(javaClass.simpleName, "***=> result is ${result.content}")
            _dismiss.postValue(Unit)
        }
    }

    // Permissions
    private fun onRequestPermission() {
        _requestPermission.postValue(Unit)
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
        startRecording()
    }

    private fun onStopTap() {
        stopRecording()
    }

    private fun onClose() {
        _dismiss.postValue(Unit)
    }

    // transitions
    private fun transitionToReadyToRecordOrIneligibleForFeature(model: JetpackAIAssistantFeature) {
        val isEligibleForFeature = voiceToContentFeatureUtils.isEligibleForVoiceToContent(model)
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
                upgradeUrl = model.upgradeUrl
            )
        )
    }

    private fun transitionToRecording() {
        val currentState = _state.value
        _state.value = currentState.copy(
            uiStateType = RECORDING,
            header = currentState.header.copy(label = R.string.voice_to_content_recording_label),
            secondaryHeader = currentState.secondaryHeader?.copy(
                timeElapsed = "00:00:00",
                isTimeElapsedVisible = true
            ),
            recordingPanel = currentState.recordingPanel?.copy(
                onStopTap = ::onStopTap,
                hasPermission = true,
                actionLabel = R.string.voice_to_content_done_label)
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

    // todo: annmarie - transition to error hasn't been fully fleshed out
    private fun transitionToError() {
        val currentState = _state.value
        _state.value = currentState.copy(
            uiStateType = ERROR,
            header = currentState.header.copy( label = R.string.voice_to_content_ready_to_record),
            secondaryHeader = null,
            recordingPanel = null
        )
    }
}

