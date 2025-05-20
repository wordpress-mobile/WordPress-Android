package org.wordpress.android.ui.mysite.cards.applicationpassword

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject
import javax.inject.Named

class ApplicationPasswordViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteSqlUtils: SiteSqlUtils,
    private val wpLoginClient: WpLoginClient,
    private val appLogWrapper: AppLogWrapper,
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

    var buildCard = true

    fun buildCard(siteModel: SiteModel) {
        // This is hidden for regular users.
        // After enabling it, please remove the Suppress annotation for buildCard and buildApplicationPasswordDiscovery
        if (buildCard) {
            buildApplicationPasswordDiscovery(siteModel)
        }
    }

    private fun buildApplicationPasswordDiscovery(site: SiteModel) {
        // Check if the site URL is already cached
        val cachedValue = siteURLCache[site.url]
        if (cachedValue == null) {
            // No cached value, set to null until get a response
            uiModelMutable.postValue(null)
            Log.d("DEBUG_TAG", "Cache miss for ${site.url}, set to null")
        } else {
            // If cached value is empty, it means the site has no authentication URL
            if (cachedValue.isEmpty()) {
                uiModelMutable.postValue(null)
                Log.d("DEBUG_TAG", "Cache empty for ${site.url}, set to null")
            } else {
                postAuthenticationUrl(cachedValue)
                Log.d("DEBUG_TAG", "Cache hit for ${site.url}, set to $cachedValue")
            }
            return
        }

        scope.launch {
            // If the site is already authorized, no need to run the discovery
            val storedSite = siteSqlUtils.getSiteWithLocalId(site.localId())
            if (storedSite != null &&
                !storedSite.apiRestUsername.isNullOrEmpty() && !storedSite.apiRestPassword.isNullOrEmpty()) {
                Log.d("DEBUG_TAG", "Site already authenticated ${site.url}, no need to run discovery")
                return@launch
            }

            val authorizationUrlComplete = getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                uiModelMutable.postValue(null)
                siteURLCache[site.url] = ""
                Log.d("DEBUG_TAG", "Site not authenticable ${site.url}, set to null")
            } else {
                postAuthenticationUrl(authorizationUrlComplete)
                siteURLCache[site.url] = authorizationUrlComplete
                Log.d("DEBUG_TAG", "Site authenticable ${site.url}, set to $authorizationUrlComplete")
            }

            Log.d("DEBUG_TAG", "----------------------")
        }
    }

    private fun postAuthenticationUrl(authorizationUrlComplete: String) {
        uiModelMutable.postValue(
            MySiteCardAndItem.Card.QuickLinksItem(
                listOf(
                    QuickLinkItem(
                        label = UiString.UiStringRes(R.string.application_password_title),
                        icon = R.drawable.ic_lock_white_24dp,
                        onClick = onClick(authorizationUrlComplete)
                    )
                )
            )
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun getAuthorizationUrlComplete(siteUrl: String): String = withContext(bgDispatcher) {
        try {
            getAuthorizationUrlCompleteInternal(siteUrl)
        } catch (throwable: Throwable) {
            Log.d("DEBUG_TAG", "Error 1 on $siteUrl")
            handleAuthenticationDiscoveryError(siteUrl, throwable)
        }
    }

    private fun handleAuthenticationDiscoveryError(siteUrl: String, throwable: Throwable): String {
        appLogWrapper.e(AppLog.T.API, "WP_RS: Error during API discovery for $siteUrl - ${throwable.message}")
        AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        return ""
    }

    private suspend fun getAuthorizationUrlCompleteInternal(siteUrl: String): String = withContext(bgDispatcher) {
        when (val urlDiscoveryResult = wpLoginClient.apiDiscovery(siteUrl)) {
            is ApiDiscoveryResult.Success -> {
                val authorizationUrl = urlDiscoveryResult.success.applicationPasswordsAuthenticationUrl.url()
                val authorizationUrlComplete =
                    applicationPasswordLoginHelper.appendParamsToRestAuthorizationUrl(authorizationUrl)
                Log.d("DEBUG_TAG", "Got URL for site $siteUrl: $authorizationUrl")
                Log.d("WP_RS", "Found authorization for $siteUrl URL: $authorizationUrlComplete")
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
                authorizationUrlComplete
            }

            is ApiDiscoveryResult.FailureFetchAndParseApiRoot -> {
                Log.d("DEBUG_TAG", "Failed discovery for $siteUrl")
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFetchAndParseApiRoot"))
            }

            is ApiDiscoveryResult.FailureFindApiRoot -> {
                Log.d("DEBUG_TAG", "Failed discovery for $siteUrl")
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFindApiRoot"))
            }

            is ApiDiscoveryResult.FailureParseSiteUrl -> {
                Log.d("DEBUG_TAG", "Failed discovery for $siteUrl")
                handleAuthenticationDiscoveryError(siteUrl, urlDiscoveryResult.error)
            }
        }
    }

    private fun onClick(authorizationUrlComplete: String) = ListItemInteraction.create {
        _onNavigation.postValue(
            Event(
                SiteNavigationAction.OpenApplicationPasswordAuthentication(authorizationUrlComplete)
            )
        )
    }
}
