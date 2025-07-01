package org.wordpress.android.ui.mysite.cards.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.api.kotlin.WpApiClient
import uniffi.wp_api.PostListParams
import uniffi.wp_api.WpAppNotifier
import uniffi.wp_api.WpAuthenticationProvider
import java.net.URL
import javax.inject.Inject

class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteSqlUtils: SiteSqlUtils,
    private val experimentalFeatures: ExperimentalFeatures,
    private val resourceProvider: ResourceProvider,
    private val appLogWrapper: AppLogWrapper
) {
    lateinit var scope: CoroutineScope

    private val siteURLCache = mutableMapOf<String, String>()

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
            dummyRequest(siteModel)
        }
    }

    private fun shouldBuildCard(): Boolean =
        experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE)

    private fun buildApplicationPasswordDiscovery(site: SiteModel) {
        // Check if the site URL is already cached
        val cachedValue = siteURLCache[site.url]
        if (cachedValue == null) {
            // No cached value, set to null until get a response
            uiModelMutable.postValue(null)
        } else {
            // If cached value is empty, it means the site has no authentication URL
            if (cachedValue.isEmpty()) {
                uiModelMutable.postValue(null)
            } else {
                postAuthenticationUrl(cachedValue)
            }
            return
        }

        scope.launch {
            // If the site is already authorized, no need to run the discovery
            val storedSite = siteSqlUtils.getSiteWithLocalId(site.localId())
            if (storedSite != null &&
                !storedSite.apiRestUsernameEncrypted.isNullOrEmpty() &&
                !storedSite.apiRestPasswordEncrypted.isNullOrEmpty()
                ) {
                return@launch
            }

            val authorizationUrlComplete = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                uiModelMutable.postValue(null)
                siteURLCache[site.url] = ""
            } else {
                postAuthenticationUrl(authorizationUrlComplete)
                siteURLCache[site.url] = authorizationUrlComplete
            }
        }
    }

    private fun dummyRequest(site: SiteModel) {
        if (site.apiRestUsernamePlain.isNullOrEmpty() || site.apiRestPasswordPlain.isNullOrEmpty()) {
            return
        }
        scope.launch {
            val authProvider = WpAuthenticationProvider.staticWithUsernameAndPassword(
                username = site.apiRestUsernamePlain, password = site.apiRestPasswordPlain
            )
            val apiRootUrl = URL("${site.url}/wp-json")
            val client = WpApiClient(
                wpOrgSiteApiRootUrl = apiRootUrl,
                authProvider = authProvider,
                appNotifier = object : WpAppNotifier {
                    override suspend fun requestedWithInvalidAuthentication() {
                        val message = UiStringText(resourceProvider.getString(R.string.application_password_invalid))
                        val button = UiStringText(resourceProvider.getString(R.string.sign_in))
                        val snackbarHolder = SnackbarMessageHolder(
                            message = message,
                            buttonTitle = button,
                            buttonAction = { reauthenticate(site) }
                        )
                        _onSnackbarMessage.postValue(Event(snackbarHolder))
                    }
                }
            )
            client.request { requestBuilder ->
                requestBuilder.posts().listWithEditContext(PostListParams())
            }
        }
    }

    private fun reauthenticate(site: SiteModel) {
        scope.launch {
            val authorizationUrlComplete = applicationPasswordLoginHelper.getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                appLogWrapper.e(AppLog.T.API, "Error getting authorization URL when reauthenticate")
            } else {
                onClick(authorizationUrlComplete) // Force the onClick to open reauthentication
            }
        }
    }

    private fun postAuthenticationUrl(authorizationUrlComplete: String) {
        uiModelMutable.postValue(
            MySiteCardAndItem.Card.QuickLinksItem(
                listOf(
                    QuickLinkItem(
                        label = UiString.UiStringRes(R.string.application_password_title),
                        icon = R.drawable.ic_lock_white_24dp,
                        onClick = ListItemInteraction.create { onClick(authorizationUrlComplete) }
                    )
                )
            )
        )
    }


    private fun onClick(authorizationUrlComplete: String) {
        _onNavigation.postValue(
            Event(
                SiteNavigationAction.OpenApplicationPasswordAuthentication(authorizationUrlComplete)
            )
        )
    }
}
