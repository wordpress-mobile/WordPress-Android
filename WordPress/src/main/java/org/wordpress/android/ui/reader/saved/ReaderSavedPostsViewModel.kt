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
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.cache.kotlin.ObservableCollection
import rs.wordpress.cache.kotlin.createObservableCollection
import uniffi.wp_api.ReaderPostSaveIdentity
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
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    data class UiState(
        val items: List<ReaderSavedListItem> = emptyList(),
        val isLoaded: Boolean = false,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        /** Why the last refresh didn't load, shown when there's nothing else to show. */
        val error: ReaderPostListEmptyState? = null,
    )

    sealed class Event {
        data class OpenItem(val item: ReaderSavedListItem) : Event()
        data class Unsaved(val item: ReaderSavedListItem) : Event()
        data object RequestFailed : Event()
        data object NoNetwork : Event()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events: Flow<Event> = _events.receiveAsFlow()

    private val reloadRequests = Channel<Unit>(Channel.CONFLATED)

    private var collection: ReaderSavedPostsCollection? = null
    private var observable: ObservableCollection<ReaderSavedListItem>? = null

    fun start() {
        if (collection != null) return
        viewModelScope.launch {
            val collection = repository.savedPosts()?.also { collection = it } ?: return@launch
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
        if (collection == null || _uiState.value.isRefreshing) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _uiState.update { it.copy(error = ReaderPostListEmptyState.NoConnection) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val succeeded = runRequest { collection.refresh() }
            val error = if (succeeded) null else ReaderPostListEmptyState.RequestFailed(isSearch = false)
            _uiState.update { it.copy(isRefreshing = false, error = error) }
            requestReload()
        }
    }

    fun loadMore() {
        val collection = collection ?: return
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runRequest { collection.loadNextPage() }
            _uiState.update { it.copy(isLoadingMore = false) }
            requestReload()
        }
    }

    fun onItemClicked(item: ReaderSavedListItem) {
        val identity = item.post?.identity
        readerTracker.trackBlog(
            AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST,
            identity?.localBlogId ?: 0L,
            identity?.feedId?.toLong() ?: 0L,
            item.post?.isFollowing,
            ReaderTracker.SOURCE_SAVED
        )
        viewModelScope.launch { _events.send(Event.OpenItem(item)) }
    }

    fun onUnsaveClicked(item: ReaderSavedListItem) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            viewModelScope.launch { _events.send(Event.NoNetwork) }
            return
        }
        viewModelScope.launch {
            val identity = item.post?.identity
            val succeeded = if (identity != null) {
                setSaved(identity, saved = false)
            } else {
                repository.unsaveKey(item.key)
            }
            _events.send(if (succeeded) Event.Unsaved(item) else Event.RequestFailed)
        }
    }

    /** Undoes an unsave. Only offered for items with a post, since a key alone can't be saved again. */
    fun onUndoUnsave(item: ReaderSavedListItem) {
        val identity = item.post?.identity ?: return
        viewModelScope.launch {
            if (!setSaved(identity, saved = true)) {
                _events.send(Event.RequestFailed)
            }
        }
    }

    /** Saves or unsaves on the account, and keeps the post's local copy, if there is one, in step. */
    private suspend fun setSaved(identity: ReaderPostSaveIdentity, saved: Boolean): Boolean {
        val succeeded = repository.setSaved(identity, saved)
        if (succeeded) {
            withContext(ioDispatcher) {
                readerPostTableWrapper.setBookmarkFlag(identity.localBlogId, identity.localPostId, saved)
            }
        }
        return succeeded
    }

    private fun requestReload() {
        reloadRequests.trySend(Unit)
    }

    private suspend fun reload() {
        val collection = collection ?: return
        try {
            val (items, info) = withContext(ioDispatcher) { collection.loadItems() to collection.listInfo() }
            _uiState.update {
                it.copy(items = items, isLoaded = true, hasMore = info.hasMore == true)
            }
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
    } catch (e: ReaderSavedException.StaleLoadMore) {
        // a refresh started while the page loaded, and replaced it
        AppLog.d(T.READER, "Saved posts page discarded: ${e.message}")
        true
    } catch (e: ReaderSavedException) {
        AppLog.e(T.READER, "Fetching saved posts failed", e)
        false
    }

    override fun onCleared() {
        observable?.close()
    }
}
