package org.wordpress.android.ui.voicetocontent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class VoiceToContentViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val voiceToContentFeatureUtils: VoiceToContentFeatureUtils,
    private val voiceToContentUseCase: VoiceToContentUseCase,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val jetpackAIStore: JetpackAIStore
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<VoiceToContentResult>()
    val uiState = _uiState as LiveData<VoiceToContentResult>

    private val _aiAssistantFeatureState = MutableLiveData<JetpackAIAssistantFeature>()
    val aiAssistantFeatureState = _aiAssistantFeatureState as LiveData<JetpackAIAssistantFeature>

    private fun isVoiceToContentEnabled() = voiceToContentFeatureUtils.isVoiceToContentEnabled()

    fun execute() {
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
                        startVoiceToContentFlow()
                    }
                    is JetpackAIAssistantFeatureResponse.Error -> {
                        _uiState.postValue(VoiceToContentResult(isError = true))
                    }
                }
            }
        }
    }

    private fun startVoiceToContentFlow() {
        val site = selectedSiteRepository.getSelectedSite() ?: run {
            _uiState.postValue(VoiceToContentResult(isError = true))
            return
        }

        if (isVoiceToContentEnabled()) {
            viewModelScope.launch {
                val result = voiceToContentUseCase.execute(site)
                _uiState.postValue(result)
            }
        }
    }
}
