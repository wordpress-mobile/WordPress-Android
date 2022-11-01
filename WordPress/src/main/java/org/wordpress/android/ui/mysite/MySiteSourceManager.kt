package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.MySiteSource.MySiteRefreshSource
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.cards.dashboard.CardsSource
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardSource
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationSource
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardSource
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Hide
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Pin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Unpin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsSource
import org.wordpress.android.ui.quickstart.QuickStartTracker
import javax.inject.Inject

class MySiteSourceManager @Inject constructor(
    private val quickStartTracker: QuickStartTracker,
    private val currentAvatarSource: CurrentAvatarSource,
    private val domainRegistrationSource: DomainRegistrationSource,
    private val dynamicCardsSource: DynamicCardsSource,
    private val quickStartCardSource: QuickStartCardSource,
    private val scanAndBackupSource: ScanAndBackupSource,
    private val selectedSiteSource: SelectedSiteSource,
    cardsSource: CardsSource,
    siteIconProgressSource: SiteIconProgressSource,
    private val bloggingPromptCardSource: BloggingPromptCardSource,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private val mySiteSources: List<MySiteSource<*>> = listOf(
            selectedSiteSource,
            siteIconProgressSource,
            quickStartCardSource,
            currentAvatarSource,
            domainRegistrationSource,
            scanAndBackupSource,
            dynamicCardsSource,
            cardsSource,
            bloggingPromptCardSource
    )

    private val showDashboardCards: Boolean
        get() = selectedSiteRepository.getSelectedSite()?.isUsingWpComRestApi == true

    private val allSupportedMySiteSources: List<MySiteSource<*>>
        get() = if (showDashboardCards) {
            mySiteSources
        } else {
            mySiteSources.filterNot(CardsSource::class.java::isInstance)
        }

    private val siteIndependentSources: List<SiteIndependentSource<*>>
        get() = mySiteSources.filterIsInstance(SiteIndependentSource::class.java)

    fun build(coroutineScope: CoroutineScope, siteLocalId: Int?): List<LiveData<out PartialState>> {
        return if (siteLocalId != null) {
            allSupportedMySiteSources.map { source -> source.build(coroutineScope, siteLocalId) }
        } else {
            siteIndependentSources.map { source -> source.build(coroutineScope) }
        }
    }

    fun isRefreshing(): Boolean {
        val source = if (selectedSiteRepository.hasSelectedSite()) {
            allSupportedMySiteSources
        } else {
            siteIndependentSources
        }
        source.filterIsInstance(MySiteRefreshSource::class.java).forEach {
            if (it.isRefreshing() == true) {
                return true
            }
        }
        return false
    }

    fun refresh() {
        allSupportedMySiteSources.filterIsInstance(MySiteRefreshSource::class.java).forEach {
            if (it is SiteIndependentSource || selectedSiteRepository.hasSelectedSite()) it.refresh()
        }
    }

    fun onResume(isSiteSelected: Boolean) {
        when (isSiteSelected) {
            true -> refreshSubsetOfAllSources()
            false -> refresh()
        }
    }

    fun clear() {
        domainRegistrationSource.clear()
        scanAndBackupSource.clear()
        selectedSiteSource.clear()
    }

    private fun refreshSubsetOfAllSources() {
        selectedSiteSource.updateSiteSettingsIfNecessary()
        currentAvatarSource.refresh()
        if (selectedSiteRepository.hasSelectedSite()) quickStartCardSource.refresh()
    }

    fun refreshBloggingPrompts(onlyCurrentPrompt: Boolean) {
        if (onlyCurrentPrompt) {
            bloggingPromptCardSource.refreshTodayPrompt()
        } else {
            bloggingPromptCardSource.refresh()
        }
    }

    /* QUICK START */

    fun refreshQuickStart() {
        quickStartCardSource.refresh()
    }

    suspend fun onQuickStartMenuInteraction(interaction: DynamicCardMenuInteraction) {
        when (interaction) {
            is DynamicCardMenuInteraction.Remove -> {
                quickStartTracker.track(Stat.QUICK_START_REMOVE_CARD_TAPPED)
                dynamicCardsSource.removeItem(interaction.cardType)
                quickStartCardSource.refresh()
            }
            is Pin -> dynamicCardsSource.pinItem(interaction.cardType)
            is Unpin -> dynamicCardsSource.unpinItem()
            is Hide -> {
                quickStartTracker.track(Stat.QUICK_START_HIDE_CARD_TAPPED)
                dynamicCardsSource.hideItem(interaction.cardType)
                quickStartCardSource.refresh()
            }
        }
    }
}
