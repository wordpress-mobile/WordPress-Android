package org.wordpress.android.ui.reader.discover

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BLOCK_SITE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.BOOKMARK
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.COMMENTS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.LIKE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.REBLOG
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SHARE
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.SITE_NOTIFICATIONS
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.VISIT_SITE
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderDiscoverViewModel @Inject constructor(
    private val readerPostRepository: ReaderPostRepository,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MediatorLiveData<DiscoverUiState>()
    val uiState: LiveData<DiscoverUiState> = _uiState

    /* TODO malinjir calculate photon dimensions - check if DisplayUtils.getDisplayPixelWidth
        returns result based on device orientation */
    private val photonWidth: Int = 500
    private val photonHeight: Int = 500

    fun start() {
        if (isStarted) return
        isStarted = true

        init()
        loadPosts()
    }

    private fun init() {
        // Start with loading state
        _uiState.value = LoadingUiState

        // Listen to changes to the discover feed
        _uiState.addSource(readerPostRepository.discoveryFeed) { posts ->
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
    }

    private fun onButtonClicked(postId: Long, blogId: Long, selected: Boolean, type: ReaderPostCardActionType) {
        when (type) {
            FOLLOW -> TODO()
            SITE_NOTIFICATIONS -> TODO()
            SHARE -> TODO()
            VISIT_SITE -> TODO()
            BLOCK_SITE -> TODO()
            LIKE -> TODO()
            BOOKMARK -> TODO()
            REBLOG -> TODO()
            COMMENTS -> TODO()
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

    private fun loadPosts() {
        // TODO malinjir we'll remove this method when the repositories start managing the requests automatically
        launch(bgDispatcher) {
            readerPostRepository.getDiscoveryFeed()
        }
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
