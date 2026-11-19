package org.wordpress.android.ui.reader.saved

import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.networking.NetworkConnectionMonitor
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.rs.data.WpServiceProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.skip
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatusLiveData
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_api.ReaderSavedSource
import uniffi.wp_api.readerSavedKeyNormalized
import uniffi.wp_api.readerSavedKeysForPost
import uniffi.wp_mobile.ReaderSavedException
import uniffi.wp_mobile.ReaderSavedObserver
import uniffi.wp_mobile.ReaderSavedPostsCollection
import uniffi.wp_mobile.ReaderSavedService
import uniffi.wp_mobile.ReaderSavedStatus
import uniffi.wp_mobile_cache.SqliteDbException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The signed-in WordPress.com account's Reader saved posts, synced through wordpress-rs
 * [ReaderSavedService]. Without an account, posts are only saved on this device.
 *
 * The posts saved on this device before syncing are imported once per account. Until that import
 * succeeds the Saved tab keeps showing the device's own list, so a failed import never hides a save.
 */
@Singleton
class ReaderSavedPostsRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val wpServiceProvider: WpServiceProvider,
    private val networkConnectionMonitor: NetworkConnectionMonitor,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    /** One service per account, as wordpress-rs requires: each instance only knows its own writes. */
    @Volatile
    private var current: AccountService? = null
    private val importMutex = Mutex()

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)

    /**
     * Emits when which posts are saved may have changed. A save or unsave emits as soon as it starts, before
     * the server answers, since [isSaved] already reads the new state then, and again if the server refuses it.
     */
    val changes: SharedFlow<Unit> = _changes.asSharedFlow()

    private val observer = object : ReaderSavedObserver {
        override fun savedPostsChanged() {
            _changes.tryEmit(Unit)
        }
    }

    /**
     * Whether saves go to a WordPress.com account. Without one, they're only kept on this device, as they
     * are just after sign-in until the account's details have loaded.
     */
    val isSyncedWithAccount: Boolean
        get() = accountStore.hasAccessToken() && userId > 0

    private val userId: Long
        get() = accountStore.account.userId

    /** Whether the Saved tab shows the synced list rather than the device's own. */
    fun isSyncedListReady(): Boolean = isSyncedWithAccount && appPrefsWrapper.isReaderSavedPostsImported(userId)

    private val _syncedListReady = MutableStateFlow(isSyncedListReady())

    /** [isSyncedListReady] as it changes, so the Saved tab can switch lists once the import finishes. */
    val syncedListReady: StateFlow<Boolean> = _syncedListReady.asStateFlow()

    /**
     * Whether [post] is saved, as far as this device knows. Doesn't touch the network, but reads the
     * cache, and opens it on first use, so call it off the main thread. Until the device's saves are
     * imported, the Saved tab shows the device's own list, so this answers as that list does.
     */
    fun isSaved(post: ReaderPost): Boolean {
        val status = try {
            // getService rather than peekService: before anything else opens the service, the device's own
            // flag would say a post saved elsewhere isn't saved
            if (isSyncedListReady()) getService()?.status(post.toSaveIdentity()) else null
        } catch (e: ReaderSavedException) {
            AppLog.e(T.READER, "Reading whether a post is saved failed", e)
            null
        }
        return when (status) {
            ReaderSavedStatus.SAVED -> true
            ReaderSavedStatus.NOT_SAVED -> false
            ReaderSavedStatus.UNKNOWN, null -> post.isBookmarked
        }
    }

    private var isActivated = false

    /**
     * Syncs whenever the app comes to the foreground, launch included, and when the device comes back
     * online, as wordpress-rs asks. Call once, after the [NetworkConnectionMonitor] has started.
     */
    @MainThread
    fun activate(processLifecycleOwner: LifecycleOwner) {
        if (isActivated) return
        isActivated = true
        processLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = sync()
        })
        // The monitor posts its first state after this runs, and onStart syncs at launch anyway, so only a
        // later change to online syncs
        val isConnected = networkConnectionMonitor.isConnected
        ConnectionStatusLiveData(isConnected)
            .let { if (isConnected.value == null) it.skip(1) else it }
            .observeForever { status -> if (status == ConnectionStatus.AVAILABLE) sync() }
    }

    /**
     * Call once the signed-in account's details have loaded, after any reset of the Reader database, so a
     * fresh sign-in imports and syncs without waiting for the app to come to the foreground again. Only while
     * the import is due, as the account's details also load on every return to My Site.
     */
    fun onAccountFetched() {
        if (!isSyncedListReady()) sync()
    }

    /**
     * Imports this device's saves if they haven't been, sends saves and unsaves made offline, then
     * catches up with saves made elsewhere.
     */
    fun sync() {
        // the account can change without a sign-out, such as when its token is replaced
        _syncedListReady.value = isSyncedListReady()
        if (!isSyncedWithAccount) return
        appScope.launch(ioDispatcher) {
            importLocalSavesIfNeeded()
            try {
                getService()?.sync()
            } catch (e: ReaderSavedException) {
                AppLog.e(T.READER, "Syncing saved posts failed", e)
            }
        }
    }

    /** Imports the posts saved on this device to the account, once. Local saves are left in place. */
    private suspend fun importLocalSavesIfNeeded(): Unit = importMutex.withLock {
        val userId = userId
        if (appPrefsWrapper.isReaderSavedPostsImported(userId)) return@withLock
        // Until the Reader database is reset for this account, its saves may be another account's: one that
        // signed out with saves it hadn't sent. The reset follows sign-in, and so does a sync.
        if (appPrefsWrapper.getLastUsedUserId() != userId) return@withLock
        val service = getService() ?: return@withLock
        val saves = readerPostTableWrapper.getBookmarkedPosts().map { it.toLocalSave() }
        try {
            // saves that can't be sent yet stay pending, and a later sync sends them
            val report = service.importSaves(saves)
            AppLog.i(
                T.READER,
                "Saved posts import: imported=${report.imported}, rejected=${report.rejected.size}, " +
                    "pending=${report.pending}, waitingForRoom=${report.waitingForRoom}, " +
                    "unsaveable=${report.unsaveable}"
            )
            // unless the account was signed out while the import ran
            if (peekService() === service) {
                appPrefsWrapper.setReaderSavedPostsImported(userId)
                _syncedListReady.value = isSyncedListReady()
            }
            trackImport(
                "result" to "imported",
                "local_saves" to saves.size,
                "imported" to report.imported.toLong(),
                "refused" to report.rejected.size,
                "pending" to report.pending.toLong(),
                "waiting_for_room" to report.waitingForRoom.toLong(),
                "unsaveable" to report.unsaveable.toLong()
            )
        } catch (e: ReaderSavedException) {
            // rs tries again next time
            AppLog.e(T.READER, "Saved posts import failed", e)
            trackImport(
                "result" to "failed",
                "local_saves" to saves.size,
                "error_kind" to importErrorKind(e),
                // the error's type only: its message can hold URLs or server text
                "api_error" to (e as? ReaderSavedException.Api)?.v1?.javaClass?.simpleName
            )
        }
    }

    /**
     * Saves or unsaves a post on the account, and returns how it went. A save with [details]
     * sends them along, so the Saved list shows the post straight away instead of an empty row until
     * the next refresh. The write finishes even if the caller is cancelled, so the server sees writes
     * in the order they were made.
     */
    suspend fun setSaved(
        identity: ReaderPostSaveIdentity,
        saved: Boolean,
        details: uniffi.wp_api.ReaderPost? = null
    ): WriteResult = write {
        when {
            !saved -> it.unsave(identity)
            details != null -> it.saveWithDetails(details)
            else -> it.save(identity)
        }
    }

    /** Unsaves a key from the saved list that has no post, such as a page saved by URL. */
    suspend fun unsaveKey(key: ReaderSavedKey): WriteResult = write { it.unsaveKey(key) }

    /** The account's saved list, or null when there's no account. */
    suspend fun savedPosts(): ReaderSavedPostsCollection? = withContext(ioDispatcher) { getService()?.savedPosts() }

    /**
     * Forgets the account's synced saves on this device. Call at sign-out, after resetting the Reader
     * database, which keeps the device's saves.
     *
     * If they'd been imported, the local copies of the ones the account holds are dropped, so the next
     * sign-in doesn't import them again and bring back any unsaved elsewhere since. Saves the account
     * doesn't hold, such as ones made offline that weren't sent, stay: this device is the only place
     * they're kept. The account imports them when it next signs in; a different account resets the
     * Reader database instead.
     *
     * Opening the cache loads the wordpress-rs library, so this runs off the main thread, after any
     * import that's under way.
     */
    // Not synchronized: it runs on the main thread, and getService holds the lock while it opens the cache.
    // current is volatile, and a service opened meanwhile is for an account that's no longer signed in.
    fun clear(userId: Long, savesImported: Boolean) {
        val service = current?.takeIf { it.userId == userId }?.service
        current = null
        _syncedListReady.value = false
        if (userId <= 0) return
        appScope.launch(ioDispatcher) {
            importMutex.withLock {
                try {
                    if (savesImported) {
                        (service ?: createService(userId))?.use { dropLocalCopiesOfAccountSaves(it) }
                    } else {
                        service?.close()
                    }
                    wpServiceProvider.getOrCreateCache().cache.removeWordpressComAccount(userId.toULong())
                } catch (e: SqliteDbException) {
                    AppLog.e(T.READER, "Removing saved posts for the signed-out account failed", e)
                } catch (e: ReaderSavedException) {
                    AppLog.e(T.READER, "Removing saved posts for the signed-out account failed", e)
                }
            }
        }
    }

    /**
     * Drops the local copies of the saves the account holds. One it doesn't, such as a save made offline that
     * hasn't been sent or one the import couldn't send, stays: this device is the only place it's kept.
     */
    private suspend fun dropLocalCopiesOfAccountSaves(service: ReaderSavedService) {
        // wordpress-rs only lists unsent saves as part of the saved list, which includes every pending row
        val unsent = service.savedPosts().use { it.loadItems() }
            .filter { it.isPending || it.isWaitingForRoom }
            .map { readerSavedKeyNormalized(it.key) }
            .toSet()
        readerPostTableWrapper.getBookmarkedPosts()
            .filter { post ->
                val identity = post.toSaveIdentity()
                val isUnsent = readerSavedKeysForPost(identity).any { readerSavedKeyNormalized(it) in unsent }
                !isUnsent && service.status(identity) == ReaderSavedStatus.SAVED
            }
            .forEach { readerPostTableWrapper.setBookmarkFlag(it.blogId, it.postId, false) }
        // otherwise the device's Saved list shows each as a post just removed, with an Undo
        readerPostTableWrapper.purgeUnbookmarkedPostsWithBookmarkTag()
    }

    private suspend fun write(block: suspend (ReaderSavedService) -> Unit): WriteResult =
        withContext(NonCancellable + ioDispatcher) {
            val service = getService() ?: return@withContext WriteResult.FAILED
            try {
                block(service)
                WriteResult.SUCCEEDED
            } catch (e: ReaderSavedException.TooManySaves) {
                AppLog.e(T.READER, "Saving a post failed: the account has reached its save limit", e)
                WriteResult.TOO_MANY_SAVES
            } catch (e: ReaderSavedException) {
                AppLog.e(T.READER, "Saving or unsaving a post failed", e)
                WriteResult.FAILED
            }
        }

    /** How an import ended, as counts and error types only: no URLs or titles. Matches iOS. */
    private fun trackImport(vararg properties: Pair<String, Any?>) {
        analyticsTrackerWrapper.track(
            Stat.READER_SAVED_POSTS_ACCOUNT_IMPORT,
            properties.filter { it.second != null }.toMap()
        )
    }

    private fun importErrorKind(e: ReaderSavedException): String = when (e) {
        is ReaderSavedException.Api -> "api"
        is ReaderSavedException.PostCannotBeSaved -> "post_cannot_be_saved"
        is ReaderSavedException.TooManySaves -> "too_many_saves"
        is ReaderSavedException.UnexpectedImportResults -> "unexpected_import_results"
        is ReaderSavedException.UnmatchedImportResults -> "unmatched_import_results"
        is ReaderSavedException.Database -> "database"
    }

    private fun peekService(): ReaderSavedService? = current?.takeIf { it.userId == userId }?.service

    /** Creates the account's service on first use, which reads the cache: call it off the main thread. */
    @Synchronized
    private fun getService(): ReaderSavedService? {
        val userId = userId.takeIf { isSyncedWithAccount } ?: return null
        return peekService() ?: createService(userId)?.also {
            it.setObserver(observer)
            // one left from another account, such as after its token was replaced, stops reporting changes
            current?.service?.let { old ->
                old.setObserver(null)
                old.close()
            }
            current = AccountService(userId, it)
        }
    }

    private fun createService(userId: Long): ReaderSavedService? = try {
        ReaderSavedService(
            wpServiceProvider.createWpComDelegate(),
            ReaderSavedSource.ANDROID,
            wpServiceProvider.getOrCreateCache().cache,
            userId.toULong()
        )
    } catch (e: SqliteDbException) {
        // opening the cache, such as a database that can't be migrated
        AppLog.e(T.READER, "Creating the saved posts service failed", e)
        null
    } catch (e: ReaderSavedException) {
        AppLog.e(T.READER, "Creating the saved posts service failed", e)
        null
    }

    private class AccountService(val userId: Long, val service: ReaderSavedService)

    /** How a save or unsave ended. One made offline succeeds: it's sent with the next sync. */
    enum class WriteResult {
        SUCCEEDED,
        FAILED,

        /** The account has reached its limit of saved posts, so the save was undone. */
        TOO_MANY_SAVES,
    }
}
