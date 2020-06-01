package org.wordpress.android.ui.reader.discover.interests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor(
    private val readerTagRepository: ReaderTagRepository
) : ViewModel() {
    var initialized: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        if (initialized) return
        loadInterests()
    }

    private fun loadInterests() {
        viewModelScope.launch { // TODO: Might want to use ScopedViewModel with mainDispatcher for consistency
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
