package org.wordpress.android.ui.voicetocontent

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
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
    private val jetpackAIStore: JetpackAIStore,
    private val recordingUseCase: RecordingUseCase
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<VoiceToContentResult>()
    val uiState = _uiState as LiveData<VoiceToContentResult>

    private val _aiAssistantFeatureState = MutableLiveData<JetpackAIAssistantFeature>()
    val aiAssistantFeatureState = _aiAssistantFeatureState as LiveData<JetpackAIAssistantFeature>

    private fun isVoiceToContentEnabled() = voiceToContentFeatureUtils.isVoiceToContentEnabled()

    init {
        observeRecordingUpdates()
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

    fun startRecording() {
        recordingUseCase.startRecording { recordingPath ->
            val file = getRecordingFile(recordingPath)
            file?.let {
                executeVoiceToContent(it)
            } ?: run {
                _uiState.postValue(VoiceToContentResult(isError = true))
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
}

