package org.wordpress.android.ui.uploads

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.NetworkUtilsWrapper
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
    /**
     * The Coroutine dispatcher used for querying in FluxC.
     */
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : CoroutineScope {
    override val coroutineContext: CoroutineContext get() = bgDispatcher

    fun queueUploadForAllSites() = launch {
        val sites = siteStore.sites
        // TODO there should be an actual queue instead of calling this directly
        upload(sites = sites)
    }

    fun queueUpload(site: SiteModel) = launch {
        // TODO there should be an actual queue instead of calling this directly
        upload(sites = listOf(site))
    }

    private suspend fun upload(sites: List<SiteModel>) = coroutineScope {
        sites.forEach { site ->
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                return@coroutineScope
            }

            yield()

            uploadSite(site = site)
        }
    }

    private fun uploadSite(site: SiteModel) {
        postStore.getLocalDraftPosts(site)
                .filterNot { UploadService.isPostUploadingOrQueued(it) }
                .forEach { localDraft ->
                    val intent = UploadService.getUploadPostServiceIntent(context, localDraft, false, false, true)
                    context.startService(intent)
                }
    }
}
