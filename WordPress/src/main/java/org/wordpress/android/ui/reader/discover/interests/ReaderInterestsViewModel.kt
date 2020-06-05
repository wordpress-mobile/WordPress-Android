package org.wordpress.android.ui.reader.discover.interests

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonDisabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonEnabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonHiddenUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentInitialUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadSuccessUiState
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor(
    private val readerTagRepository: ReaderTagRepository
) : ViewModel() {
    var initialized: Boolean = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData(ContentInitialUiState)
    val uiState: LiveData<UiState> = _uiState

    private val _navigateToDiscover = MutableLiveData<Event<Unit>>()
    val navigateToDiscover: LiveData<Event<Unit>> = _navigateToDiscover

    private val selectedInterests = ReaderTagList()

    fun start() {
        if (initialized) {
            val uiState = uiState.value as ContentLoadSuccessUiState
            updateUiState(uiState.copy(interestTagsUiState = getInterestsUiStateWithSelectedStates()))
            return
        }
        loadInterests()
    }

    private fun loadInterests() {
        viewModelScope.launch {
            val tagList = readerTagRepository.getInterests()
            if (tagList.isNotEmpty()) {
                updateUiState(
                    ContentLoadSuccessUiState(
                        transformToInterestsUiState(tagList),
                        tagList,
                        getDoneButtonUiState()
                    )
                )
                if (!initialized) {
                    initialized = true
                }
            }
        }
    }

    fun onInterestAtIndexToggled(index: Int, isChecked: Boolean) {
        uiState.value?.let {
            val enableDoneButton = selectedInterests.isEmpty() && isChecked
            val disableDoneButton = selectedInterests.size == 1 && !isChecked

            updateSelectedInterests(it.interests[index])

            if (enableDoneButton || disableDoneButton) {
                val uiState = uiState.value as ContentLoadSuccessUiState
                updateUiState(
                    uiState.copy(
                        interestTagsUiState = getInterestsUiStateWithSelectedStates(),
                        doneBtnUiState = getDoneButtonUiState()
                    )
                )
            }
        }
    }

    fun onDoneButtonClick() {
        viewModelScope.launch {
            readerTagRepository.saveInterests(selectedInterests)
            _navigateToDiscover.value = Event(Unit)
        }
    }

    private fun updateSelectedInterests(interestAtIndex: ReaderTag?) {
        if (!selectedInterests.contains(interestAtIndex)) {
            selectedInterests.add(interestAtIndex)
        } else {
            selectedInterests.remove(interestAtIndex)
        }
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
        interests.map { interest ->
            InterestUiState(interest.tagTitle)
        }

    private fun getInterestsUiStateWithSelectedStates(): List<InterestUiState> {
        val uiState = uiState.value as UiState

        return uiState.interestsUiState.mapIndexed { index, interestUiState ->
            val interestAtIndex = uiState.interests[index]
            val isInterestAtIndexSelected = selectedInterests.contains(interestAtIndex)

            interestUiState.copy(isChecked = isInterestAtIndexSelected)
        }
    }

    private fun getDoneButtonUiState() = if (selectedInterests.isNotEmpty()) {
        DoneButtonEnabledUiState
    } else {
        DoneButtonDisabledUiState
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    sealed class UiState(
        val interestsUiState: List<InterestUiState>,
        val interests: ReaderTagList,
        val doneButtonUiState: DoneButtonUiState,
        val progressBarVisible: Boolean = false,
        val titleVisible: Boolean = !progressBarVisible,
        val subtitleVisible: Boolean = !progressBarVisible
    ) {
        object ContentInitialUiState : UiState(
            interestsUiState = emptyList(),
            interests = ReaderTagList(),
            doneButtonUiState = DoneButtonHiddenUiState,
            progressBarVisible = true
        )

        data class ContentLoadSuccessUiState(
            val interestTagsUiState: List<InterestUiState>,
            val interestTags: ReaderTagList,
            val doneBtnUiState: DoneButtonUiState
        ) : UiState(
            interestsUiState = interestTagsUiState,
            interests = interestTags,
            doneButtonUiState = doneBtnUiState,
            progressBarVisible = false
        )
    }

    data class InterestUiState(
        val title: String,
        val isChecked: Boolean = false
    )

    sealed class DoneButtonUiState(
        @StringRes val titleRes: Int = R.string.reader_btn_done,
        val enabled: Boolean = false,
        val visible: Boolean = true
    ) {
        object DoneButtonEnabledUiState : DoneButtonUiState(
            titleRes = R.string.reader_btn_done,
            enabled = true
        )

        object DoneButtonDisabledUiState : DoneButtonUiState(
            titleRes = R.string.reader_btn_select_few_interests,
            enabled = false
        )

        object DoneButtonHiddenUiState : DoneButtonUiState(
            visible = false
        )
    }
}
