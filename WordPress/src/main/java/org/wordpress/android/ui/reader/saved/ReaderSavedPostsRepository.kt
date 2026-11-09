package org.wordpress.android.ui.reader.saved

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.rs.data.WpServiceProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_api.ReaderSavedSource
import uniffi.wp_mobile.ReaderSavedException
import uniffi.wp_mobile.ReaderSavedPostsCollection
import uniffi.wp_mobile.ReaderSavedService
import uniffi.wp_mobile.ReaderSavedStatus
import uniffi.wp_mobile_cache.SqliteDbException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The signed-in account's Reader saved posts, synced through wordpress-rs [ReaderSavedService] when
 * the [Feature.READER_SAVED_POSTS_SYNC] experimental feature is on.
 *
 * The posts saved on this device before syncing are imported once per account. Until that import
 * succeeds the Saved tab keeps showing the device's own list, so a failed import never hides a save.
 */
@Singleton
class ReaderSavedPostsRepository @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val accountStore: AccountStore,
    private val wpServiceProvider: WpServiceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    /** One service per account, as wordpress-rs requires: each instance only knows its own writes. */
    @Volatile
    private var current: AccountService? = null
    private val importMutex = Mutex()

    val isEnabled: Boolean
        get() = experimentalFeatures.isEnabled(Feature.READER_SAVED_POSTS_SYNC) && accountStore.hasAccessToken()

    private val userId: Long
        get() = accountStore.account.userId

    /** Whether the Saved tab shows the synced list rather than the device's own. */
    fun isSyncedListReady(): Boolean = isEnabled && appPrefsWrapper.isReaderSavedPostsImported(userId)

    /**
     * Whether [post] is saved, as far as this device knows. Doesn't touch the network or the disk, so
     * it's safe on the main thread.
     */
    fun isSaved(post: ReaderPost): Boolean {
        val status = if (isEnabled) peekService()?.status(post.toSaveIdentity()) else null
        return when (status) {
            ReaderSavedStatus.SAVED -> true
            ReaderSavedStatus.NOT_SAVED -> false
            ReaderSavedStatus.UNKNOWN, null -> post.isBookmarked
        }
    }

    /** Imports this device's saves if they haven't been, then catches up with saves made elsewhere. */
    fun sync() {
        if (!isEnabled) return
        appScope.launch(ioDispatcher) {
            importLocalSavesIfNeeded()
            try {
                getService()?.refresh()
            } catch (e: ReaderSavedException) {
                AppLog.e(T.READER, "Refreshing saved posts failed", e)
            }
        }
    }

    /**
     * Imports the posts saved on this device to the account, once. Returns whether they have been
     * imported. Local saves are left in place either way.
     */
    suspend fun importLocalSavesIfNeeded(): Boolean = withContext(ioDispatcher) {
        importMutex.withLock {
            val userId = userId
            if (appPrefsWrapper.isReaderSavedPostsImported(userId)) return@withLock true
            val service = getService() ?: return@withLock false
            val saves = readerPostTableWrapper.getBookmarkedPosts().map { it.toLocalSave() }
            try {
                val report = service.importLocalSaves(saves)
                AppLog.i(
                    T.READER,
                    "Saved posts import: imported=${report.imported}, rejected=${report.rejected.size}, " +
                        "unsaveable=${report.unsaveable}, alreadyImported=${report.alreadyImported}"
                )
                appPrefsWrapper.setReaderSavedPostsImported(userId)
                true
            } catch (e: ReaderSavedException) {
                AppLog.e(T.READER, "Saved posts import failed", e)
                false
            }
        }
    }

    /**
     * Saves or unsaves a post on the account, and returns whether it worked. The write finishes even
     * if the caller is cancelled, so the server sees writes in the order they were made.
     */
    suspend fun setSaved(identity: ReaderPostSaveIdentity, saved: Boolean): Boolean = write {
        if (saved) it.save(identity) else it.unsave(identity)
    }

    /** Unsaves a key from the saved list that has no post, such as a page saved by URL. */
    suspend fun unsaveKey(key: ReaderSavedKey): Boolean = write { it.unsaveKey(key) }

    /** The account's saved list, or null when there's no account. */
    suspend fun savedPosts(): ReaderSavedPostsCollection? = withContext(ioDispatcher) { getService()?.savedPosts() }

    /**
     * Forgets the account's synced saves on this device. Call at sign-out. Opening the cache loads the
     * wordpress-rs library, so the removal runs off the main thread.
     */
    @Synchronized
    fun clear(userId: Long) {
        current = null
        if (userId <= 0) return
        appScope.launch(ioDispatcher) {
            try {
                wpServiceProvider.getOrCreateCache().cache.removeWordpressComAccount(userId.toULong())
            } catch (e: SqliteDbException) {
                AppLog.e(T.READER, "Removing saved posts for the signed-out account failed", e)
            }
        }
    }

    private suspend fun write(block: suspend (ReaderSavedService) -> Unit): Boolean =
        withContext(NonCancellable + ioDispatcher) {
            val service = getService() ?: return@withContext false
            try {
                block(service)
                true
            } catch (e: ReaderSavedException) {
                AppLog.e(T.READER, "Saving or unsaving a post failed", e)
                false
            }
        }

    private fun peekService(): ReaderSavedService? = current?.takeIf { it.userId == userId }?.service

    /** Creates the account's service on first use, which reads the cache: call it off the main thread. */
    @Synchronized
    private fun getService(): ReaderSavedService? {
        if (!accountStore.hasAccessToken()) return null
        val userId = userId
        if (userId <= 0) return null
        peekService()?.let { return it }
        return try {
            ReaderSavedService(
                wpServiceProvider.createWpComDelegate(),
                ReaderSavedSource.ANDROID,
                wpServiceProvider.getOrCreateCache().cache,
                userId.toULong()
            ).also { current = AccountService(userId, it) }
        } catch (e: ReaderSavedException) {
            AppLog.e(T.READER, "Creating the saved posts service failed", e)
            null
        }
    }

    private class AccountService(val userId: Long, val service: ReaderSavedService)
}
