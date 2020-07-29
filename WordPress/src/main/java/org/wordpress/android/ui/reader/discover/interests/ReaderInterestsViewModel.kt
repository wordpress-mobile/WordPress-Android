package org.wordpress.android.ui.reader.discover.interests

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonDisabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonEnabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonHiddenUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.ConnectionErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.RequestFailedErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialLoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor(
    private val readerTagRepository: ReaderTagRepository
) : ViewModel() {
    private var isStarted = false
    private lateinit var currentLanguage: String
    private lateinit var parentViewModel: ReaderViewModel

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private var userTagsFetchedSuccessfully = false

    fun start(parentViewModel: ReaderViewModel, currentLanguage: String) {
        this.parentViewModel = parentViewModel
        if (isStarted && isLanguageSame(currentLanguage)) {
            return
        }
        loadUserTags()
        this.currentLanguage = currentLanguage
        isStarted = true
    }

    private fun isLanguageSame(currentLanguage: String) = this.currentLanguage == currentLanguage

    private fun loadUserTags() {
        updateUiState(InitialLoadingUiState)
        viewModelScope.launch {
            when (val result = readerTagRepository.getUserTags()) {
                is SuccessWithData<*> -> {
                    userTagsFetchedSuccessfully = true
                    val userTags = result.data as ReaderTagList
                    if (userTags.isEmpty()) {
                        loadInterests()
                    } else {
                        parentViewModel.onCloseReaderInterests()
                    }
                }
                is Error -> {
                    if (result is NetworkUnavailable) {
                        updateUiState(ConnectionErrorUiState)
                    } else if (result is RemoteRequestFailure) {
                        updateUiState(RequestFailedErrorUiState)
                    }
                }
            }
        }
    }

    private fun loadInterests() {
        updateUiState(InitialLoadingUiState)
        viewModelScope.launch {
            val newUiState: UiState? = when (val result = readerTagRepository.getInterests()) {
                is SuccessWithData<*> -> {
                    val tags = result.data as ReaderTagList
                    val distinctTags = ReaderTagList().apply { addAll(tags.distinctBy { it.tagSlug }) }
                    ContentUiState(
                        interestsUiState = transformToInterestsUiState(distinctTags),
                        interests = distinctTags
                    )
                }
                is NetworkUnavailable -> {
                    ConnectionErrorUiState
                }
                is RemoteRequestFailure -> {
                    RequestFailedErrorUiState
                }
                else -> {
                    null
                }
            }

            newUiState?.let {
                updateUiState(it)
            }
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
        val contentUiState = uiState.value as ContentUiState

        updateUiState(
            contentUiState.copy(
                progressBarVisible = true,
                doneButtonUiState = DoneButtonDisabledUiState(titleRes = R.string.reader_btn_done)
            )
        )

        viewModelScope.launch {
            when (val result = readerTagRepository.saveInterests(contentUiState.getSelectedInterests())) {
                is Success -> {
                    parentViewModel.onCloseReaderInterests()
                }
                is Error -> {
                    if (result is NetworkUnavailable) {
                        _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(R.string.no_network_message))
                        )
                    } else if (result is RemoteRequestFailure) {
                        _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(R.string.reader_error_request_failed_title))
                        )
                    }
                    updateUiState(
                        contentUiState.copy(
                            progressBarVisible = false,
                            doneButtonUiState = DoneButtonEnabledUiState(titleRes = R.string.reader_btn_done)
                        )
                    )
                }
            }
        }
    }

    fun onRetryButtonClick() {
        if (!userTagsFetchedSuccessfully) {
            loadUserTags()
        } else {
            loadInterests()
        }
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
        interests.map { interest ->
            InterestUiState(interest.tagTitle, interest.tagSlug)
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
        open val progressBarVisible: Boolean = false,
        val titleVisible: Boolean = false,
        val subtitleVisible: Boolean = false,
        val errorLayoutVisible: Boolean = false
    ) {
        object InitialLoadingUiState : UiState(
            progressBarVisible = true
        )

        data class ContentUiState(
            val interestsUiState: List<InterestUiState>,
            val interests: ReaderTagList,
            override val progressBarVisible: Boolean = false,
            override val doneButtonUiState: DoneButtonUiState = DoneButtonDisabledUiState()
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

            object RequestFailedErrorUiState : ErrorUiState(
                titleResId = R.string.reader_error_request_failed_title
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
                        checkedInterestUiState.slug
                    }.contains(it.tagSlug)
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
                    DoneButtonDisabledUiState()
                } else {
                    DoneButtonEnabledUiState()
                }
            } else {
                DoneButtonHiddenUiState
            }
        }
    }

    data class InterestUiState(
        val title: String,
        val slug: String,
        val isChecked: Boolean = false
    )

    sealed class DoneButtonUiState(
        @StringRes open val titleRes: Int = R.string.reader_btn_done,
        val enabled: Boolean = false,
        val visible: Boolean = true
    ) {
        data class DoneButtonEnabledUiState(
            @StringRes override val titleRes: Int = R.string.reader_btn_done
        ) : DoneButtonUiState(
            enabled = true
        )

        data class DoneButtonDisabledUiState(
            @StringRes override val titleRes: Int = R.string.reader_btn_select_few_interests
        ) : DoneButtonUiState(
            enabled = false
        )

        object DoneButtonHiddenUiState : DoneButtonUiState(
            visible = false
        )
    }
}
