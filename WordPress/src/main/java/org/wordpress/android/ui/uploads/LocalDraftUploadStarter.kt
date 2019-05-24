package org.wordpress.android.ui.uploads

import android.arch.lifecycle.Lifecycle.Event
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
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
class LocalDraftUploadStarter @Inject constructor(
    /**
     * The Application context
     */
    private val context: Context,
    private val postStore: PostStore,
    private val pageStore: PageStore,
    private val siteStore: SiteStore,
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
     * mLocalDraftUploadStarter.activateAutoUploading(ProcessLifecycleOwner.get())
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

    private fun queueUploadFromAllSites() = launch {
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
    fun queueUploadFromSite(site: SiteModel) = launch {
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
        val posts = async { postStore.getLocalDraftPosts(site) }
        val pages = async { pageStore.getLocalDraftPages(site) }

        val postsAndPages = posts.await() + pages.await()

        postsAndPages.filterNot { uploadServiceFacade.isPostUploadingOrQueued(it) }
                .forEach { localDraft ->
                    uploadServiceFacade.uploadPost(
                            context = context,
                            post = localDraft,
                            trackAnalytics = false,
                            publish = false,
                            isRetry = true
                    )
                }
    }
}
