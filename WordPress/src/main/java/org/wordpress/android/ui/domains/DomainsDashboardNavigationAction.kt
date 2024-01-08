package org.wordpress.android.ui.domains

import org.wordpress.android.fluxc.model.SiteModel

sealed class DomainsDashboardNavigationAction {
    data class GetDomain(val site: SiteModel) : DomainsDashboardNavigationAction()
    data class ClaimDomain(val site: SiteModel) : DomainsDashboardNavigationAction()
}
