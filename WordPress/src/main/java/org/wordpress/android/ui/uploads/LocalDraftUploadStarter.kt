package org.wordpress.android.ui.uploads

import android.arch.lifecycle.Lifecycle.Event
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
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
 * The method [startAutoUploads] must be called once, preferably during app creation, for the auto-uploads to work.
 */
@Singleton
class LocalDraftUploadStarter @Inject constructor(
    /**
     * The Application context
     */
    private val context: Context,
    private val postStore: PostStore,
    private val siteStore: SiteStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
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
     * mLocalDraftUploadStarter.startAutoUploads(ProcessLifecycleOwner.get())
     * ```
     */
    fun startAutoUploads(processLifecycleOwner: ProcessLifecycleOwner) {
        // Since this class is meant to be a Singleton, it should be fine (I think) to use observeForever in here.
        // We're skipping the first emitted value because the processLifecycleObserver below will also trigger an
        // immediate upload.
        connectionStatus.skip(1).observeForever {
            queueUploadFromAllSites()
        }

        processLifecycleOwner.lifecycle.addObserver(processLifecycleObserver)
    }

    private fun queueUploadFromAllSites() = launch {
        val sites = siteStore.sites
        checkConnectionAndUpload(sites = sites)
    }

    /**
     * Upload all local drafts from the given [site].
     */
    fun queueUpload(site: SiteModel) = launch {
        checkConnectionAndUpload(sites = listOf(site))
    }

    private suspend fun checkConnectionAndUpload(sites: List<SiteModel>) = coroutineScope {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return@coroutineScope
        }

        sites.forEach {
            upload(scope = this, site = it)
        }
    }

    private fun upload(scope: CoroutineScope, site: SiteModel) = scope.launch(Dispatchers.IO) {
        postStore.getLocalDraftPosts(site)
                .filterNot { uploadServiceFacade.isPostUploadingOrQueued(it) }
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
