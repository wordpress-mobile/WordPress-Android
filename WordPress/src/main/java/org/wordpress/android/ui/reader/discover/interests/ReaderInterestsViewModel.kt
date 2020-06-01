package org.wordpress.android.ui.reader.discover.interests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType.DEFAULT
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor() : ViewModel() {
    private var initialized: Boolean = false

    private val _uiState = MutableLiveData<ReaderInterestsUiState>()
    val uiState: LiveData<ReaderInterestsUiState> = _uiState

    fun start() {
        if (initialized) return
        loadInterests()
    }

    private fun loadInterests() {
        // TODO: get list from tags repository once available
        val tagList = getMockInterests()
        if (tagList.isNotEmpty()) {
            _uiState.value = ReaderInterestsUiState(tagList)
            if (!initialized) {
                initialized = true
            }
        }
    }

    data class ReaderInterestsUiState(
        val interests: ReaderTagList
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
