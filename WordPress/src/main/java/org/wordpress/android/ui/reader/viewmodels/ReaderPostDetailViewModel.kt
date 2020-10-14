package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderPostActions
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderPostDetailViewModel @Inject constructor(
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerPostMoreButtonUiStateBuilder: ReaderPostMoreButtonUiStateBuilder,
    private val postDetailUiStateBuilder: ReaderPostDetailUiStateBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MediatorLiveData<ReaderPostDetailsUiState>()
    val uiState: LiveData<ReaderPostDetailsUiState> = _uiState

    private val _refreshPost = MediatorLiveData<Event<Unit>>()
    val refreshPost: LiveData<Event<Unit>> = _refreshPost

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true

        init()
    }

    private fun init() {
        _uiState.addSource(readerPostCardActionsHandler.followStatusUpdated) { data ->
            val currentUiState: ReaderPostDetailsUiState? = _uiState.value

            currentUiState?.let {
                findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                    post.isFollowedByCurrentUser = data.following
                    _uiState.value = convertPostToUiState(post)
                }
            }
        }

        _refreshPost.addSource(readerPostCardActionsHandler.refreshPosts) {
            val currentUiState: ReaderPostDetailsUiState? = _uiState.value
            currentUiState?.let {
                findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                    _uiState.value = convertPostToUiState(post)
                }
            }
        }

        _snackbarEvents.addSource(readerPostCardActionsHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _navigationEvents.addSource(readerPostCardActionsHandler.navigationEvents) { event ->
            _navigationEvents.value = event
        }
    }

    private fun onMoreButtonClicked(uiState: ReaderPostDetailsUiState) {
        changeMoreMenuVisibility(uiState, true)
    }

    private fun onMoreMenuDismissed(uiState: ReaderPostDetailsUiState) {
        changeMoreMenuVisibility(uiState, false)
    }

    private fun changeMoreMenuVisibility(currentUiState: ReaderPostDetailsUiState, show: Boolean) {
        launch {
            findPost(currentUiState.postId, currentUiState.blogId)?.let { post ->
                val moreMenuItems = if (show) {
                    readerPostMoreButtonUiStateBuilder.buildMoreMenuItems(
                            post, TAG_FOLLOWED, this@ReaderPostDetailViewModel::onButtonClicked
                    )
                } else {
                    null
                }

                _uiState.value = currentUiState.copy(moreMenuItems = moreMenuItems)
            }
        }
    }

    fun onButtonClicked(postId: Long, blogId: Long, type: ReaderPostCardActionType) {
        launch {
            findPost(postId, blogId)?.let {
                readerPostCardActionsHandler.onAction(it, type, isBookmarkList = false)
            }
        }
    }

    fun onShowPost(post: ReaderPost) {
        _uiState.value = convertPostToUiState(post)
    }

    private fun onTagItemClicked(tagSlug: String) {
        launch(ioDispatcher) {
            val readerTag = readerUtilsWrapper.getTagFromTagName(tagSlug, FOLLOWED)
            _navigationEvents.postValue(Event(ShowPostsByTag(readerTag)))
        }
    }

    private fun onBlogSectionClicked(postId: Long, blogId: Long) {
        launch {
            readerPostCardActionsHandler.handleHeaderClicked(blogId, postId)
        }
    }

    private fun findPost(postId: Long, blogId: Long): ReaderPost? {
        return readerPostTableWrapper.getBlogPost(
                blogId,
                postId,
                true
        )
    }

    private fun convertPostToUiState(
        post: ReaderPost
    ): ReaderPostDetailsUiState {
        return postDetailUiStateBuilder.mapPostToUiState(
                post = post,
                onButtonClicked = this@ReaderPostDetailViewModel::onButtonClicked,
                onMoreButtonClicked = this@ReaderPostDetailViewModel::onMoreButtonClicked,
                onMoreDismissed = this@ReaderPostDetailViewModel::onMoreMenuDismissed,
                onBlogSectionClicked = this@ReaderPostDetailViewModel::onBlogSectionClicked,
                onFollowClicked = { onButtonClicked(post.postId, post.blogId, FOLLOW) },
                onTagItemClicked = this@ReaderPostDetailViewModel::onTagItemClicked
        )
    }

    data class ReaderPostDetailsUiState(
        val postId: Long,
        val blogId: Long,
        val headerUiState: ReaderPostDetailsHeaderUiState,
        val moreMenuItems: List<SecondaryAction>? = null,
        val actions: ReaderPostActions,
        val onMoreButtonClicked: (ReaderPostDetailsUiState) -> Unit,
        val onMoreDismissed: (ReaderPostDetailsUiState) -> Unit
    )

    override fun onCleared() {
        super.onCleared()
        readerPostCardActionsHandler.onCleared()
    }
}
