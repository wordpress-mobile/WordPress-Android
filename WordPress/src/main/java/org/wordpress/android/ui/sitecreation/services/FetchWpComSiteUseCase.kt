package org.wordpress.android.ui.sitecreation.services

import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val FETCH_SITE_BASE_RETRY_DELAY_IN_MILLIS = 1000L
private const val DEFAULT_NUMBER_OF_RETRIES = 3

/**
 * Transforms FETCH_SITE -> UPDATE_SITE fluxC request-response pair to a coroutine.
 */
class FetchWpComSiteUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore
) {
    private var continuation: Continuation<OnSiteChanged>? = null

    suspend fun fetchSiteWithRetry(
        remoteSiteId: Long,
        numberOfRetries: Int = DEFAULT_NUMBER_OF_RETRIES
    ): OnSiteChanged {
        repeat(numberOfRetries) { attemptNumber ->
            val onSiteFetched = fetchSite(remoteSiteId)
            if (!onSiteFetched.isError) {
                // return only when the request succeeds
                return onSiteFetched
            }
            // linear backoff
            delay((attemptNumber + 1) * FETCH_SITE_BASE_RETRY_DELAY_IN_MILLIS) // +1 -> starts from 0
        }
        // return the last attempt no matter the result (success/error)
        return fetchSite(remoteSiteId)
    }

    private suspend fun fetchSite(siteId: Long): OnSiteChanged {
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(createWpComSiteModel(siteId)))
        }
    }

    private fun createWpComSiteModel(siteId: Long): SiteModel {
        val siteModel = SiteModel()
        siteModel.siteId = siteId
        siteModel.setIsWPCom(true)
        return siteModel
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onSiteFetched(event: OnSiteChanged) {
        continuation?.resume(event)
        continuation = null
    }
}
