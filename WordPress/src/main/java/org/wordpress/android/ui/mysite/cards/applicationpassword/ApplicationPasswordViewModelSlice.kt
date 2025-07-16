package org.wordpress.android.ui.mysite.cards.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ApplicationPasswordViewModelSlice @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val siteStore: SiteStore,
    private val experimentalFeatures: ExperimentalFeatures,
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
                postAuthenticationUrl(authorizationUrlComplete)
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
