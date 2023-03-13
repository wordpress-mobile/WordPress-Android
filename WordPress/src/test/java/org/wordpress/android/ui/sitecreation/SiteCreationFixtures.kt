package org.wordpress.android.ui.sitecreation

import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotInLocalDb
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.theme.defaultTemplateSlug

const val SUB_DOMAIN = "test"
const val URL = "$SUB_DOMAIN.wordpress.com"
val FREE_DOMAIN = DomainModel(URL, true, "", 1)

const val SITE_REMOTE_ID = 1L
private const val SITE_LOCAL_ID = 1

val SITE_CREATION_STATE = SiteCreationState(
    segmentId = 1,
    siteDesign = defaultTemplateSlug,
    domain = FREE_DOMAIN,
    remoteSiteId = SITE_REMOTE_ID,
)

val FETCH_SUCCESS = OnSiteChanged(1)
val FETCH_ERROR = OnSiteChanged(0).apply { error = SiteError(GENERIC_ERROR) }

val RESULT_CREATED = NotInLocalDb(SITE_REMOTE_ID, false)
val RESULT_COMPLETED = Completed(SITE_LOCAL_ID, false, URL)

val SERVICE_SUCCESS = SiteCreationServiceState(SUCCESS, Pair(SITE_REMOTE_ID, URL))
val SERVICE_ERROR = SiteCreationServiceState(FAILURE, SiteCreationServiceState(CREATE_SITE))
