package org.wordpress.android.ui.sitecreation.creation

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transforms OnWPComSiteFetched EventBus event to a coroutine.
 */
class FetchSiteUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore
) {
    private var continuation: Continuation<OnSiteChanged>? = null

    suspend fun fetchSite(siteId: Long, isWpCom: Boolean): OnSiteChanged {
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(createWpComSiteModel(siteId, isWpCom)))
        }
    }

    private fun createWpComSiteModel(siteId: Long, isWpCom: Boolean): SiteModel {
        val siteModel = SiteModel()
        siteModel.siteId = siteId
        siteModel.setIsWPCom(isWpCom)
        return siteModel
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSiteFetched(event: OnSiteChanged) {
        continuation?.resume(event)
        continuation = null
    }
}
