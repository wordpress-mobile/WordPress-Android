package org.wordpress.android.ui.jetpack

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchJetpackCapabilitiesPayload
import org.wordpress.android.fluxc.store.SiteStore.OnJetpackCapabilitiesFetched
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FetchJetpackCapabilitiesUseCase @Inject constructor(
    @Suppress("unused") private val siteStore: SiteStore,
    private val dispatcher: Dispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var continuation: Continuation<OnJetpackCapabilitiesFetched>? = null

    suspend fun fetchJetpackCapabilities(remoteSiteId: Long): OnJetpackCapabilitiesFetched {
        return withContext(bgDispatcher) {
            if (continuation != null) {
                throw IllegalStateException("Request already in progress.")
            }

            dispatcher.register(this@FetchJetpackCapabilitiesUseCase)
            suspendCoroutine { cont ->
                val payload = FetchJetpackCapabilitiesPayload(remoteSiteId)
                continuation = cont
                dispatcher.dispatch(SiteActionBuilder.newFetchJetpackCapabilitiesAction(payload))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onJetpackCapabilitiesFetched(event: OnJetpackCapabilitiesFetched) {
        dispatcher.unregister(this@FetchJetpackCapabilitiesUseCase)
        continuation?.resume(event)
        continuation = null
    }
}
