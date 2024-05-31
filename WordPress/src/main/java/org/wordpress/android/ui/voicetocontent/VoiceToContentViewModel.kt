package org.wordpress.android.ui.voicetocontent

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
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
    private val jetpackAIStore: JetpackAIStore,
    private val recordingUseCase: RecordingUseCase,
    private val contextProvider: ContextProvider
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<VoiceToContentResult>()
    val uiState = _uiState as LiveData<VoiceToContentResult>

    private val _aiAssistantFeatureState = MutableLiveData<JetpackAIAssistantFeature>()
    val aiAssistantFeatureState = _aiAssistantFeatureState as LiveData<JetpackAIAssistantFeature>

    private val _requestPermission = MutableLiveData<Unit>()
    val requestPermission = _requestPermission as LiveData<Unit>

    private val _state = MutableStateFlow<VoiceToContentUiState>(VoiceToContentUiState.Initializing(
        headerText = R.string.voice_to_content_initializing,
        labelText = R.string.voice_to_content_preparing,
        onCloseAction = ::onClose
    ))
    val state: StateFlow<VoiceToContentUiState> = _state.asStateFlow()

    private fun isVoiceToContentEnabled() = voiceToContentFeatureUtils.isVoiceToContentEnabled()

    init {
        observeRecordingUpdates()
        // Simulate initialization delay
        viewModelScope.launch {
            delay(2000)
            _state.value = VoiceToContentUiState.ReadyToRecord(
                headerText = R.string.voice_to_content_ready_to_record,
                labelText = R.string.voice_to_content_ready_to_record_label,
                subLabelText = R.string.voice_to_content_tap_to_start,
                onMicTap = ::onMicTap,
                onCloseAction = ::onClose,
                onRequestPermission = ::onRequestPermission,
                hasPermission = hasAllPermissionsForRecording()
            )
        }
    }

    private fun onClose() {
        // Handle close
    }

    private fun onRequestPermission() {
        _requestPermission.postValue(Unit)
    }

    private fun onMicTap() {
        _state.value = VoiceToContentUiState.Recording(
            headerText = R.string.voice_to_content_recording,
            elapsedTime = "0 sec",
            onStopTap = ::onStopTap,
            onCloseAction = ::onClose
        )
        // Simulate recording time
        viewModelScope.launch {
            for (i in 1..60) {
                delay(1000)
                _state.value = (state.value as? VoiceToContentUiState.Recording)?.copy(
                    elapsedTime = "$i sec"
                ) ?: return@launch
            }
            _state.value = VoiceToContentUiState.Processing(
                headerText = R.string.voice_to_content_processing,
                onCloseAction = ::onClose
            )
            // Simulate processing delay
            delay(2000)
            // Handle recording complete
        }
    }

    private fun onStopTap() {
        // Handle stop recording
    }

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

    fun showStartRecordingView() {
        onMicTap()
    }

    fun startRecording() {
        recordingUseCase.startRecording { audioRecorderResult ->
            when (audioRecorderResult) {
                is Success -> {
                    val file = getRecordingFile(audioRecorderResult.recordingPath)
                    file?.let {
                        executeVoiceToContent(it)
                    } ?: run {
                        _uiState.postValue(VoiceToContentResult(isError = true))
                    }
                }
                is Error -> {
                    _uiState.postValue(VoiceToContentResult(isError = true))
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

    fun stopRecording() {
      recordingUseCase.stopRecording()
    }

    fun executeVoiceToContent(file: File) {
        val site = selectedSiteRepository.getSelectedSite() ?: run {
            _uiState.postValue(VoiceToContentResult(isError = true))
            return
        }

        if (isVoiceToContentEnabled()) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = jetpackAIStore.fetchJetpackAIAssistantFeature(site)
                when (result) {
                    is JetpackAIAssistantFeatureResponse.Success -> {
                        _aiAssistantFeatureState.postValue(result.model)
                        startVoiceToContentFlow(site, file)
                    }
                    is JetpackAIAssistantFeatureResponse.Error -> {
                        _uiState.postValue(VoiceToContentResult(isError = true))
                    }
                }
            }
        }
    }

    private fun startVoiceToContentFlow(site: SiteModel, file: File) {
        if (isVoiceToContentEnabled()) {
            viewModelScope.launch {
                val result = voiceToContentUseCase.execute(site, file)
                _uiState.postValue(result)
            }
        }
    }

    private fun hasAllPermissionsForRecording(): Boolean {
        return IAudioRecorder.REQUIRED_RECORDING_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                contextProvider.getContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

