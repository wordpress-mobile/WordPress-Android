package org.wordpress.android.ui.mysite

import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory

data class MySiteUiState(
    val currentAvatarUrl: String? = null,
    val site: SiteModel? = null,
    val showSiteIconProgressBar: Boolean = false,
    val isDomainCreditAvailable: Boolean = false,
    val scanAvailable: Boolean = false,
    val backupAvailable: Boolean = false,
    val activeTask: QuickStartTask? = null,
    val quickStartCategories: List<QuickStartCategory> = listOf(),
    val pinnedDynamicCard: DynamicCardType? = null,
    val visibleDynamicCards: List<DynamicCardType> = listOf()
) {
    sealed class PartialState {
        data class CurrentAvatarUrl(val url: String) : PartialState()
        data class SelectedSite(val site: SiteModel?) : PartialState()
        data class ShowSiteIconProgressBar(val showSiteIconProgressBar: Boolean) : PartialState()
        data class DomainCreditAvailable(val isDomainCreditAvailable: Boolean) : PartialState()
        data class JetpackCapabilities(val scanAvailable: Boolean, val backupAvailable: Boolean) : PartialState()
        data class QuickStartUpdate(
            val activeTask: QuickStartTask? = null,
            val categories: List<QuickStartCategory> = listOf()
        ) : PartialState()

        data class DynamicCardsUpdate(
            val pinnedDynamicCard: DynamicCardType? = null,
            val cards: List<DynamicCardType>
        ) : PartialState()
    }

    fun update(partialState: PartialState): MySiteUiState {
        return when (partialState) {
            is CurrentAvatarUrl -> this.copy(currentAvatarUrl = partialState.url)
            is SelectedSite -> this.copy(site = partialState.site)
            is ShowSiteIconProgressBar -> this.copy(showSiteIconProgressBar = partialState.showSiteIconProgressBar)
            is DomainCreditAvailable -> this.copy(isDomainCreditAvailable = partialState.isDomainCreditAvailable)
            is JetpackCapabilities -> this.copy(
                    scanAvailable = partialState.scanAvailable,
                    backupAvailable = partialState.backupAvailable
            )
            is QuickStartUpdate -> this.copy(
                    activeTask = partialState.activeTask,
                    quickStartCategories = partialState.categories
            )
            is DynamicCardsUpdate -> this.copy(
                    pinnedDynamicCard = partialState.pinnedDynamicCard,
                    visibleDynamicCards = partialState.cards
            )
        }
    }
}
