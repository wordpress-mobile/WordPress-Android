package org.wordpress.android.ui.reader.discover

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.repository.ReaderDiscoverRepository
import org.wordpress.android.ui.reader.usecases.PreLoadPostContent
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderDiscoverViewModel @Inject constructor(
    private val readerDiscoverRepositoryFactory: ReaderDiscoverRepository.Factory,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val reblogUseCase: ReblogUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MediatorLiveData<DiscoverUiState>()
    val uiState: LiveData<DiscoverUiState> = _uiState

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _preloadPostEvents = MediatorLiveData<Event<PreLoadPostContent>>()
    val preloadPostEvents = _preloadPostEvents

    /**
     * Post which is about to be reblogged after the user selects a target site.
     */
    private var pendingReblogPost: ReaderPost? = null

    /* TODO malinjir calculate photon dimensions - check if DisplayUtils.getDisplayPixelWidth
        returns result based on device orientation */
    private val photonWidth: Int = 500
    private val photonHeight: Int = 500

    private lateinit var readerDiscoverRepository: ReaderDiscoverRepository

    fun start() {
        if (isStarted) return
        isStarted = true

        init()
    }

    private fun init() {
        // Start with loading state
        _uiState.value = LoadingUiState

        // Get the correct repository
        readerDiscoverRepository = readerDiscoverRepositoryFactory.create()
        readerDiscoverRepository.start()

        // Listen to changes to the discover feed
        _uiState.addSource(readerDiscoverRepository.discoverFeed) { posts ->
            _uiState.value = ContentUiState(
                    posts.map {
                        postUiStateBuilder.mapPostToUiState(
                                post = it,
                                photonWidth = photonWidth,
                                photonHeight = photonHeight,
                                isBookmarkList = false,
                                onButtonClicked = this::onButtonClicked,
                                onItemClicked = this::onItemClicked,
                                onItemRendered = this::onItemRendered,
                                onDiscoverSectionClicked = this::onDiscoverClicked,
                                onMoreButtonClicked = this::onMoreButtonClicked,
                                onVideoOverlayClicked = this::onVideoOverlayClicked,
                                onPostHeaderViewClicked = this::onPostHeaderClicked,
                                postListType = TAG_FOLLOWED
                        )
                    }
            )
        }

        readerDiscoverRepository.communicationChannel.observeForever { data ->
            data?.let {
                // TODO listen for communications from the reeaderPostRepository, but not 4ever!
            }
        }

        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            val target = event.peekContent()
            if (target is ShowSitePickerForResult) {
                pendingReblogPost = target.post
            }
            _navigationEvents.value = event
        }

        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _preloadPostEvents.addSource(readerPostCardActionsHandler.preloadPostEvents) { event ->
            _preloadPostEvents.value = event
        }
    }

    private fun onButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        launch {
            // TODO malinjir replace with repository. Also consider if we need to load the post form db in on click.
            val post = ReaderPostTable.getBlogPost(blogId, postId, true)
            readerPostCardActionsHandler.onAction(post, type, isBookmarkList = false)
        }
    }

    private fun onVideoOverlayClicked(postId: Long, blogId: Long) {
        // TODO malinjir implement action
    }

    private fun onPostHeaderClicked(postId: Long, blogId: Long) {
        // TODO malinjir implement action
    }

    private fun onItemClicked(postId: Long, blogId: Long) {
        AppLog.d(T.READER, "OnItemClicked")
    }

    private fun onItemRendered(postId: Long, blogId: Long) {
        AppLog.d(T.READER, "OnItemRendered")
    }

    private fun onDiscoverClicked(postId: Long, blogId: Long) {
        AppLog.d(T.READER, "OnDiscoverClicked")
    }

    // TODO malinjir get rid of the view reference
    private fun onMoreButtonClicked(postId: Long, blogId: Long, view: View) {
        AppLog.d(T.READER, "OnMoreButtonClicked")
    }

    fun onReblogSiteSelected(siteLocalId: Int) {
        // TODO malinjir almost identical to ReaderPostCardActionsHandler.handleReblogClicked.
        //  Consider refactoring when ReaderPostCardActionType is transformed into a sealed class.
        val state = reblogUseCase.onReblogSiteSelected(siteLocalId, pendingReblogPost)
        val navigationTarget = reblogUseCase.convertReblogStateToNavigationEvent(state)
        if (navigationTarget != null) {
            _navigationEvents.postValue(Event(navigationTarget))
        } else {
            _snackbarEvents.postValue(Event(SnackbarMessageHolder(R.string.reader_reblog_error)))
        }
        pendingReblogPost = null
    }

    override fun onCleared() {
        super.onCleared()
        readerDiscoverRepository.stop()
    }

    sealed class DiscoverUiState(
        val contentVisiblity: Boolean = false,
        val progressVisibility: Boolean = false
    ) {
        data class ContentUiState(val cards: List<ReaderCardUiState>) : DiscoverUiState(contentVisiblity = true)
        object LoadingUiState : DiscoverUiState(progressVisibility = true)
        object ErrorUiState : DiscoverUiState()
    }
}
