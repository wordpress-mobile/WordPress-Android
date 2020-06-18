package org.wordpress.android.ui.uploads

import android.content.Context
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
class UploadStarter @Inject constructor(
    /**
     * The Application context
     */
    private val context: Context,
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

    override val coroutineContext: CoroutineContext get() = job + bgDispatcher

    /**
     * The hook for making this class automatically launch uploads whenever the app is placed in the foreground.
     */
    private val processLifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Event.ON_START)
        fun onAppComesFromBackground() {
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
        connectionStatus.skip(1).observe(processLifecycleOwner, Observer {
            queueUploadFromAllSites()
        })

        processLifecycleOwner.lifecycle.addObserver(processLifecycleObserver)
    }

    fun queueUploadFromAllSites() = launch {
        val sites = siteStore.sites
        try {
            checkConnectionAndUpload(sites = sites)
        } catch (e: Exception) {
            AppLog.e(T.MEDIA, e)
        }
    }

    /**
     * Upload all local drafts from the given [site].
     */
    fun queueUploadFromSite(site: SiteModel) = launch {
        try {
            checkConnectionAndUpload(sites = listOf(site))
        } catch (e: Exception) {
            AppLog.e(T.MEDIA, e)
        }
    }

    /**
     * If there is an internet connection, uploads all posts with local changes belonging to [sites].
     *
     * This coroutine will suspend until all the [upload] operations have completed. If one of them fails, all query
     * and queuing attempts ([upload]) will be canceled. The exception will be thrown by this method.
     */
    private suspend fun checkConnectionAndUpload(sites: List<SiteModel>) = coroutineScope {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return@coroutineScope
        }

        sites.forEach {
            launch(ioDispatcher) {
                upload(site = it)
            }
        }
    }

    /**
     * This is meant to be used by [checkConnectionAndUpload] only.
     *
     * The method needs to be synchronized from the following reasons. When the app comes to foreground both
     * `queueUploadFromAllSites` and `queueUploadFromSite` are invoked. The problem is that they can run in parallel
     * and `uploadServiceFacade.isPostUploadingOrQueued(it)` might return out-of-date result and a same post is added
     * twice.
     */
    @Synchronized
    private suspend fun upload(site: SiteModel) = coroutineScope {
        val posts = async { postStore.getPostsWithLocalChanges(site) }
        val pages = async { pageStore.getPagesWithLocalChanges(site) }
        val list = posts.await() + pages.await()

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
                    trackAutoUploadAction(action, post.status, post.isPage)
                    AppLog.d(
                            AppLog.T.POSTS,
                            "UploadStarter for post (isPage: ${post.isPage}) title: ${post.title}, action: $action"
                    )
                    dispatcher.dispatch(
                            UploadActionBuilder.newIncrementNumberOfAutoUploadAttemptsAction(
                                    post
                            )
                    )
                    uploadServiceFacade.uploadPost(
                            context = context,
                            post = post,
                            trackAnalytics = false
                    )
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
