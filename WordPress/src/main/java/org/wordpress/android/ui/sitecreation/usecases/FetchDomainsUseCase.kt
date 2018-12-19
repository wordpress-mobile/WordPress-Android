package org.wordpress.android.ui.sitecreation.usecases

import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation

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

    suspend fun fetchDomains(payload: SuggestDomainsPayload): OnSuggestedDomains {
        return suspendCancellableCoroutine { cont ->
            pair = Pair(payload.query, cont)
            dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSuggestedDomains(event: OnSuggestedDomains) {
        pair?.let {
            if (event.query == it.first) {
                it.second.resume(event)
                pair = null
            }
        }
    }
}
