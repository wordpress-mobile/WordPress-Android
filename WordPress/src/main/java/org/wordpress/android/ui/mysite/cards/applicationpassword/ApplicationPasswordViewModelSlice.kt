package org.wordpress.android.ui.mysite.cards.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
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
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

/**
 * Renders the application-password card from the site's readiness. All the
 * provisioning mechanics — validate, mint, REST-root recovery, XML-RPC
 * recovery — live in [SiteProvisioningSource]; this slice is a thin view over
 * the result:
 *
 * - [SiteAuthState.Unprovisionable] → a re-authentication banner (creds went
 *   bad) or a first-time "authenticate" card, looked up lazily.
 * - provisioned (any non-[SiteReadiness.NeedsAuth] terminal state) → hidden,
 *   except true self-hosted sites whose XML-RPC endpoint the pipeline couldn't
 *   recover, which get the XML-RPC-disabled card.
 */
class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteStore: SiteStore,
    private val appLogWrapper: AppLogWrapper,
    private val siteProvisioningSource: SiteProvisioningSource,
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
        // Read fresh: the pipeline's parallel XML-RPC branch may have just recovered the endpoint.
        val storedSite = siteStore.getSiteByLocalId(site.id) ?: site
        // Only true self-hosted sites need XML-RPC, and a missing endpoint alone isn't proof it's off —
        // recovery also fails transiently (e.g. a 429). Warn only when the pipeline reached a definitive
        // negative, so a throttled site doesn't get a false "XML-RPC Disabled".
        if (!storedSite.isUsingWpComRestApi &&
            storedSite.xmlRpcUrl.isNullOrEmpty() &&
            siteProvisioningSource.isXmlRpcUnavailable(storedSite.id)
        ) {
            buildXmlRpcDisabledCard(storedSite)
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
            is ApplicationPasswordLoginHelper.DiscoveryResult.WpComSite -> {
                uiModelMutable.postValue(null)
                appLogWrapper.d(
                    AppLog.T.MAIN,
                    "A_P: Hiding reauthentication card for ${site.url} - WordPress.com site"
                )
            }
            is ApplicationPasswordLoginHelper.DiscoveryResult.Failed -> handleFailedDiscovery(site, result)
        }
    }

    private suspend fun buildAuthenticationCard(site: SiteModel) {
        when (val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)) {
            is ApplicationPasswordLoginHelper.DiscoveryResult.Authorized -> {
                showApplicationPasswordCreateCard(site, result.authorizationUrl)
            }
            is ApplicationPasswordLoginHelper.DiscoveryResult.WpComSite -> {
                uiModelMutable.postValue(null)
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Hiding card for ${site.url} - WordPress.com site")
            }
            is ApplicationPasswordLoginHelper.DiscoveryResult.Failed -> handleFailedDiscovery(site, result)
        }
    }

    /**
     * Discovery couldn't produce an authorization URL, so neither the create nor the re-authenticate
     * card can be built. A private site is the one cause we can name: its Privacy gate answers the
     * anonymous discovery request with a 403, and without saying so the user sees nothing at all
     * while their credentials stay broken. Every other cause still hides the card pending #22884.
     */
    private fun handleFailedDiscovery(
        site: SiteModel,
        result: ApplicationPasswordLoginHelper.DiscoveryResult.Failed,
    ) {
        if (result.reason == ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason.PrivateSite) {
            uiModelMutable.postValue(
                MySiteCardAndItem.Item.SingleActionCard(
                    textResource = R.string.application_password_private_site_card,
                    // Hidden by centerText — the card is a notice with nothing to tap.
                    imageResource = R.drawable.ic_notice_white_24dp,
                    onActionClick = { },
                    showLearnMore = false,
                    centerText = true,
                )
            )
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Showing private-site card for ${site.url}")
            return
        }
        // TODO follow-up: surface result.userFacingMessage in the card (issue #22884).
        uiModelMutable.postValue(null)
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Hiding card for ${site.url} - bad discovery: ${result.userFacingMessage}"
        )
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

    private fun onClick(site: SiteModel, alternativeUrl: String) {
        _onNavigation.postValue(
            Event(
                SiteNavigationAction.OpenApplicationPasswordAutoAuthentication(site, alternativeUrl)
            )
        )
    }
}
