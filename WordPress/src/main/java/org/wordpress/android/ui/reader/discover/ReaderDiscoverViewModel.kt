package org.wordpress.android.ui.reader.discover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderCardType.PHOTO
import org.wordpress.android.models.ReaderCardType.GALLERY
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.ContentUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.DiscoverUiState.LoadingUiState
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ReaderDiscoverViewModel @Inject constructor(
    private val readerPostRepository: ReaderPostRepository,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MediatorLiveData<DiscoverUiState>()
    val uiState: LiveData<DiscoverUiState> = _uiState

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
            _uiState.value = ContentUiState(posts.map { mapPostToUiState(it) })
        }
    }

    private fun loadPosts() {
        // TODO malinjir we'll remove this method when the repositories start managing the requests automatically
        launch(bgDispatcher) {
            readerPostRepository.getDiscoveryFeed()
        }
    }

    private fun mapPostToUiState(post: ReaderPost) {

        val blogUrl = post.takeIf { it.hasBlogUrl() }?.blogUrl?.let {
            // TODO malinjir remove static access
            UrlUtils.removeScheme(it)
        }

        // TODO malinjir remove static access
        val dateLine = DateTimeUtils.javaDateToTimeSpan(
                post.displayDate,
                WordPress.getContext()
        )

        val avatarOrBlavatarUrl = post.takeIf { it.hasBlogImageUrl() }?.blogImageUrl?.let {
            // TODO malinjir remove static access + use R.dimen.avatar_sz_medium
            GravatarUtils.fixGravatarUrl(it, 9999)
        }

        val blogName = post.takeIf { it.hasBlogName() }?.blogName

        val excerpt = post.takeIf { post.cardType != PHOTO && post.hasExcerpt() }?.excerpt
        val title = post.takeIf { post.cardType != PHOTO && it.hasTitle() }?.title
        // TODO malinjir `post.cardType != GALLERY` might not be needed
        val photoFrameVisibility = (post.hasFeaturedVideo() || post.hasFeaturedImage())
                && post.cardType != GALLERY
        val photoTitle = post.takeIf { it.cardType == PHOTO && it.hasTitle() }?.title

        ReaderPostUiState(
                post.postId,
                title = title,
                excerpt = excerpt,
                blogUrl = blogUrl,
                blogName = blogName,
                dateLine = dateLine,
                avatarOrBlavatarUrl = avatarOrBlavatarUrl,
                photoFrameVisibility = photoFrameVisibility,
                photoTitle = photoTitle
        )
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
        class ReaderPostUiState(
            val id: Long,
            val title: String?,
            val excerpt: String?,// mTxtText
            val dateLine: String,
            val blogUrl: String?,
            val avatarOrBlavatarUrl: String?,
            val blogName: String?,
            val photoFrameVisibility: Boolean,
            val photoTitle: String?
        ) : ReaderCardUiState() {
            val dotSeparatorVisibility: Boolean = blogUrl != null
        }
    }
}
