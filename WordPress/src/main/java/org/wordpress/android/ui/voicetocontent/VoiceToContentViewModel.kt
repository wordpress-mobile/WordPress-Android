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

    private val _state = MutableStateFlow<VoiceToContentUiState>(VoiceToContentUiState.Initializing(
        header = R.string.voice_to_content_initializing,
        labelText = R.string.voice_to_content_preparing,
        onCloseAction = ::onClose
    ))
    val state: StateFlow<VoiceToContentUiState> = _state.asStateFlow()

    private fun isVoiceToContentEnabled() = voiceToContentFeatureUtils.isVoiceToContentEnabled()

    init {
        observeRecordingUpdates()
    }

    fun start() {
        val site = selectedSiteRepository.getSelectedSite() ?: run {
            transitionToError()
            return
        }
        if (isVoiceToContentEnabled()) {
            viewModelScope.launch {
                when (val result = prepareVoiceToContentUseCase.execute(site)) {
                    is PrepareVoiceToContentResult.Success -> {
                        transitionToReadyToRecord(result.model)
                    }

                    is PrepareVoiceToContentResult.Error -> {
                        transitionToError()
                    }
                }
            }
        }
    }

    // Recording
    private fun observeRecordingUpdates() {
        viewModelScope.launch {
            recordingUseCase.recordingUpdates().collect { update ->
                if (update.fileSizeLimitExceeded) {
                    stopRecording()
                } else {
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
            transitionToFinished(result.content)
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
    private fun transitionToReadyToRecord(model: JetpackAIAssistantFeature) {
        // todo: annmarie- put together the proper labels; especially the requests available count
        _state.value = VoiceToContentUiState.ReadyToRecord(
            header = R.string.voice_to_content_ready_to_record,
            labelText = R.string.voice_to_content_ready_to_record_label,
            subLabelText = R.string.voice_to_content_tap_to_start,
            requestsAvailable = voiceToContentFeatureUtils.getRequestLimit(model),
            isEligibleForFeature = voiceToContentFeatureUtils.isEligibleForVoiceToContent(model),
            onMicTap = ::onMicTap,
            onCloseAction = ::onClose,
            onRequestPermission = ::onRequestPermission,
            hasPermission = hasAllPermissionsForRecording()
        )
    }

    private fun transitionToRecording() {
        _state.value = VoiceToContentUiState.Recording(
            header = R.string.voice_to_content_recording,
            elapsedTime = "0 sec",
            onStopTap = ::onStopTap,
            onCloseAction = ::onClose
        )
    }

    private fun transitionToProcessing() {
        _state.value = VoiceToContentUiState.Processing(
            header = R.string.voice_to_content_processing,
            onCloseAction = ::onClose
        )
    }

    // todo: annmarie - transition to error hasn't been fully fleshed out ... some errors will be shown on top of
    // the existing screen
    private fun transitionToError() {
        _state.value = VoiceToContentUiState.Error(
            header = R.string.voice_to_content_ready_to_record,
            message = "Something bad happened and we can't continue",
            onCloseAction = ::onClose
        )
    }
    // todo: annmarie - transition to finished MUST be removed, as we are pushing the user to editPostActivity
    private fun transitionToFinished(content: String?) {
        _state.value = VoiceToContentUiState.Finished(
            header = R.string.voice_to_content_finished_label,
            content = content ?: "",
            onCloseAction = ::onClose
        )
    }
}

