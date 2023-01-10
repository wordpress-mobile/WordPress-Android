package org.wordpress.android.ui.reader.discover.interests

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment.EntryPoint
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonDisabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonEnabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonHiddenUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.ConnectionErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.RequestFailedErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialLoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderInterestsViewModel @Inject constructor(
    private val readerTagRepository: ReaderTagRepository,
    private val readerTracker: ReaderTracker
) : ViewModel() {
    private var isStarted = false
    private lateinit var currentLanguage: String
    private var parentViewModel: ReaderViewModel? = null

    private var entryPoint = EntryPoint.DISCOVER
    private var userTags = ReaderTagList()

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _closeReaderInterests = MutableLiveData<Event<Unit>>()
    val closeReaderInterests: LiveData<Event<Unit>> = _closeReaderInterests

    private var userTagsFetchedSuccessfully = false

    fun start(
        currentLanguage: String,
        parentViewModel: ReaderViewModel?,
        entryPoint: EntryPoint
    ) {
        if (isStarted && this.currentLanguage == currentLanguage) {
            return
        }
        isStarted = true
        this.currentLanguage = currentLanguage
        this.parentViewModel = parentViewModel
        this.entryPoint = entryPoint
        parentViewModel?.dismissQuickStartSnackbarIfNeeded()
        loadUserTags()
    }

    private fun loadUserTags() {
        updateUiState(InitialLoadingUiState)
        viewModelScope.launch {
            when (val result = readerTagRepository.getUserTags()) {
                is ReaderRepositoryCommunication.SuccessWithData<*> -> {
                    userTagsFetchedSuccessfully = true
                    userTags = result.data as ReaderTagList
                    when (entryPoint) {
                        EntryPoint.DISCOVER -> checkAndLoadInterests(userTags)
                        EntryPoint.SETTINGS -> loadInterests(userTags)
                    }
                }
                is ReaderRepositoryCommunication.Error -> {
                    if (result is ReaderRepositoryCommunication.Error.NetworkUnavailable) {
                        updateUiState(ConnectionErrorUiState)
                    } else if (result is ReaderRepositoryCommunication.Error.RemoteRequestFailure) {
                        updateUiState(RequestFailedErrorUiState)
                    }
                }
                ReaderRepositoryCommunication.Started -> Unit // Do nothing
                ReaderRepositoryCommunication.Success -> Unit // Do nothing
            }
        }
    }

    private fun checkAndLoadInterests(userTags: ReaderTagList) {
        if (userTags.isEmpty()) {
            loadInterests(userTags)
        } else {
            parentViewModel?.onCloseReaderInterests()
        }
    }

    private fun loadInterests(userTags: ReaderTagList) {
        updateUiState(InitialLoadingUiState)
        viewModelScope.launch {
            val newUiState: UiState? = when (val result = readerTagRepository.getInterests()) {
                is ReaderRepositoryCommunication.SuccessWithData<*> -> {
                    readerTracker.track(AnalyticsTracker.Stat.SELECT_INTERESTS_SHOWN)

                    val tags = (result.data as ReaderTagList).filter { checkAndExcludeTag(userTags, it) }
                    val distinctTags = ReaderTagList().apply { addAll(tags.distinctBy { it.tagSlug }) }
                    when (entryPoint) {
                        EntryPoint.DISCOVER -> ContentUiState(
                            interestsUiState = transformToInterestsUiState(distinctTags),
                            interests = distinctTags,
                            doneButtonUiState = DoneButtonDisabledUiState(),
                            titleVisible = true
                        )
                        EntryPoint.SETTINGS -> ContentUiState(
                            interestsUiState = transformToInterestsUiState(distinctTags),
                            interests = distinctTags,
                            doneButtonUiState = DoneButtonDisabledUiState(R.string.reader_btn_done),
                            titleVisible = false
                        )
                    }
                }
                is ReaderRepositoryCommunication.Error.NetworkUnavailable -> {
                    ConnectionErrorUiState
                }
                is ReaderRepositoryCommunication.Error.RemoteRequestFailure -> {
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

    private fun checkAndExcludeTag(userTags: ReaderTagList, tag: ReaderTag): Boolean {
        var contain = false
        userTags.forEach { excludedTag ->
            if (excludedTag.tagSlug.equals(tag.tagSlug)) {
                contain = true
                return@forEach
            }
        }
        return !contain
    }

    fun onInterestAtIndexToggled(index: Int, isChecked: Boolean) {
        uiState.value?.let {
            val currentUiState = uiState.value as ContentUiState
            val updatedInterestsUiState = getUpdatedInterestsUiState(index, isChecked)

            updateUiState(
                currentUiState.copy(
                    interestsUiState = updatedInterestsUiState,
                    doneButtonUiState = currentUiState.getDoneButtonState(
                        entryPoint = entryPoint,
                        isInterestChecked = isChecked
                    )
                )
            )

            parentViewModel?.completeQuickStartFollowSiteTaskIfNeeded()
        }
    }

    fun onDoneButtonClick() {
        val contentUiState = uiState.value as ContentUiState

        updateUiState(
            contentUiState.copy(
                progressBarVisible = true,
                doneButtonUiState = DoneButtonDisabledUiState(R.string.reader_btn_done)
            )
        )

        trackInterests(contentUiState.getSelectedInterests())

        viewModelScope.launch {
            readerTagRepository.clearTagLastUpdated(ReaderTag.createDiscoverPostCardsTag())
            when (val result = readerTagRepository.saveInterests(contentUiState.getSelectedInterests())) {
                is ReaderRepositoryCommunication.Success -> {
                    when (entryPoint) {
                        EntryPoint.DISCOVER -> parentViewModel?.onCloseReaderInterests()
                        EntryPoint.SETTINGS -> _closeReaderInterests.value = Event(Unit)
                    }
                }
                is ReaderRepositoryCommunication.Error -> {
                    if (result is ReaderRepositoryCommunication.Error.NetworkUnavailable) {
                        _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
                        )
                    } else if (result is ReaderRepositoryCommunication.Error.RemoteRequestFailure) {
                        _snackbarEvents.postValue(
                            Event(SnackbarMessageHolder(UiStringRes(R.string.reader_error_request_failed_title)))
                        )
                    }
                    updateUiState(
                        contentUiState.copy(
                            progressBarVisible = false,
                            doneButtonUiState = DoneButtonEnabledUiState(R.string.reader_btn_done)
                        )
                    )
                }
                is ReaderRepositoryCommunication.Started -> Unit // Do nothing
                is ReaderRepositoryCommunication.SuccessWithData<*> -> Unit // Do nothing
            }
        }
    }

    fun onRetryButtonClick() {
        if (!userTagsFetchedSuccessfully) {
            loadUserTags()
        } else {
            loadInterests(userTags)
        }
    }

    private fun transformToInterestsUiState(interests: ReaderTagList) =
        interests.map { interest ->
            TagUiState(interest.tagTitle, interest.tagSlug)
        }

    private fun getUpdatedInterestsUiState(index: Int, isChecked: Boolean): List<TagUiState> {
        val currentUiState = uiState.value as ContentUiState
        val newInterestsUiState = currentUiState.interestsUiState.toMutableList()
        newInterestsUiState[index] = currentUiState.interestsUiState[index].copy(isChecked = isChecked)
        return newInterestsUiState
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    private fun trackInterests(tags: List<ReaderTag>) {
        tags.forEach {
            val source = when (entryPoint) {
                EntryPoint.DISCOVER -> ReaderTracker.SOURCE_DISCOVER
                EntryPoint.SETTINGS -> ReaderTracker.SOURCE_SETTINGS
            }
            readerTracker.trackTag(
                AnalyticsTracker.Stat.READER_TAG_FOLLOWED,
                it.tagSlug,
                source
            )
        }
        readerTracker.trackTagQuantity(AnalyticsTracker.Stat.SELECT_INTERESTS_PICKED, tags.size)
    }

    fun onBackButtonClick() {
        when (entryPoint) {
            EntryPoint.DISCOVER -> parentViewModel?.onCloseReaderInterests()
            EntryPoint.SETTINGS -> _closeReaderInterests.value = Event(Unit)
        }
    }

    sealed class UiState(
        open val doneButtonUiState: DoneButtonUiState = DoneButtonHiddenUiState,
        open val progressBarVisible: Boolean = false,
        open val titleVisible: Boolean = false,
        val errorLayoutVisible: Boolean = false
    ) {
        object InitialLoadingUiState : UiState(
            progressBarVisible = true
        )

        data class ContentUiState(
            val interestsUiState: List<TagUiState>,
            val interests: ReaderTagList,
            override val progressBarVisible: Boolean = false,
            override val doneButtonUiState: DoneButtonUiState,
            override val titleVisible: Boolean
        ) : UiState(
            progressBarVisible = false,
            titleVisible = titleVisible,
            errorLayoutVisible = false
        )

        sealed class ErrorUiState constructor(
            val titleRes: Int
        ) : UiState(
            progressBarVisible = false,
            errorLayoutVisible = true
        ) {
            object ConnectionErrorUiState : ErrorUiState(R.string.no_network_message)

            object RequestFailedErrorUiState : ErrorUiState(R.string.reader_error_request_failed_title)
        }

        private fun getCheckedInterestsUiState(): List<TagUiState> {
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
            entryPoint: EntryPoint,
            isInterestChecked: Boolean = false
        ): DoneButtonUiState {
            return if (this is ContentUiState) {
                val disableDoneButton = interests.isEmpty() ||
                        (getCheckedInterestsUiState().size == 1 && !isInterestChecked)
                if (disableDoneButton) {
                    when (entryPoint) {
                        EntryPoint.DISCOVER -> DoneButtonDisabledUiState()
                        EntryPoint.SETTINGS -> DoneButtonDisabledUiState(R.string.reader_btn_done)
                    }
                } else {
                    DoneButtonEnabledUiState()
                }
            } else {
                DoneButtonHiddenUiState
            }
        }
    }

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
