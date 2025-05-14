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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject
import javax.inject.Named


class ApplicationPasswordViewModelSlice @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteSqlUtils: SiteSqlUtils,
    private val wpLoginClient: WpLoginClient,
) {
    lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    fun site() = selectedSiteRepository.getSelectedSite()

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    val _uiModel = MutableLiveData<MySiteCardAndItem.Card?>()
    val uiMode: LiveData<MySiteCardAndItem.Card?> = _uiModel

    fun buildCard(siteModel: SiteModel) {
        buildApplicationPasswordDiscovery(siteModel)
    }

    private fun buildApplicationPasswordDiscovery(site: SiteModel) {
        scope.launch {
            // If the site is already authorized, no need to run the discovery
            val storedSite = siteSqlUtils.getSiteWithLocalId(site.localId())
            if (storedSite != null &&
                !storedSite.apiRestUsername.isNullOrEmpty() && !storedSite.apiRestPassword.isNullOrEmpty()) {
                return@launch
            }

            val authorizationUrlComplete = getAuthorizationUrlComplete(site.url)
            if (authorizationUrlComplete.isEmpty()) {
                _uiModel.postValue(null)
            } else {
                _uiModel.postValue(
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
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun getAuthorizationUrlComplete(siteUrl: String): String = withContext(bgDispatcher) {
        try {
            getAuthorizationUrlCompleteInternal(siteUrl)
        } catch (throwable: Throwable) {
            handleAuthenticationDiscoveryError(siteUrl, throwable)
        }
    }

    private fun handleAuthenticationDiscoveryError(siteUrl: String, throwable: Throwable): String {
        Log.e("WP_RS", "VM: Error during API discovery for $siteUrl", throwable)
        AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
        return ""
    }

    private suspend fun getAuthorizationUrlCompleteInternal(siteUrl: String): String = withContext(bgDispatcher) {
        when (val urlDiscoveryResult = wpLoginClient.apiDiscovery(siteUrl)) {
            is ApiDiscoveryResult.Success -> {
                val authorizationUrl = urlDiscoveryResult.success.applicationPasswordsAuthenticationUrl.url()
                val authorizationUrlComplete =
                    applicationPasswordLoginHelper.appendParamsToRestAuthorizationUrl(authorizationUrl)
                Log.d("WP_RS", "Found authorization for $siteUrl URL: $authorizationUrlComplete")
                AnalyticsTracker.track(Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
                authorizationUrlComplete
            }

            is ApiDiscoveryResult.FailureFetchAndParseApiRoot ->
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFetchAndParseApiRoot"))

            is ApiDiscoveryResult.FailureFindApiRoot ->
                handleAuthenticationDiscoveryError(siteUrl, Exception("FailureFindApiRoot"))

            is ApiDiscoveryResult.FailureParseSiteUrl ->
                handleAuthenticationDiscoveryError(siteUrl, urlDiscoveryResult.error)
        }
    }

    private fun onClick(authorizationUrlComplete: String) = ListItemInteraction.create {
        _onNavigation.postValue(
            Event(
                SiteNavigationAction.OpenApplicationPasswordAuthentication(authorizationUrlComplete)
            )
        )
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }

    data class MoreClickWithTask(
        val listActionType: ListItemAction,
        val quickStartEvent: QuickStartEvent
    )
}
