package org.wordpress.android.ui.mysite.cards.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import javax.inject.Inject

class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteStore: SiteStore,
    private val experimentalFeatures: ExperimentalFeatures,
    private val appLogWrapper: AppLogWrapper,
    private val wpApiClientProvider: WpApiClientProvider,
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

    fun buildCard(siteModel: SiteModel) {
        val storedSite = siteStore.sites.firstOrNull { it.id == siteModel.id }

        // For sites using application passwords, validate credentials
        if (storedSite != null && storedSite.isUsingSelfHostedRestApi) {
            validateCredentialsAndBuildCard(storedSite)
            return
        }

        if (shouldBuildCard()) {
            buildApplicationPasswordDiscovery(siteModel)
        } else {
            // Hide the card when feature flag is disabled to prevent stale UI state
            uiModelMutable.postValue(null)
        }
    }

    @Suppress("TooGenericExceptionCaught", "LongMethod")
    private fun validateCredentialsAndBuildCard(site: SiteModel) {
        appLogWrapper.d(AppLog.T.MAIN, "A_P: Validating credentials for ${site.url}")
        appLogWrapper.d(AppLog.T.MAIN, "A_P:   isUsingSelfHostedRestApi: ${site.isUsingSelfHostedRestApi}")
        appLogWrapper.d(AppLog.T.MAIN, "A_P:   hasApiRestUsername: ${!site.apiRestUsernamePlain.isNullOrEmpty()}")
        appLogWrapper.d(AppLog.T.MAIN, "A_P:   hasApiRestPassword: ${!site.apiRestPasswordPlain.isNullOrEmpty()}")
        appLogWrapper.d(AppLog.T.MAIN, "A_P:   wpApiRestUrl: ${site.wpApiRestUrl}")
        appLogWrapper.d(AppLog.T.MAIN, "A_P:   origin: ${site.origin}")

        // If credentials are missing, show reauthentication banner immediately
        if (applicationPasswordLoginHelper.siteHasBadCredentials(site)) {
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Credentials missing for ${site.url}, showing banner")
            buildReauthenticationBanner(site)
            return
        }

        // Validate credentials by making a simple API call
        scope.launch {
            try {
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Making API call to validate credentials")
                val client = wpApiClientProvider.getWpApiClient(site)
                val response = client.request { requestBuilder ->
                    requestBuilder.users().retrieveMeWithViewContext()
                }
                appLogWrapper.d(AppLog.T.MAIN, "A_P: API response type: ${response::class.simpleName}")
                when (response) {
                    is WpRequestResult.Success -> {
                        // Credentials are valid, hide the card
                        uiModelMutable.postValue(null)
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: Credentials valid for ${site.url}")
                    }
                    is WpRequestResult.WpError -> {
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: WpError for ${site.url}: ${response.response}")
                        buildReauthenticationBanner(site)
                    }
                    is WpRequestResult.UnknownError -> {
                        appLogWrapper.d(
                            AppLog.T.MAIN,
                            "A_P: UnknownError for ${site.url}: code=${response.statusCode}, msg=${response.response}"
                        )
                        buildReauthenticationBanner(site)
                    }
                    is WpRequestResult.RequestExecutionFailed -> {
                        val isTimeout = response.reason is RequestExecutionErrorReason.HttpTimeoutError
                        if (isTimeout) {
                            appLogWrapper.d(AppLog.T.MAIN, "A_P: Request timed out for ${site.url}")
                        } else {
                            appLogWrapper.d(
                                AppLog.T.MAIN,
                                "A_P: RequestExecutionFailed for ${site.url}: " +
                                    "reason=${response.reason}, statusCode=${response.statusCode}"
                            )
                        }
                        // Don't show reauthentication banner for timeouts - it's likely a network issue
                        if (!isTimeout) {
                            buildReauthenticationBanner(site)
                        } else {
                            uiModelMutable.postValue(null)
                        }
                    }
                    else -> {
                        // Credentials are invalid, show reauthentication banner
                        appLogWrapper.d(AppLog.T.MAIN, "A_P: Other error for ${site.url}: $response")
                        buildReauthenticationBanner(site)
                    }
                }
            } catch (e: Exception) {
                appLogWrapper.e(
                    AppLog.T.MAIN,
                    "A_P: Exception validating credentials for ${site.url}: ${e::class.simpleName}: ${e.message}"
                )
                uiModelMutable.postValue(null)
            }
        }
    }

    private fun shouldBuildCard(): Boolean =
        experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE)

    private fun buildReauthenticationBanner(site: SiteModel) {
        scope.launch {
            val authorizationUrlComplete = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                uiModelMutable.postValue(null)
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Hiding reauthentication card for ${site.url} - bad discovery")
            } else {
                showReauthenticationCard(site, authorizationUrlComplete)
            }
        }
    }

    private fun showReauthenticationCard(site: SiteModel, alternativeUrl: String) {
        uiModelMutable.postValue(
            MySiteCardAndItem.Item.SingleActionCard(
                textResource = R.string.application_password_reauthentication_banner,
                imageResource = R.drawable.ic_notice_white_24dp,
                onActionClick = { onClick(site, alternativeUrl) }
            )
        )
        appLogWrapper.d(AppLog.T.MAIN, "A_P: Showing reauthentication card for ${site.url}")
    }

    private fun buildApplicationPasswordDiscovery(site: SiteModel) {
        scope.launch {
            // If the site is already authorized, no need to run the discovery
            val storedSite = siteStore.sites.firstOrNull { it.id == site.id }
            if (storedSite != null && !applicationPasswordLoginHelper.siteHasBadCredentials(storedSite)) {
                uiModelMutable.postValue(null)
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Hiding card for ${site.url} - authenticated")
                return@launch
            }

            val authorizationUrlComplete = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                uiModelMutable.postValue(null)
                appLogWrapper.d(AppLog.T.MAIN, "A_P: Hiding card for ${site.url} - bad discovery")
            } else {
                showApplicationPasswordCreateCard(site, authorizationUrlComplete)
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


    private fun onClick(site: SiteModel, alternativeUrl: String) {
        _onNavigation.postValue(
            Event(
                SiteNavigationAction.OpenApplicationPasswordAutoAuthentication(site, alternativeUrl)
            )
        )
    }
}
