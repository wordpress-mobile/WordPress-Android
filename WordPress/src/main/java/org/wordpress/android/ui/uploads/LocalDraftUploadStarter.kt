package org.wordpress.android.ui.uploads

import android.arch.lifecycle.Lifecycle.Event
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.OnLifecycleEvent
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
 * Provides a way to find and upload all local drafts.
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
    connectionStatus: LiveData<ConnectionStatus>
) : CoroutineScope {
    private val job = Job()

    override val coroutineContext: CoroutineContext get() = job + bgDispatcher

    /**
     * The hook for making this class automatically launch uploads whenever the app is placed in the foreground.
     *
     * This must be attached during [org.wordpress.android.WordPress]' creation like so:
     *
     * ```
     * ProcessLifecycleOwner.get().getLifecycle().addObserver(mLocalDraftUploadStarter.getProcessLifecycleObserver());
     * ```
     */
    val processLifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Event.ON_START)
        fun onAppComesFromBackground() {
            queueUploadForAllSites()
        }
    }

    init {
        // Since this class is meant to be a Singleton, it should be fine (I think) to use observeForever in here.
        connectionStatus.skip(1).observeForever {
            queueUploadForAllSites()
        }
    }

    private fun queueUploadForAllSites() = launch {
        val sites = siteStore.sites
        upload(sites = sites)
    }

    fun queueUpload(site: SiteModel) = launch {
        upload(sites = listOf(site))
    }

    private suspend fun upload(sites: List<SiteModel>) = coroutineScope {
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
