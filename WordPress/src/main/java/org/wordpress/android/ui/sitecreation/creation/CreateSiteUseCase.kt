package org.wordpress.android.ui.sitecreation.creation

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import javax.inject.Inject
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class CreateSiteUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore
) {
    private var continuation: Continuation<OnNewSiteCreated>? = null

    suspend fun createSite(siteData: NewSiteCreationServiceData, languageWordPressId: String): OnNewSiteCreated {
        if (continuation != null) {
            throw IllegalStateException("Create site request has already been sent.")
        }
        return suspendCoroutine { cont ->
            val newSitePayload = NewSitePayload(
                    siteData.siteSlug,
                    siteData.siteTitle ?: "",
                    languageWordPressId,
                    PUBLIC,
                    false
            )
            continuation = cont
            dispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(newSitePayload))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onNewSiteCreated(event: OnNewSiteCreated) {
        continuation?.resume(event)
        continuation = null
    }
}
