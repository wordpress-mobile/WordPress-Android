package org.wordpress.android.ui.reader.repository

import android.webkit.URLUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.actions.ReaderActions
import org.wordpress.android.ui.reader.actions.ReaderBlogActions
import org.wordpress.android.ui.reader.actions.ReaderTagActions
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.viewmodel.ContextProvider
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

/**
 * Data access for Manage subscriptions: the followed tags and sites, and following them by name or URL.
 * The Volley callbacks are bridged to suspend functions and the EventBus events to a [Flow].
 */
class ReaderSubsRepository @Inject constructor(
    private val contextProvider: ContextProvider,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore,
    private val readerTracker: ReaderTracker,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    val events: Flow<ReaderSubsEvent> = callbackFlow {
        val subscriber = EventSubscriber { event ->
            trySend(event).onFailure {
                AppLog.w(AppLog.T.READER, "reader subs > failed to deliver $event")
            }
        }
        eventBusWrapper.register(subscriber)
        awaitClose { eventBusWrapper.unregister(subscriber) }
    }

    suspend fun getFollowedTags(): List<ReaderTag> = withContext(ioDispatcher) {
        ReaderTagTable.getFollowedTags()
    }

    suspend fun getFollowedBlogs(): List<ReaderBlog> = withContext(ioDispatcher) {
        ReaderBlogTable.getFollowedBlogs()
    }

    fun isValidTagName(name: String) = ReaderTag.isValidTagName(name)

    fun isFollowedTagName(name: String): Boolean = ReaderTagTable.isFollowedTagName(name)

    fun isFollowedUrl(url: String) = ReaderBlogTable.isFollowedBlogUrl(url) || ReaderBlogTable.isFollowedFeedUrl(url)

    fun createFollowedTag(name: String): ReaderTag = ReaderUtils.createTagFromTagName(name, ReaderTagType.FOLLOWED)

    /**
     * Returns the normalized URL for the passed entry, adding a protocol if it has none, or null if
     * it isn't a valid network URL
     */
    fun normalizeEntryUrl(entry: String): String? {
        val url = UrlUtils.normalizeUrl(if (entry.contains("://")) entry else "http://$entry")
        return url.takeIf { URLUtil.isNetworkUrl(it) }
    }

    fun getHost(url: String): String = UrlUtils.getHost(url)

    fun requestUpdate(tasks: EnumSet<UpdateTask>) =
        ReaderUpdateServiceStarter.startService(contextProvider.getContext(), tasks)

    suspend fun followTag(tag: ReaderTag, follow: Boolean): Boolean = awaitAction { listener ->
        val isLoggedIn = accountStore.hasAccessToken()
        if (follow) {
            ReaderTagActions.addTag(tag, listener, isLoggedIn)
        } else {
            ReaderTagActions.deleteTag(tag, listener, isLoggedIn)
        }
    }

    suspend fun followSite(blogId: Long, feedId: Long, follow: Boolean): Boolean = awaitAction { listener ->
        val source = ReaderTracker.SOURCE_SETTINGS
        if (feedId != 0L) {
            ReaderBlogActions.followFeedById(blogId, feedId, follow, listener, source, readerTracker)
        } else {
            ReaderBlogActions.followBlogById(blogId, feedId, follow, listener, source, readerTracker)
        }
    }

    /**
     * Follows the passed URL as a feed, since typed URLs are more likely to point to a feed than a
     * WordPress site (and the endpoint follows it as a site if it is one)
     */
    suspend fun followUrl(url: String): Boolean = awaitAction { listener ->
        ReaderBlogActions.followFeedByUrl(url, listener, ReaderTracker.SOURCE_SETTINGS, readerTracker)
        true
    }

    /**
     * Returns null if the URL is reachable, otherwise the status code. The follow endpoint accepts any
     * URL, even one that doesn't exist, so this is checked first.
     */
    suspend fun checkUrlReachable(url: String): Int? = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            ReaderBlogActions.checkUrlReachable(url, object : ReaderActions.OnRequestListener<Void> {
                override fun onSuccess(result: Void?) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onFailure(statusCode: Int) {
                    if (continuation.isActive) continuation.resume(statusCode)
                }
            })
        }
    }

    /**
     * Runs an action that reports through an [ReaderActions.ActionListener] and returns whether it
     * succeeded. [start] returns false if the action didn't start.
     */
    private suspend fun awaitAction(start: (ReaderActions.ActionListener) -> Boolean): Boolean =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = ReaderActions.ActionListener { succeeded ->
                    if (continuation.isActive) continuation.resume(succeeded)
                }
                if (!start(listener) && continuation.isActive) continuation.resume(false)
            }
        }

    // EventBus invokes subscriber methods reflectively, so this class has to stay public
    class EventSubscriber(private val onEvent: (ReaderSubsEvent) -> Unit) {
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(@Suppress("UNUSED_PARAMETER") event: ReaderEvents.FollowedTagsFetched) {
            onEvent(ReaderSubsEvent.FollowedTagsFetched)
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onEvent(event: ReaderEvents.FollowedBlogsFetched) {
            onEvent(ReaderSubsEvent.FollowedBlogsFetched(event.didChange()))
        }
    }
}

sealed class ReaderSubsEvent {
    data object FollowedTagsFetched : ReaderSubsEvent()
    data class FollowedBlogsFetched(val didChange: Boolean) : ReaderSubsEvent()
}
