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
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor(
    private val readerTagRepository: ReaderTagRepository
) : ViewModel() {
    private var isStarted = false
    private lateinit var parentViewModel: ReaderViewModel

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start(parentViewModel: ReaderViewModel) {
        this.parentViewModel = parentViewModel
        if (isStarted) {
            return
        }
        loadUserTags()
        isStarted = true
    }

    private fun loadUserTags() {
        updateUiState(LoadingUiState)
        viewModelScope.launch {
            val userTags = readerTagRepository.getUserTags() // TODO: error handling
            if (userTags.isEmpty()) {
                loadInterests()
            } else {
                parentViewModel.onCloseReaderInterests()
            }
        }
    }

    private fun loadInterests() {
        updateUiState(LoadingUiState)
        viewModelScope.launch {
            val tagList = readerTagRepository.getInterests() // TODO: error handling
            updateUiState(
                ContentUiState(
                    interestsUiState = transformToInterestsUiState(tagList),
                    interests = tagList
                )
            )
        }
    }

    fun onInterestAtIndexToggled(index: Int, isChecked: Boolean) {
        uiState.value?.let {
            val currentUiState = uiState.value as ContentUiState
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
            parentViewModel.onCloseReaderInterests()
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
        val currentUiState = uiState.value as ContentUiState
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
        val errorLayoutVisible: Boolean = false
    ) {
        object LoadingUiState : UiState(
            progressBarVisible = true
        )

        data class ContentUiState(
            val interestsUiState: List<InterestUiState>,
            val interests: ReaderTagList,
            override val doneButtonUiState: DoneButtonUiState = DoneButtonDisabledUiState
        ) : UiState(
            progressBarVisible = false,
            titleVisible = true,
            subtitleVisible = true,
            errorLayoutVisible = false
        )

        sealed class ErrorUiState constructor(
            val titleResId: Int,
            val subtitleResId: Int? = null,
            val showContactSupport: Boolean = false
        ) : UiState(
            progressBarVisible = false,
            errorLayoutVisible = true
        ) {
            object ConnectionErrorUiState : ErrorUiState(
                titleResId = R.string.no_network_message
            )
        }

        private fun getCheckedInterestsUiState(): List<InterestUiState> {
            return if (this is ContentUiState) {
                interestsUiState.filter { it.isChecked }
            } else {
                emptyList()
            }
        }

        fun getSelectedInterests(): List<ReaderTag> {
            return if (this is ContentUiState) {
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
            return if (this is ContentUiState) {
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
