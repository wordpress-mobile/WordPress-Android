package org.wordpress.android.ui.mysite.cards.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import androidx.annotation.VisibleForTesting
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.repositories.SiteAuthState
import org.wordpress.android.repositories.SiteProvisioningSource
import org.wordpress.android.repositories.SiteReadiness
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

/**
 * Renders the application-password card from the site's readiness. Credential
 * provisioning (validate / mint) now lives in [SiteProvisioningSource]; this
 * slice is a view over the auth slice of that state:
 *
 * - [SiteAuthState.Unprovisionable] → a re-authentication banner (creds went
 *   bad) or a first-time "authenticate" card, looked up lazily.
 * - provisioned (any non-[SiteReadiness.NeedsAuth] terminal state) → hidden,
 *   except true self-hosted sites missing an XML-RPC endpoint, which get the
 *   XML-RPC-disabled card plus a background rediscovery attempt.
 */
class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteStore: SiteStore,
    private val appLogWrapper: AppLogWrapper,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteXMLRPCClient: SiteXMLRPCClient,
    private val siteProvisioningSource: SiteProvisioningSource,
    private val dispatcher: Dispatcher,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    val uiModelMutable = MutableLiveData<MySiteCardAndItem?>()
    val uiModel: LiveData<MySiteCardAndItem?> = uiModelMutable

    private var collectJob: Job? = null
    private var currentSite: SiteModel? = null

    fun buildCard(siteModel: SiteModel) {
        collectJob?.cancel()
        currentSite = siteModel
        collectJob = scope.launch {
            siteProvisioningSource.stateFor(siteModel).collect { readiness ->
                // Bail if the user switched sites while suspended — postValue is not a
                // suspension point, so cancellation alone won't catch this.
                if (currentSite?.id != siteModel.id) return@collect
                renderCard(siteModel, readiness)
            }
        }
    }

    private suspend fun renderCard(site: SiteModel, readiness: SiteReadiness) {
        when (readiness) {
            is SiteReadiness.NeedsAuth -> when (val auth = readiness.auth) {
                is SiteAuthState.Unprovisionable ->
                    if (auth.hadCredentials) buildReauthenticationBanner(site) else buildAuthenticationCard(site)
                SiteAuthState.Provisioning -> {
                    // Mint in flight / transient validation error — hide and let the next run retry.
                    uiModelMutable.postValue(null)
                    appLogWrapper.d(AppLog.T.MAIN, "A_P: Provisioning in progress for ${site.url}")
                }
                SiteAuthState.Provisioned -> Unit // unreachable: Provisioned never wraps in NeedsAuth
            }
            // Any terminal provisioned state — the credentials are usable, so the only card left to
            // show is the self-hosted XML-RPC fallback. Capability outcome (Ready/Unreachable) is the
            // connectivity banner's concern, not this card's.
            SiteReadiness.Ready,
            SiteReadiness.Unreachable,
            SiteReadiness.TransientError -> handleProvisioned(site)
            SiteReadiness.Probing -> Unit // leave the card unchanged while the pipeline runs
        }
    }

    private fun handleProvisioned(site: SiteModel) {
        // Re-read the stored site so we see credentials/endpoints the pipeline just persisted.
        val storedSite = siteStore.sites.firstOrNull { it.id == site.id } ?: site
        // Only true self-hosted sites need the XML-RPC fallback path — Atomic and Jetpack-WPCom-REST
        // sites talk REST end-to-end and don't need XML-RPC.
        if (!storedSite.isUsingWpComRestApi && storedSite.xmlRpcUrl.isNullOrEmpty()) {
            buildXmlRpcDisabledCard(storedSite)
            attemptXmlRpcRediscovery(storedSite)
        } else {
            uiModelMutable.postValue(null)
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Hiding card for ${site.url} - authenticated")
        }
    }

    private suspend fun buildReauthenticationBanner(site: SiteModel) {
        when (val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)) {
            is ApplicationPasswordLoginHelper.DiscoveryResult.Authorized -> {
                uiModelMutable.postValue(
                    MySiteCardAndItem.Item.SingleActionCard(
                        textResource = R.string.application_password_reauthentication_banner,
                        imageResource = R.drawable.ic_notice_white_24dp,
                        onActionClick = { onClick(site, result.authorizationUrl) }
                    )
                )
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Showing reauthentication card for ${site.url}")
            }
            is ApplicationPasswordLoginHelper.DiscoveryResult.Failed -> {
                // TODO follow-up: surface result.userFacingMessage in the card (issue #22884).
                uiModelMutable.postValue(null)
                appLogWrapper.d(
                    AppLog.T.MAIN,
                    "A_P: Hiding reauthentication card for ${site.url} - bad discovery: ${result.userFacingMessage}"
                )
            }
        }
    }

    private suspend fun buildAuthenticationCard(site: SiteModel) {
        when (val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)) {
            is ApplicationPasswordLoginHelper.DiscoveryResult.Authorized -> {
                showApplicationPasswordCreateCard(site, result.authorizationUrl)
            }
            is ApplicationPasswordLoginHelper.DiscoveryResult.Failed -> {
                // TODO follow-up: surface result.userFacingMessage in the card (issue #22884).
                uiModelMutable.postValue(null)
                appLogWrapper.d(
                    AppLog.T.MAIN,
                    "A_P: Hiding card for ${site.url} - bad discovery: ${result.userFacingMessage}"
                )
            }
        }
    }

    private fun showApplicationPasswordCreateCard(site: SiteModel, alternativeUrl: String) {
        uiModelMutable.postValue(
            MySiteCardAndItem.Card.QuickLinksItem(
                listOf(
                    QuickLinkItem(
                        label = UiStringRes(R.string.application_password_title),
                        icon = R.drawable.ic_lock_white_24dp,
                        onClick = ListItemInteraction.create { onClick(site, alternativeUrl) }
                    )
                )
            )
        )
        appLogWrapper.d(AppLog.T.MAIN, "A_P: Showing card for ${site.url}")
    }

    private fun buildXmlRpcDisabledCard(site: SiteModel) {
        uiModelMutable.postValue(
            MySiteCardAndItem.Item.SingleActionCard(
                textResource = R.string.xmlrpc_disabled_card_text,
                imageResource = R.drawable.ic_notice_red_24dp,
                onActionClick = {
                    _onNavigation.postValue(
                        Event(
                            SiteNavigationAction
                                .OpenXmlRpcDisabledBottomSheet
                        )
                    )
                },
                centerImageVertically = true,
                showLearnMore = false
            )
        )
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Showing XML-RPC disabled card for ${site.url}"
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun attemptXmlRpcRediscovery(site: SiteModel) {
        scope.launch {
            try {
                val xmlRpcEndpoint = withContext(ioDispatcher) {
                    selfHostedEndpointFinder
                        .verifyOrDiscoverXMLRPCEndpoint(site.url)
                }

                // Verify with an authenticated call
                val result = withContext(ioDispatcher) {
                    siteXMLRPCClient.fetchSites(
                        xmlRpcEndpoint,
                        site.apiRestUsernamePlain,
                        site.apiRestPasswordPlain
                    )
                }
                if (result.isError) {
                    return@launch
                }

                site.xmlRpcUrl = xmlRpcEndpoint
                dispatcher.dispatch(
                    SiteActionBuilder.newUpdateSiteAction(site)
                )
                // Endpoint recovered — hide the XML-RPC-disabled card.
                uiModelMutable.postValue(null)
            } catch (
                @Suppress("SwallowedException")
                e: SelfHostedEndpointFinder.DiscoveryException
            ) {
                // XML-RPC rediscovery failed; card remains visible
            }
        }
    }

    private fun onClick(site: SiteModel, alternativeUrl: String) {
        _onNavigation.postValue(
            Event(
                SiteNavigationAction.OpenApplicationPasswordAutoAuthentication(site, alternativeUrl)
            )
        )
    }
}
