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
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceData
import org.wordpress.android.util.UrlUtilsWrapper
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Transforms OnNewSiteCreated EventBus event to a coroutine.
 */
class CreateSiteUseCase @Inject constructor(
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val siteStore: SiteStore,
    private val urlUtilsWrapper: UrlUtilsWrapper
) {
    private var continuation: Continuation<OnNewSiteCreated>? = null

    suspend fun createSite(
        siteData: SiteCreationServiceData,
        languageWordPressId: String,
        siteVisibility: SiteVisibility = PUBLIC,
        dryRun: Boolean = false
    ): OnNewSiteCreated {
        if (continuation != null) {
            throw IllegalStateException("Create site request has already been sent.")
        }
        /*
         * To create a site with WordPress.com sub-domain, we need to pass the domain name without the "wordpress.com"
         * whereas to create a site with other domains, we need to pass the full url. This issue is addressed
         * in this use case since it's closest to the network layer.
         *
         * Ideally API wouldn't work like this or if it does FluxC is the one that handles the issue. However, at the
         * time of this comment, changing FluxC's Payload might end up affecting the old site creation flow,
         * so the workaround is applied here instead.
         */
        val domain = if (isWordPressComSubDomain(siteData.domain)) {
            urlUtilsWrapper.extractSubDomain(siteData.domain)
        } else siteData.domain
        return suspendCoroutine { cont ->
            val newSitePayload = NewSitePayload(
                    domain,
                    languageWordPressId,
                    siteVisibility,
                    siteData.segmentId,
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

fun isWordPressComSubDomain(url: String) = url.endsWith(".wordpress.com")
