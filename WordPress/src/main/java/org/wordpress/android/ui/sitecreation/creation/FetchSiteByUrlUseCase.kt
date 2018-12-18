package org.wordpress.android.ui.sitecreation.creation

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnWPComSiteFetched
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms OnWPComSiteFetched EventBus event to a coroutine.
 */
class FetchSiteByUrlUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore
) {
    private var continuation: Continuation<OnWPComSiteFetched>? = null
    private var siteUrl: String? = null

    suspend fun fetchSite(siteUrl: String): OnWPComSiteFetched {
        this.siteUrl = siteUrl
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(siteUrl))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onWpComSiteFetched(event: OnWPComSiteFetched) {
        if (event.checkedUrl == siteUrl) {
            continuation?.resume(event)
            continuation = null
        }
    }
}
