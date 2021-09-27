package org.wordpress.android.ui.domains

import org.wordpress.android.fluxc.model.SiteModel

sealed class DomainsNavigationEvents {
    data class GetDomain(val site: SiteModel) : DomainsNavigationEvents()
    data class OpenManageDomains(val url: String) : DomainsNavigationEvents()
}
