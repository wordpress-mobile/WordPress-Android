package org.wordpress.android.ui.voicetocontent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
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
    private val selectedSiteRepository: SelectedSiteRepository
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<VoiceToContentResult>()
    val uiState = _uiState as LiveData<VoiceToContentResult>

    private fun isVoiceToContentEnabled() = voiceToContentFeatureUtils.isVoiceToContentEnabled()

    fun execute() {
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
