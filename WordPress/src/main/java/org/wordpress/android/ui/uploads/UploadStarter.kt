package org.wordpress.android.ui.uploads

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.testing.OpenForTesting
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction
import org.wordpress.android.ui.uploads.UploadActionUseCase.UploadAction.DO_NOTHING
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.skip
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Automatically remote-auto-save or upload all local modifications to posts.
 *
 * Auto-uploads happen when the app is placed in the foreground or when the internet connection is restored. In
 * addition to this, call sites can also request an immediate execution by calling [checkConnectionAndUpload].
 *
 * The method [activateAutoUploading] must be called once, preferably during app creation, for the auto-uploads to work.
 */
@Singleton
@OpenForTesting
@Suppress("LongParameterList")
class UploadStarter @Inject constructor(
    private val appContext: Context,
    private val dispatcher: Dispatcher,
    private val postStore: PostStore,
    private val pageStore: PageStore,
    private val siteStore: SiteStore,
    private val uploadActionUseCase: UploadActionUseCase,
    private val tracker: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val uploadServiceFacade: UploadServiceFacade,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val connectionStatus: LiveData<ConnectionStatus>
) : CoroutineScope {
    private val job = Job()

    /**
     * When the app comes to foreground both `queueUploadFromAllSites` and `queueUploadFromSite` are invoked.
     * The problem is that they can run in parallel and `uploadServiceFacade.isPostUploadingOrQueued(it)` might return
     * out-of-date result and a same post is added twice.
     */
    private val mutex = Mutex()

    override val coroutineContext: CoroutineContext get() = job + bgDispatcher

    /**
     * The hook for making this class automatically launch uploads whenever the app is placed in the foreground.
     */
    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            queueUploadFromAllSites()
        }
    }

    /**
     * Activates the necessary observers for this class to start auto-uploading.
     *
     * This must be called during [org.wordpress.android.WordPress]' creation like so:
     *
     * ```
     * mUploadStarter.activateAutoUploading(ProcessLifecycleOwner.get())
     * ```
     */
    fun activateAutoUploading(processLifecycleOwner: ProcessLifecycleOwner) {
        // We're skipping the first emitted value because the processLifecycleObserver below will also trigger an
        // immediate upload.
        connectionStatus.skip(1).observe(processLifecycleOwner) { queueUploadFromAllSites() }

        processLifecycleOwner.lifecycle.addObserver(processLifecycleObserver)
    }

    fun queueUploadFromAllSites() = launch { checkConnectionAndUpload(sites = siteStore.sites) }

    /**
     * Upload all local drafts from the given [site].
     */
    fun queueUploadFromSite(site: SiteModel) = launch { checkConnectionAndUpload(sites = listOf(site)) }

    /**
     * If there is an internet connection, uploads all posts with local changes belonging to [sites].
     *
     * This coroutine will suspend until all the [upload] operations have completed. If one of them fails, all query
     * and queuing attempts ([upload]) will continue. The last exception will be thrown by this method.
     */
    private suspend fun checkConnectionAndUpload(sites: List<SiteModel>) = coroutineScope {
        if (!networkUtilsWrapper.isNetworkAvailable()) return@coroutineScope
        try {
            sites.forEach {
                launch(ioDispatcher) {
                    upload(site = it)
                }
            }
        } catch (e: Exception) {
            AppLog.e(T.MEDIA, e)
        }
    }

    /**
     * This is meant to be used by [checkConnectionAndUpload] only.
     */
    private suspend fun upload(site: SiteModel) = coroutineScope {
        mutex.withLock {
            val posts = async { postStore.getPostsWithLocalChanges(site) }
            val pages = async { pageStore.getPagesWithLocalChanges(site) }
            val list = posts.await() + pages.await()
            var throwable: Throwable? = null

            list.asSequence()
                .map { post ->
                    val action = uploadActionUseCase.getAutoUploadAction(post, site)
                    Pair(post, action)
                }
                .filter { (_, action) ->
                    action != DO_NOTHING
                }
                .toList()
                .forEach { (post, action) ->
                    runCatching {
                        trackAutoUploadAction(action, post.status, post.isPage)
                        AppLog.d(
                            T.POSTS,
                            "UploadStarter for post " +
                                    "(isPage: ${post.isPage.compareTo(false)}) " +
                                    "title: ${post.title}, " +
                                    "action: $action"
                        )
                        dispatcher.dispatch(
                            UploadActionBuilder.newIncrementNumberOfAutoUploadAttemptsAction(
                                post
                            )
                        )
                        uploadServiceFacade.uploadPost(
                            context = appContext,
                            post = post,
                            trackAnalytics = false
                        )
                    }.onFailure {
                        AppLog.e(T.POSTS, it)
                        throwable = it
                    }
                }
            throwable?.let {
                throw it
            }
        }
    }

    private fun trackAutoUploadAction(
        action: UploadAction,
        status: String,
        isPage: Boolean
    ) {
        tracker.track(
            if (isPage) Stat.AUTO_UPLOAD_PAGE_INVOKED else Stat.AUTO_UPLOAD_POST_INVOKED,
            mapOf(
                "upload_action" to action.toString(),
                "post_status" to status
            )
        )
    }
}
