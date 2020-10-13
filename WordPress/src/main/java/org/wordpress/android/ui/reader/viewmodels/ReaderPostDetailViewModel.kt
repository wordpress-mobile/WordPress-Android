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
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowPostsByTag
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType.FOLLOW
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.ReaderPostDetailsHeaderViewUiStateBuilder
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderPostDetailViewModel @Inject constructor(
    private val readerPostCardActionsHandler: ReaderPostCardActionsHandler,
    private val postDetailsHeaderViewUiStateBuilder: ReaderPostDetailsHeaderViewUiStateBuilder,
    private val readerUtilsWrapper: ReaderUtilsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _headerUiState = MediatorLiveData<ReaderPostDetailsHeaderUiState>()
    val headerUiState: LiveData<ReaderPostDetailsHeaderUiState> = _headerUiState

    private val _navigationEvents = MediatorLiveData<Event<ReaderNavigationEvents>>()
    val navigationEvents: LiveData<Event<ReaderNavigationEvents>> = _navigationEvents

    private var isStarted = false

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true

        init()
    }

    private fun init() {
        _headerUiState.addSource(readerPostCardActionsHandler.followStatusUpdated) { data ->
            val post = readerPostTableWrapper.getBlogPost(
                    data.blogId,
                    (_headerUiState.value as ReaderPostDetailsHeaderUiState).blogSectionUiState.postId,
                    true
            )
            post?.let {
                it.isFollowedByCurrentUser = data.following
                _headerUiState.value = createPostDetailsHeaderUiState(it)
            }
        }
    }

    fun onButtonClicked(post: ReaderPost, type: ReaderPostCardActionType) {
        launch {
            readerPostCardActionsHandler.onAction(post, type, isBookmarkList = false)
        }
    }

    private fun createPostDetailsHeaderUiState(
        post: ReaderPost
    ): ReaderPostDetailsHeaderUiState {
        return postDetailsHeaderViewUiStateBuilder.mapPostToUiState(
                post,
                this@ReaderPostDetailViewModel::onBlogSectionClicked,
                { onButtonClicked(post, FOLLOW) },
                this@ReaderPostDetailViewModel::onTagItemClicked
        )
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
}
