package org.wordpress.android.ui.domains.management

import uniffi.wp_api.AllDomainItem
import uniffi.wp_api.DomainListItemStatus
import uniffi.wp_api.DomainListItemStatusId
import uniffi.wp_api.DomainListItemStatusType
import uniffi.wp_api.DomainSubtype
import uniffi.wp_api.DomainSubtypeId

fun testDomainItem(
    domain: String = "",
    siteSlug: String = "",
    blogName: String = "",
    statusLabel: String = "Active",
    statusType: DomainListItemStatusType = DomainListItemStatusType.Success,
    subtypeId: DomainSubtypeId = DomainSubtypeId.DomainRegistration,
) = AllDomainItem(
    domain = domain,
    subtype = DomainSubtype(id = subtypeId, label = ""),
    blogId = 0u,
    blogName = blogName,
    siteSlug = siteSlug,
    autoRenewing = false,
    currentUserIsOwner = false,
    isDomainOnlySite = false,
    expiry = null,
    expired = false,
    primaryDomain = false,
    canSetAsPrimary = false,
    domainStatus = DomainListItemStatus(
        id = DomainListItemStatusId.Active,
        label = statusLabel,
        statusType = statusType,
        cta = null,
    ),
    subscriptionId = null,
    tags = emptyList(),
)
