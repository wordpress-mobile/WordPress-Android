package org.wordpress.android.ui.sitecreation.usecases

import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val FETCH_DOMAINS_SHOULD_INCLUDE_DOT_BLOG_VENDOR = false
private const val FETCH_DOMAINS_SIZE = 20

/**
 * Transforms newSuggestDomainsAction EventBus event to a coroutine.
 *
 * The client may dispatch multiple requests, but we want to accept only the latest one and ignore all others.
 * We can't rely just on job.cancel() as the OnSuggestedDomains may have already been dispatched and FluxC will
 * return a result.
 */
class FetchDomainsUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val siteStore: SiteStore
) {
    /**
     * Query - Continuation pair
     */
    private var pair: Pair<String, Continuation<OnSuggestedDomains>>? = null

    suspend fun fetchDomains(
        query: String,
        segmentId: Long? = null,
        includeVendorDot: Boolean = FETCH_DOMAINS_SHOULD_INCLUDE_DOT_BLOG_VENDOR,
        includeDotBlog: Boolean = false,
        size: Int = FETCH_DOMAINS_SIZE
    ): OnSuggestedDomains {
        /**
         * Depending on the payload the server may override the following values. Setting this values here to get
         * reasonable results in case SKIP button is pressed ("default" template)
         */
        val payload = SuggestDomainsPayload(
            query = query,
            segmentId = segmentId,
            quantity = size,
            includeVendorDot = includeVendorDot,
            includeWordpressCom = true,
            onlyWordpressCom = true,
            includeDotBlogSubdomain = includeDotBlog
        )

        return suspendCancellableCoroutine { cont ->
            pair = Pair(payload.query, cont)
            dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onSuggestedDomains(event: OnSuggestedDomains) {
        pair?.let {
            if (event.query == it.first) {
                it.second.resume(event)
                pair = null
            }
        }
    }
}
