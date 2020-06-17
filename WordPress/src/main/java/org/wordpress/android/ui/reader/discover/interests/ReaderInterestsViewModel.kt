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
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadSuccessUiState
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor(
    private val readerTagRepository: ReaderTagRepository
) : ViewModel() {
    private var isStarted = false

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _navigateToDiscover = MutableLiveData<Event<Unit>>()
    val navigateToDiscover: LiveData<Event<Unit>> = _navigateToDiscover

    fun start() {
        if (isStarted) {
            return
        }
        loadInterests()
        isStarted = true
    }

    private fun loadInterests() {
        updateUiState(LoadingUiState)
        viewModelScope.launch {
            val tagList = readerTagRepository.getInterests()
            updateUiState(
                ContentLoadSuccessUiState(
                    interestsUiState = transformToInterestsUiState(tagList),
                    interests = tagList
                )
            )
        }
    }

    fun onInterestAtIndexToggled(index: Int, isChecked: Boolean) {
        uiState.value?.let {
            val currentUiState = uiState.value as ContentLoadSuccessUiState
            val updatedInterestsUiState = getUpdatedInterestsUiState(index, isChecked)

            updateUiState(
                currentUiState.copy(
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

    fun onRetryButtonClick() {
        loadInterests()
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
        interests.map { interest ->
            InterestUiState(interest.tagTitle)
        }

    private fun getUpdatedInterestsUiState(index: Int, isChecked: Boolean): List<InterestUiState> {
        val currentUiState = uiState.value as ContentLoadSuccessUiState
        val newInterestsUiState = currentUiState.interestsUiState.toMutableList()
        newInterestsUiState[index] = currentUiState.interestsUiState[index].copy(isChecked = isChecked)
        return newInterestsUiState
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    sealed class UiState(
        open val doneButtonUiState: DoneButtonUiState = DoneButtonHiddenUiState,
        val progressBarVisible: Boolean = false,
        val titleVisible: Boolean = false,
        val subtitleVisible: Boolean = false,
        val fullscreenErrorLayoutVisible: Boolean = false
    ) {
        object LoadingUiState : UiState(
            progressBarVisible = true
        )

        data class ContentLoadSuccessUiState(
            val interestsUiState: List<InterestUiState>,
            val interests: ReaderTagList,
            override val doneButtonUiState: DoneButtonUiState = DoneButtonDisabledUiState
        ) : UiState(
            progressBarVisible = false,
            titleVisible = true,
            subtitleVisible = true,
            fullscreenErrorLayoutVisible = false
        )

        sealed class ContentLoadFailedUiState constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null,
            val showContactSupport: Boolean = false
        ) : UiState(
            progressBarVisible = false,
            fullscreenErrorLayoutVisible = true
        ) {
            object ContentLoadFailedConnectionErrorUiState : ContentLoadFailedUiState(
                titleResId = R.string.no_network_message
            )
        }

        private fun getCheckedInterestsUiState(): List<InterestUiState> {
            return if (this is ContentLoadSuccessUiState) {
                interestsUiState.filter { it.isChecked }
            } else {
                emptyList()
            }
        }

        fun getSelectedInterests(): List<ReaderTag> {
            return if (this is ContentLoadSuccessUiState) {
                interests.filter {
                    getCheckedInterestsUiState().map { checkedInterestUiState ->
                        checkedInterestUiState.title
                    }.contains(it.tagTitle)
                }
            } else {
                emptyList()
            }
        }

        fun getDoneButtonState(
            isInterestChecked: Boolean = false
        ): DoneButtonUiState {
            return if (this is ContentLoadSuccessUiState) {
                val disableDoneButton = interests.isEmpty() ||
                    (getCheckedInterestsUiState().size == 1 && !isInterestChecked)
                if (disableDoneButton) {
                    DoneButtonDisabledUiState
                } else {
                    DoneButtonEnabledUiState
                }
            } else {
                DoneButtonHiddenUiState
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
