package org.wordpress.android.ui.reader.discover.interests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderInterestsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val readerTagRepository: ReaderTagRepository
) : ScopedViewModel(mainDispatcher) {
    var initialized: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        if (initialized) return
        loadInterests()
    }

    private fun loadInterests() {
        launch {
            val tagList = readerTagRepository.getInterests()
            if (tagList.isNotEmpty()) {
                updateUiState(UiState(transformToInterestsUiState(tagList), tagList))
                if (!initialized) {
                    initialized = true
                }
            }
        }
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
        interests.map { interest ->
            InterestUiState(
                interest.tagTitle
            )
        }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    data class UiState(
        val interestsUiState: List<InterestUiState>,
        val interests: ReaderTagList
    )

    data class InterestUiState(
        val title: String,
        val isChecked: Boolean = false
    )
}
