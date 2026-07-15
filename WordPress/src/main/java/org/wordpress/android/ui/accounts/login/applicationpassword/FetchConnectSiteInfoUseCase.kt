package org.wordpress.android.ui.accounts.login.applicationpassword

import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Bridges the Dispatcher/EventBus FETCH_CONNECT_SITE_INFO action into a coroutine so callers can
 * suspend on a single connect-site-info lookup.
 *
 * The use case registers itself on the Dispatcher when a request is dispatched and unregisters once
 * the result arrives or the coroutine is cancelled, so consumers don't need to manage its lifecycle.
 * Only one request is expected in flight at a time (the caller serializes calls).
 */
class FetchConnectSiteInfoUseCase @Inject constructor(
    private val dispatcher: Dispatcher
) {
    private var continuation: Continuation<ConnectSiteInfoPayload>? = null

    suspend fun fetchConnectSiteInfo(siteUrl: String): ConnectSiteInfoPayload =
        suspendCancellableCoroutine { cont ->
            continuation = cont
            dispatcher.register(this)
            cont.invokeOnCancellation {
                dispatcher.unregister(this)
                continuation = null
            }
            dispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(siteUrl))
        }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onConnectSiteInfoChecked(event: OnConnectSiteInfoChecked) {
        val cont = continuation ?: return
        continuation = null
        dispatcher.unregister(this)
        cont.resume(event.info)
    }
}
