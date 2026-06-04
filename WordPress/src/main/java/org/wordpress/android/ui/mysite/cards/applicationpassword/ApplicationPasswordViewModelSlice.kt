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
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.CredentialsChangedNotifier
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
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

class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteStore: SiteStore,
    private val appLogWrapper: AppLogWrapper,
    private val wpApiClientProvider: WpApiClientProvider,
    private val applicationPasswordValidator: ApplicationPasswordValidator,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteXMLRPCClient: SiteXMLRPCClient,
    private val siteApiRestUrlRecoverer: SiteApiRestUrlRecoverer,
    private val dispatcher: Dispatcher,
    private val credentialsChangedNotifier: CredentialsChangedNotifier,
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

    // Single-flight guard: buildCard is invoked from onResume / refresh / onSitePicked, which can
    // fire close together. Without this, two coroutines both pass the "creds missing" check in
    // ApplicationPasswordsManager and issue two server-side mints. Worse, the 409 conflict handler
    // then deletes-and-recreates the winner's password, so the losing racer destroys working creds.
    private var buildJob: Job? = null

    fun buildCard(siteModel: SiteModel) {
        if (buildJob?.isActive == true) {
            appLogWrapper.d(
                AppLog.T.MAIN,
                "A_P: Skipping buildCard for ${siteModel.url} - previous run still in flight"
            )
            return
        }
        buildJob = scope.launch {
            val storedSite = siteStore.sites.firstOrNull { it.id == siteModel.id } ?: siteModel
            val hadCreds = !applicationPasswordLoginHelper.siteHasBadCredentials(storedSite)

            // Step 1: if we already have stored creds, validate them with Basic auth against the
            // direct host. This actually exercises the application password (unlike
            // WpApiClientProvider.getWpApiClient, which routes WPCom-flagged sites through the
            // bearer-token path and would not catch a revoked password).
            if (hadCreds) {
                when (applicationPasswordValidator.validate(storedSite)) {
                    ApplicationPasswordValidator.Outcome.Valid -> {
                        // Heal in the background so the card hides immediately on a slow network.
                        scope.launch { healApiRestUrlIfMissing(storedSite) }
                        handleValidAuth(storedSite)
                        return@launch
                    }
                    ApplicationPasswordValidator.Outcome.NetworkUnavailable -> {
                        // Don't punish flaky networks — leave the card hidden and try again next time.
                        uiModelMutable.postValue(null)
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: Validation network error for ${storedSite.url}")
                        return@launch
                    }
                    ApplicationPasswordValidator.Outcome.Invalid -> {
                        // Stored creds are stale (revoked, deleted, etc.) — clear them so the next
                        // mint creates fresh ones, and invalidate the cached client.
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: Stored creds invalid for ${storedSite.url}, clearing")
                        siteStore.deleteStoredApplicationPasswordCredentials(storedSite)
                        wpApiClientProvider.clearSelfHostedClient(storedSite.id)
                    }
                }
            }

            // Step 2: mint a fresh application password via the FluxC Jetpack tunnel. wordpress-rs
            // can't do this today — the WP.com REST proxy doesn't expose the application-passwords
            // endpoint under /wp/v2/sites/{id}/... (see Automattic/wordpress-rs#1350) — so FluxC's
            // Jetpack-tunnel client is the only working path for Atomic / Jetpack-WPCom-REST sites.
            val createResult = siteStore.createApplicationPassword(storedSite)
            if (!createResult.isError && createResult.credentials != null) {
                wpApiClientProvider.clearSelfHostedClient(storedSite.id)
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Headless mint succeeded for ${storedSite.url}")
                credentialsChangedNotifier.notifyChanged(storedSite.id)
                // The mint goes through the Jetpack tunnel and never runs discovery — without this
                // step, freshly minted Atomic sites end up with working creds but a NULL
                // wpApiRestUrl in the local DB. Run in the background so the card hides immediately.
                scope.launch { healApiRestUrlIfMissing(storedSite) }
                handleValidAuth(storedSite)
                return@launch
            }
            appLogWrapper.d(
                AppLog.T.MAIN,
                "A_P: Headless mint failed for ${storedSite.url} (notSupported=" +
                    "${createResult.error?.notSupported})"
            )

            // Step 3: mint failed. If we started with creds, show the reauth banner; otherwise the
            // standard "authenticate" card. Either way, discovery is required to populate the URL.
            if (hadCreds) {
                buildReauthenticationBanner(storedSite)
            } else {
                buildAuthenticationCard(storedSite)
            }
        }
    }

    private suspend fun healApiRestUrlIfMissing(site: SiteModel) {
        if (!site.wpApiRestUrl.isNullOrEmpty()) return
        siteApiRestUrlRecoverer.discoverApiRootUrl(site.url)?.let { apiRootUrl ->
            site.wpApiRestUrl = apiRootUrl
            siteApiRestUrlRecoverer.persistApiRootUrl(site.id, apiRootUrl)
        }
    }

    private fun handleValidAuth(site: SiteModel) {
        // Only true self-hosted sites need the XML-RPC fallback path — Atomic and Jetpack-WPCom-REST
        // sites talk REST end-to-end and don't need XML-RPC.
        if (!site.isUsingWpComRestApi && site.xmlRpcUrl.isNullOrEmpty()) {
            buildXmlRpcDisabledCard(site)
            attemptXmlRpcRediscovery(site)
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
                buildCard(site)
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
