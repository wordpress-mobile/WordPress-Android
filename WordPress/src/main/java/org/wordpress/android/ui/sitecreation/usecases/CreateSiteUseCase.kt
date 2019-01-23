package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility.PUBLIC
import org.wordpress.android.ui.sitecreation.services.NewSiteCreationServiceData
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Transforms OnNewSiteCreated EventBus event to a coroutine.
 */
class CreateSiteUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore
) {
    private var continuation: Continuation<OnNewSiteCreated>? = null

    suspend fun createSite(
        siteData: NewSiteCreationServiceData,
        languageWordPressId: String,
        siteVisibility: SiteVisibility = PUBLIC,
        dryRun: Boolean = false
    ): OnNewSiteCreated {
        if (continuation != null) {
            throw IllegalStateException("Create site request has already been sent.")
        }
        return suspendCoroutine { cont ->
            val newSitePayload = NewSitePayload(
                    siteData.domain,
                    siteData.siteTitle ?: "",
                    languageWordPressId,
                    siteVisibility,
                    siteData.verticalId,
                    siteData.segmentId,
                    siteData.siteTagLine,
                    dryRun
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
