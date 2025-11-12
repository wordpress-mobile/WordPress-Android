package org.wordpress.android.ui.mysite.cards.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.Companion.ANDROID_JETPACK_CLIENT
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.Companion.ANDROID_WORDPRESS_CLIENT
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ApplicationPasswordCreateParams
import javax.inject.Inject
import kotlin.String

class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val wpApiClientProvider: WpApiClientProvider,
    private val siteStore: SiteStore,
    private val experimentalFeatures: ExperimentalFeatures,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appLogWrapper: AppLogWrapper,
    private val appSecrets: AppSecrets
) {
    lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    val uiModelMutable = MutableLiveData<MySiteCardAndItem.Card?>()
    val uiModel: LiveData<MySiteCardAndItem.Card?> = uiModelMutable

    fun buildCard(siteModel: SiteModel) {
        if (shouldBuildCard()) {
            buildApplicationPasswordDiscovery(siteModel)
        } else {
            // Hide the card when feature flag is disabled to prevent stale UI state
            uiModelMutable.postValue(null)
        }
    }

    private fun shouldBuildCard(): Boolean =
        experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE)

    private fun buildApplicationPasswordDiscovery(site: SiteModel) {
        scope.launch {
            // If the site is already authorized, no need to run the discovery
            val storedSite = siteStore.sites.firstOrNull { it.id == site.id }
            if (storedSite != null && !applicationPasswordLoginHelper.siteHasBadCredentials(site)) {
                uiModelMutable.postValue(null)
                return@launch
            }

            val authorizationUrlComplete = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                uiModelMutable.postValue(null)
            } else {
                postAuthenticationUrl(site)
            }
        }
    }

    private fun postAuthenticationUrl(site: SiteModel) {
        uiModelMutable.postValue(
            MySiteCardAndItem.Card.QuickLinksItem(
                listOf(
                    QuickLinkItem(
                        label = UiString.UiStringRes(R.string.application_password_title),
                        icon = R.drawable.ic_lock_white_24dp,
                        onClick = ListItemInteraction.create { onClick(site) }
                    )
                )
            )
        )
    }


    private fun onClick(site: SiteModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(
                site = site,
            )
            val appName = if (buildConfigWrapper.isJetpackApp) {
                ANDROID_JETPACK_CLIENT
            } else {
                ANDROID_WORDPRESS_CLIENT
            }
            val userIdResponse = client.request { requestBuilder ->
                requestBuilder.applicationPasswords().createForCurrentUser(
                    params = ApplicationPasswordCreateParams(
                        appId = null,
                        name = "$appName-${System.currentTimeMillis()}"
                    )
                )
            }
            when (userIdResponse) {
                is WpRequestResult.Success -> {
                    val name = userIdResponse.response.data.name
                    val password = userIdResponse.response.data.password
                    val apiRootUrl = wpApiClientProvider.getApiRootUrlFrom(site)
                    applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(
                        UriLogin(
                            siteUrl = site.url,
                            user = name,
                            password = password,
                            apiRootUrl = apiRootUrl
                        )
                    )
                    _onSnackbarMessage.postValue(
                        Event(
                            SnackbarMessageHolder(
                                UiString.UiStringResWithParams(R.string.application_password_credentials_stored,
                                    UiString.UiStringText(site.url)
                                )
                            )
                        )
                    )
                }

                else -> {
                    appLogWrapper.e(AppLog.T.API, "Error creating application password")
                    _onSnackbarMessage.postValue(
                        Event(
                            SnackbarMessageHolder(
                                UiString.UiStringResWithParams(
                                    R.string.application_password_credentials_storing_error,
                                    UiString.UiStringText(site.url)
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}
