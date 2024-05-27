package org.wordpress.android.ui.voicetocontent

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
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
    private val recordingUseCase: RecordingUseCase
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<VoiceToContentResult>()
    val uiState = _uiState as LiveData<VoiceToContentResult>

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
                    // Handle other updates if needed, e.g., elapsed time and file size
                    Log.d("AudioRecorder", "Recording update: $update")
                }
            }
        }
    }

    fun startRecording() {
        recordingUseCase.startRecording()
    }

    fun stopRecording() {
        viewModelScope.launch {
            val file = recordingUseCase.stopRecording()
            file?.let {
                executeVoiceToContent(it)
            } ?: run {
                _uiState.postValue(VoiceToContentResult(isError = true))
            }
        }
    }

    fun executeVoiceToContent(file: File) {
        val site = selectedSiteRepository.getSelectedSite() ?: run {
            _uiState.postValue(VoiceToContentResult(isError = true))
            return
        }

        if (isVoiceToContentEnabled()) {
            viewModelScope.launch {
                val result = voiceToContentUseCase.execute(site, file)
                _uiState.postValue(result)
            }
        }
    }
}
