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
import kotlinx.coroutines.sync.Mutex
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadUtils.PostUploadAction
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.CrashLoggingUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.skip
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Automatically uploads local drafts.
 *
 * Auto-uploads happen when the app is placed in the foreground or when the internet connection is restored. In
 * addition to this, call sites can also request an immediate execution by calling [upload].
 *
 * The method [activateAutoUploading] must be called once, preferably during app creation, for the auto-uploads to work.
 */
@Singleton
open class UploadStarter @Inject constructor(
    /**
     * The Application context
     */
    private val context: Context,
    private val postStore: PostStore,
    private val pageStore: PageStore,
    private val siteStore: SiteStore,
    private val uploadStore: UploadStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val uploadServiceFacade: UploadServiceFacade,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val postUtilsWrapper: PostUtilsWrapper,
    private val connectionStatus: LiveData<ConnectionStatus>
) : CoroutineScope {
    private val job = Job()
    private val MAXIMUM_AUTO_INITIATED_UPLOAD_RETRIES = 10

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

    open fun queueUploadFromAllSites() = launch {
        val sites = siteStore.sites
        try {
            checkConnectionAndUpload(sites = sites)
        } catch (e: Exception) {
            CrashLoggingUtils.log(e)
        }
    }

    /**
     * Upload all local drafts from the given [site].
     */
    open fun queueUploadFromSite(site: SiteModel) = launch {
        try {
            checkConnectionAndUpload(sites = listOf(site))
        } catch (e: Exception) {
            CrashLoggingUtils.log(e)
        }
    }

    /**
     * If there is an internet connection, uploads all local drafts belonging to [sites].
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
     */
    private suspend fun upload(site: SiteModel) = coroutineScope {
        try {
            mutex.lock()
            val posts = async { postStore.getPostsWithLocalChanges(site) }
            val pages = async { pageStore.getPagesWithLocalChanges(site) }

            val postsAndPages = posts.await() + pages.await()

            // TODO Set Retry = false when autosaving or perhaps check `getNumberOfPostUploadErrorsOrCancellations != 0`
            postsAndPages
                    .asSequence()
                    .filterNot {
                        if (UploadUtils.getPostUploadAction(it) == PostUploadAction.REMOTE_AUTO_SAVE) {
                            UploadUtils.postLocalChangesAlreadyRemoteAutoSaved(it)
                        } else {
                            false
                        }
                    }
                    .filterNot { uploadServiceFacade.isPostUploadingOrQueued(it) }
                    .filter { postUtilsWrapper.isPublishable(it) }
                    .filter {
                        uploadStore.getNumberOfPostUploadErrorsOrCancellations(it) < MAXIMUM_AUTO_INITIATED_UPLOAD_RETRIES
                    }
                    .toList()
                    .forEach { post ->
                        AppLog.d(AppLog.T.POSTS, "UploadStarter for post title: ${post.title}")
                        uploadServiceFacade.uploadPost(
                                context = context,
                                post = post,
                                // TODO Should we track analytics in certain cases ?!? We don't know if it's
                                //  firstTimePublish since we don't have the original status of the post
                                trackAnalytics = false
                        )
                    }
        } finally {
            mutex.unlock()
        }
    }
}
