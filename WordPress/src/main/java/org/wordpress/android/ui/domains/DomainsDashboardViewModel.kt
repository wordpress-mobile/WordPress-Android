package org.wordpress.android.ui.domains

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DomainsDashboardViewModel @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) : ViewModel() {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<List<MySiteCardAndItem>>()
    val uiModel = _uiModel

    val siteUrl: String = SiteUtils.getHomeURLOrHostName(selectedSiteRepository.selectedSiteChange.value)

    // TODO: UI and logic is work in progress.  Will be revamped once design is ready
    private fun buildPrimarySiteAddressUiItems(onClick: (ListItemAction) -> Unit): List<MySiteCardAndItem> {
        val listItems = mutableListOf<MySiteCardAndItem>()
        listItems += CategoryHeaderItem(UiStringRes(string.domains_primary_domain))
        listItems += ListItem(
                R.drawable.ic_domains_white_24dp,
                primaryText = UiStringResWithParams(
                        string.domains_primary_domain_address,
                        listOf(UiStringText(siteUrl))
                ),
                onClick = ListItemInteraction.create(ListItemAction.POSTS, onClick)
        )

        if (selectedSiteRepository.getSelectedSite()?.hasFreePlan == true) {
            listItems += ListItem(
                    R.drawable.ic_domains_white_24dp,
                    primaryText = UiStringRes(string.domains_free_plan_get_your_domain_title),
                    onClick = ListItemInteraction.create(this::domainRegistrationClick)
            )
        } else {
            DomainRegistrationCard(ListItemInteraction.create(this::domainRegistrationClick))
        }
        return listItems
    }

    private var isStarted: Boolean = false
    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
        _uiModel.value = buildPrimarySiteAddressUiItems { onItemClick() }
    }

    private fun onItemClick() {
        // TODO
    }

    private fun domainRegistrationClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(OpenDomainRegistration(selectedSite))
    }
}
