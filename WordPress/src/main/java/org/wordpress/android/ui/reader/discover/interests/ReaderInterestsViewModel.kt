package org.wordpress.android.ui.reader.discover.interests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType.DEFAULT
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor() : ViewModel() {
    var initialized: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        if (initialized) return
        loadInterests()
    }

    private fun loadInterests() {
        // TODO: get list from tags repository once available
        val tagList = getMockInterests()
        if (tagList.isNotEmpty()) {
            updateUiState(UiState(transformToInterestsUiState(tagList), tagList))
            if (!initialized) {
                initialized = true
            }
        }
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
            interests.mapIndexed { index, interestTag -> // TODO: use index to know checked status
                InterestUiState(
                    interestTag.tagTitle
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

    private fun getMockInterests() =
            ReaderTagList().apply {
                for (c in 'A'..'Z')
                    (add(
                            ReaderTag(
                                    c.toString(), c.toString(), c.toString(),
                                    "https://public-api.wordpress.com/rest/v1.2/read/tags/$c/posts",
                                    DEFAULT
                            )
                    ))
            }
}
