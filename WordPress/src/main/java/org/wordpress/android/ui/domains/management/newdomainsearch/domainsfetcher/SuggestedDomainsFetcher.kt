package org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload
import org.wordpress.android.util.dispatchAndAwait
import javax.inject.Inject

class SuggestedDomainsFetcher @Inject constructor(
    private val dispatcher: Dispatcher
) {
    suspend fun fetch(action: Action<SuggestDomainsPayload>): OnSuggestedDomains = dispatcher.dispatchAndAwait(action)
}
