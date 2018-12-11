package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms newSuggestDomainsAction EventBus event to a coroutine.
 */
class FetchDomainsUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val siteStore: SiteStore
) {
    private var continuation: Continuation<OnSuggestedDomains>? = null

    suspend fun fetchDomains(payload: SuggestDomainsPayload): OnSuggestedDomains {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSuggestedDomains(event: OnSuggestedDomains) {
        continuation?.resume(event)
        continuation = null
    }
}
