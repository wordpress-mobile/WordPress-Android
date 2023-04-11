package org.wordpress.android.ui.sitecreation

import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.ui.domains.DomainRegistrationCheckoutWebViewActivity
import org.wordpress.android.ui.domains.DomainRegistrationCompletedEvent
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Completed
import org.wordpress.android.ui.sitecreation.SiteCreationResult.Created
import org.wordpress.android.ui.sitecreation.SiteCreationResult.CreatedButNotFetched
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.CREATE_SITE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.FAILURE
import org.wordpress.android.ui.sitecreation.services.SiteCreationServiceState.SiteCreationStep.SUCCESS
import org.wordpress.android.ui.sitecreation.theme.defaultTemplateSlug

const val SUB_DOMAIN = "test"
const val URL = "$SUB_DOMAIN.wordpress.com"
const val URL_CUSTOM = "$SUB_DOMAIN.host.com"
const val SITE_SLUG = "${SUB_DOMAIN}host0.wordpress.com"
val FREE_DOMAIN = DomainModel(URL, true, "", 1, false)
val PAID_DOMAIN = DomainModel(URL_CUSTOM, false, "$1", 2, true)

const val SITE_REMOTE_ID = 1L

val SITE_CREATION_STATE = SiteCreationState(
    segmentId = 1,
    siteDesign = defaultTemplateSlug,
    domain = FREE_DOMAIN,
)

val SITE_MODEL = SiteModel().apply { siteId = SITE_REMOTE_ID; url = SITE_SLUG }

val CHECKOUT_DETAILS = DomainRegistrationCheckoutWebViewActivity.OpenCheckout.CheckoutDetails(SITE_MODEL, SITE_SLUG)
val CHECKOUT_EVENT = DomainRegistrationCompletedEvent(URL_CUSTOM, "email@host.com")

val FETCH_SUCCESS = OnSiteChanged(1)
val FETCH_ERROR = OnSiteChanged(0).apply { error = SiteError(GENERIC_ERROR) }

val CART_SUCCESS = OnShoppingCartCreated(mock<CreateShoppingCartResponse>())
val CART_ERROR = OnShoppingCartCreated(mock<CreateShoppingCartError>())

val RESULT_CREATED = mock<Created>()
val RESULT_NOT_IN_LOCAL_DB = CreatedButNotFetched.NotInLocalDb(SITE_MODEL)
val RESULT_IN_CART = CreatedButNotFetched.InCart(SITE_MODEL)
val RESULT_COMPLETED = Completed(SITE_MODEL)

val SERVICE_SUCCESS = SiteCreationServiceState(SUCCESS, Pair(SITE_REMOTE_ID, SITE_SLUG))
val SERVICE_ERROR = SiteCreationServiceState(FAILURE, SiteCreationServiceState(CREATE_SITE))
