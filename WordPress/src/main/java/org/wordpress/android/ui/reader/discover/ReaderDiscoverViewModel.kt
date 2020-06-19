package org.wordpress.android.ui.reader.discover

import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.image.ImageType
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
                                onBookmarkClicked = this::onBookmarkClicked,
                                onLikeClicked = this::onLikeClicked,
                                onReblogClicked = this::onReblogClicked,
                                onCommentsClicked = this::onCommentsClicked
                        )
                    }
            )
        }
    }

    private fun onBookmarkClicked(postId: Long, blogId: Long, selected: Boolean) {
        // TODO malinjir implement action
    }

    private fun onLikeClicked(postId: Long, blogId: Long, selected: Boolean) {
        // TODO malinjir implement action
    }

    private fun onReblogClicked(postId: Long, blogId: Long, selected: Boolean) {
        // TODO malinjir implement action
    }

    private fun onCommentsClicked(postId: Long, blogId: Long, selected: Boolean) {
        // TODO malinjir implement action
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

    sealed class ReaderCardUiState {
        data class ReaderPostUiState(
            val postId: Long,
            val blogId: Long,
            val dateLine: String,
            val title: String?,
            val blogName: String?,
            val excerpt: String?, // mTxtText
            val blogUrl: String?,
            val photoTitle: String?,
            val featuredImageUrl: String?,
            val videoThumbnailUrl: String?,
            val avatarOrBlavatarUrl: String?,
            val thumbnailStripUrls: List<String>?,
            val discoverSection: DiscoverLayoutUiState?,
            val videoOverlayVisibility: Boolean,
            val moreMenuVisibility: Boolean,
            val photoFrameVisibility: Boolean,
            val bookmarkAction: ActionUiState,
            val likeAction: ActionUiState,
            val reblogAction: ActionUiState,
            val commentsAction: ActionUiState
        ) : ReaderCardUiState() {
            val dotSeparatorVisibility: Boolean = blogUrl != null

            data class DiscoverLayoutUiState(
                val discoverText: Spanned,
                val discoverAvatarUrl: String,
                val imageType: ImageType
            )

            data class ActionUiState(
                val isEnabled: Boolean,
                val isSelected: Boolean? = false,
                val contentDescription: UiString? = null,
                val count: Int = 0,
                val onClicked: ((Long, Long, Boolean) -> Unit)? = null
            )
        }
    }
}
