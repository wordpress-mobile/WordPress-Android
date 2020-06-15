package org.wordpress.android.ui.reader.discover.interests

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
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

    fun start() {
        if (initialized) return
        loadInterests()
    }

    private fun loadInterests() {
        viewModelScope.launch {
            val tagList = readerTagRepository.getInterests()
            if (tagList.isNotEmpty()) {
                val currentUiState = uiState.value as UiState
                updateUiState(
                   ContentLoadSuccessUiState(
                        interestsUiState = transformToInterestsUiState(tagList),
                        interests = tagList,
                        doneButtonUiState = currentUiState.getDoneButtonState()
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
            val currentUiState = uiState.value as ContentLoadSuccessUiState
            val updatedInterestsUiState = getUpdatedInterestsUiState(index, isChecked)

            updateUiState(
                uiState.copy(
                    interestsUiState = updatedInterestsUiState,
                    doneButtonUiState = currentUiState.getDoneButtonState(isInterestChecked = isChecked)
                )
            )
        }
    }

    fun onDoneButtonClick() {
        viewModelScope.launch {
            val currentUiState = uiState.value as UiState
            readerTagRepository.saveInterests(currentUiState.getSelectedInterests())
            _navigateToDiscover.value = Event(Unit)
        }
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
        interests.map { interest ->
            InterestUiState(interest.tagTitle)
        }

    private fun getUpdatedInterestsUiState(index: Int, isChecked: Boolean): List<InterestUiState> {
        val currentUiState = uiState.value as UiState
        val newInterestsUiState = currentUiState.interestsUiState.toMutableList()
        newInterestsUiState[index] = currentUiState.interestsUiState[index].copy(isChecked = isChecked)
        return newInterestsUiState
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    sealed class UiState(
        val interestsUiState: List<InterestUiState>,
        val interests: ReaderTagList,
        val doneButtonUiState: DoneButtonUiState
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
            val doneBtnUiState: DoneButtonUiState,
            val progressBarVisible: Boolean = false,
            val titleVisible: Boolean = !progressBarVisible,
            val subtitleVisible: Boolean = !progressBarVisible
        ) : UiState(
            interestsUiState = interestTagsUiState,
            interests = interestTags,
            doneButtonUiState = doneBtnUiState,
            progressBarVisible = false
        )

        private val checkedInterestsUiState = interestsUiState.filter { it.isChecked }

        fun getSelectedInterests() = interests.filter {
            checkedInterestsUiState.map {
                checkedInterestUiState -> checkedInterestUiState.title
            }.contains(it.tagTitle)
        }

        fun getDoneButtonState(
            isInterestChecked: Boolean = false
        ): DoneButtonUiState {
            val disableDoneButton = interests.isEmpty() || (checkedInterestsUiState.size == 1 && !isInterestChecked)
            return if (disableDoneButton) {
                DoneButtonDisabledUiState
            } else {
                DoneButtonEnabledUiState
            }
        }
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
