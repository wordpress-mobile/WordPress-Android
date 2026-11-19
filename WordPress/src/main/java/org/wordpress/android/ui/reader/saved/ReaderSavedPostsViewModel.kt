package org.wordpress.android.ui.reader.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.saved.ReaderSavedPostsRepository.WriteResult
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.cache.kotlin.ObservableCollection
import rs.wordpress.cache.kotlin.createObservableCollection
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderSavedException
import uniffi.wp_mobile.ReaderSavedListItem
import uniffi.wp_mobile.ReaderSavedPostsCollection
import javax.inject.Inject
import javax.inject.Named

/**
 * The Saved tab when saves are synced: the account's saved list from the wordpress-rs cache, which
 * reloads whenever the cache changes, so saves made anywhere in the app show up without bookkeeping.
 */
@HiltViewModel
class ReaderSavedPostsViewModel @Inject constructor(
    private val repository: ReaderSavedPostsRepository,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerTracker: ReaderTracker,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val cardBuilder: ReaderSavedPostCardBuilder,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    /** A saved item and the card that shows it. */
    data class SavedPost(val item: ReaderSavedListItem, val card: ReaderPostUiState)

    data class UiState(
        val posts: List<SavedPost> = emptyList(),
        val isLoaded: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        /** Why the last refresh didn't load, shown when there's nothing else to show. */
        val error: ReaderPostListEmptyState? = null,
    )

    sealed class Event {
        data class OpenItem(val item: ReaderSavedListItem) : Event()
        data class OpenComments(val blogId: Long, val postId: Long) : Event()
        data class OpenSite(val blogId: Long, val feedId: Long, val isFollowed: Boolean) : Event()
        data class Unsaved(val item: ReaderSavedListItem) : Event()
        data object RequestFailed : Event()
        data object NoNetwork : Event()

        /** Undoing an unsave went past the account's limit of saved posts. */
        data object TooManySaves : Event()
        data object LoadMoreFailed : Event()

        /** Shows the item at [index], such as a post an undo restored, which comes back as the newest. */
        data class ScrollTo(val index: Int) : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val reloadRequests = Channel<Unit>(Channel.CONFLATED)

    private var isStarted = false

    // a post an undo restores, scrolled to once the list has it: saving it again makes it the newest save, so it
    // comes back at the top rather than where it was
    private var revealKey: ReaderSavedKey? = null

    // a card only changes when its item does, so cards are kept across reloads
    private var cards = emptyMap<ReaderSavedListItem, ReaderPostUiState>()
    private var photonWidth = 0
    private var photonHeight = 0
    private val cardCallbacks = ReaderSavedPostCardBuilder.Callbacks(
        onClicked = ::onItemClicked,
        onUnsaveClicked = ::onUnsaveClicked,
        onCommentsClicked = ::onCommentsClicked,
        onSiteClicked = ::onSiteClicked,
    )
    private var collection: ReaderSavedPostsCollection? = null
    private var observable: ObservableCollection<ReaderSavedListItem>? = null

    /** [photonWidth] and [photonHeight] size the cards' images, as in the Reader's other post lists. */
    fun start(photonWidth: Int, photonHeight: Int) {
        // the window width, and so the image size, changes on rotation
        val isPhotonSizeChanged = photonWidth != this.photonWidth || photonHeight != this.photonHeight
        this.photonWidth = photonWidth
        this.photonHeight = photonHeight
        // set before the collection, which is created off the main thread, so a view recreated meanwhile
        // doesn't start a second one
        if (isStarted) {
            if (isPhotonSizeChanged) {
                cards = emptyMap()
                requestReload()
            }
            return
        }
        isStarted = true
        viewModelScope.launch {
            val collection = repository.savedPosts()?.also { collection = it } ?: run {
                // shows the failure rather than a blank list, and a refresh tries again
                isStarted = false
                _uiState.update {
                    it.copy(isLoaded = true, error = ReaderPostListEmptyState.RequestFailed(isSearch = false))
                }
                return@launch
            }
            observable = createObservableCollection(
                loadData = { collection.loadItems() },
                isRelevantUpdate = { collection.isRelevantUpdate(it) }
            ).also { observable ->
                // The cache reports changes from inside the write that made them, while it still holds the
                // database. Reloading in place (on Main.immediate, from a write polled on main) would wait for
                // that lock on the thread holding it, so only queue the reload here.
                observable.addObserver { requestReload() }
            }
            // one reload at a time, and at most one more queued behind it, since a burst of writes would
            // otherwise read the list once per write and could land out of order
            launch { reloadRequests.receiveAsFlow().collect { reload() } }
            // what the cache has first, then catch up with the server
            requestReload()
            refresh()
        }
    }

    fun refresh() {
        val collection = collection
        when {
            // the saved list couldn't be opened, so try again
            collection == null -> start(photonWidth, photonHeight)
            // a refresh would replace the page being loaded
            _uiState.value.isRefreshing || _uiState.value.isLoadingMore -> Unit
            !networkUtilsWrapper.isNetworkAvailable() -> {
                _uiState.update { it.copy(error = ReaderPostListEmptyState.NoConnection) }
                // the error only shows in place of an empty list, so say so when there are posts
                if (_uiState.value.posts.isNotEmpty()) _events.trySend(Event.NoNetwork)
            }
            else -> refresh(collection)
        }
    }

    private fun refresh(collection: ReaderSavedPostsCollection) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val succeeded = runRequest { collection.refresh() }
            val error = if (succeeded) null else ReaderPostListEmptyState.RequestFailed(isSearch = false)
            _uiState.update { it.copy(isRefreshing = false, error = error) }
            if (!succeeded && _uiState.value.posts.isNotEmpty()) _events.send(Event.RequestFailed)
            requestReload()
        }
    }

    fun loadMore() {
        val collection = collection ?: return
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val succeeded = runRequest { collection.loadNextPage() }
            _uiState.update { it.copy(isLoadingMore = false) }
            requestReload()
            // the list only asks for more when it's scrolled to the end again, so offer a retry
            if (!succeeded) _events.send(Event.LoadMoreFailed)
        }
    }

    private fun onItemClicked(item: ReaderSavedListItem) {
        track(AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST, item)
        _events.trySend(Event.OpenItem(item))
    }

    private fun onCommentsClicked(item: ReaderSavedListItem) {
        val identity = item.post?.identity ?: return
        _events.trySend(Event.OpenComments(identity.localBlogId, identity.localPostId))
    }

    private fun onSiteClicked(item: ReaderSavedListItem) {
        val identity = item.post?.identity ?: return
        val feedId = identity.feedId?.toLong() ?: 0L
        _events.trySend(Event.OpenSite(identity.localBlogId, feedId, isFollowed = item.post?.isFollowing == true))
    }

    fun onUnsaveClicked(item: ReaderSavedListItem) {
        // offline, the unsave waits on this device and goes with the next sync
        viewModelScope.launch {
            val identity = item.post?.identity
            val result = if (identity != null) {
                setSaved(identity, saved = false)
            } else {
                repository.unsaveKey(item.key)
            }
            if (result == WriteResult.SUCCEEDED) {
                // a failed refresh's message no longer describes the list, such as one this leaves empty
                _uiState.update { state ->
                    state.copy(error = state.error.takeUnless { it is ReaderPostListEmptyState.RequestFailed })
                }
                track(AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_SAVED_POST_LIST, item)
                _events.send(Event.Unsaved(item))
            } else {
                _events.send(Event.RequestFailed)
            }
        }
    }

    /** Undoes an unsave. Only offered for items with a post, since a key alone can't be saved again. */
    fun onUndoUnsave(item: ReaderSavedListItem) {
        val identity = item.post?.identity ?: return
        // before the save, as the list shows it again as soon as it starts
        revealKey = item.key
        viewModelScope.launch {
            val result = setSaved(identity, saved = true)
            if (result != WriteResult.SUCCEEDED) revealKey = null
            when (result) {
                WriteResult.SUCCEEDED -> track(AnalyticsTracker.Stat.READER_POST_SAVED_FROM_SAVED_POST_LIST, item)
                WriteResult.FAILED -> _events.send(Event.RequestFailed)
                WriteResult.TOO_MANY_SAVES -> _events.send(Event.TooManySaves)
            }
        }
    }

    /** Tracks as the device's Saved list does, so the two lists' numbers compare. */
    private fun track(stat: AnalyticsTracker.Stat, item: ReaderSavedListItem) {
        val identity = item.post?.identity
        readerTracker.trackBlog(
            stat,
            identity?.localBlogId ?: 0L,
            identity?.feedId?.toLong() ?: 0L,
            item.post?.isFollowing,
            ReaderTracker.SOURCE_SAVED
        )
    }

    /** Saves or unsaves on the account, and keeps the post's local copy, if there is one, in step. */
    private suspend fun setSaved(identity: ReaderPostSaveIdentity, saved: Boolean): WriteResult {
        val result = repository.setSaved(identity, saved)
        if (result == WriteResult.SUCCEEDED) {
            // not waited on, so the snackbar with its Undo shows as soon as the account has the change
            viewModelScope.launch(ioDispatcher) {
                readerPostTableWrapper.setBookmarkFlag(identity.localBlogId, identity.localPostId, saved)
            }
        }
        return result
    }

    private fun requestReload() {
        reloadRequests.trySend(Unit)
    }

    private suspend fun reload() {
        val collection = collection ?: return
        try {
            val cards = cards
            val (posts, info) = withContext(ioDispatcher) {
                val posts = collection.loadItems().map { item ->
                    SavedPost(item, cards[item] ?: cardBuilder.build(item, photonWidth, photonHeight, cardCallbacks))
                }
                posts to collection.listInfo()
            }
            this.cards = posts.associate { it.item to it.card }
            _uiState.update {
                it.copy(posts = posts, isLoaded = true, hasMore = info.hasMore == true)
            }
            revealKey?.let { key ->
                val index = posts.indexOfFirst { it.item.key == key }
                if (index >= 0) {
                    revealKey = null
                    _events.send(Event.ScrollTo(index))
                }
            }
            // back online, such as after the sync a reconnect starts, so the offline message no longer holds
            val isBackOnline = _uiState.value.error == ReaderPostListEmptyState.NoConnection &&
                networkUtilsWrapper.isNetworkAvailable()
            if (isBackOnline) refresh()
        } catch (e: ReaderSavedException) {
            AppLog.e(T.READER, "Loading saved posts failed", e)
            _uiState.update { it.copy(isLoaded = true) }
        }
    }

    /**
     * Runs a request against the server, and returns whether it worked. Off the main thread, since the
     * cache writes what comes back as part of the call.
     */
    private suspend fun runRequest(request: suspend () -> Unit): Boolean = try {
        withContext(ioDispatcher) { request() }
        true
    } catch (e: ReaderSavedException) {
        AppLog.e(T.READER, "Fetching saved posts failed", e)
        false
    }

    override fun onCleared() {
        observable?.close()
        collection?.close()
    }
}
