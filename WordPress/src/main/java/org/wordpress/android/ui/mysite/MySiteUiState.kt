package org.wordpress.android.ui.mysite

import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BloggingPromptUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
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
    val visibleDynamicCards: List<DynamicCardType> = listOf(),
    val cardsUpdate: CardsUpdate? = null,
    val bloggingPromptsUpdate: BloggingPromptUpdate? = null
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

        data class CardsUpdate(
            val cards: List<CardModel>? = null,
            val showErrorCard: Boolean = false,
            val showSnackbarError: Boolean = false,
            val showStaleMessage: Boolean = false
        ) : PartialState()

        data class BloggingPromptUpdate(
            val promptModel: BloggingPromptModel?
        ) : PartialState()
    }

    fun update(partialState: PartialState): MySiteUiState {
        val uiState = updateSnackbarStatusToShowOnlyOnce(partialState)

        return when (partialState) {
            is CurrentAvatarUrl -> uiState.copy(currentAvatarUrl = partialState.url)
            is SelectedSite -> uiState.copy(site = partialState.site)
            is ShowSiteIconProgressBar -> uiState.copy(showSiteIconProgressBar = partialState.showSiteIconProgressBar)
            is DomainCreditAvailable -> uiState.copy(isDomainCreditAvailable = partialState.isDomainCreditAvailable)
            is JetpackCapabilities -> uiState.copy(
                scanAvailable = partialState.scanAvailable,
                backupAvailable = partialState.backupAvailable
            )
            is QuickStartUpdate -> uiState.copy(
                activeTask = partialState.activeTask,
                quickStartCategories = partialState.categories
            )
            is DynamicCardsUpdate -> uiState.copy(
                pinnedDynamicCard = partialState.pinnedDynamicCard,
                visibleDynamicCards = partialState.cards
            )
            is CardsUpdate -> uiState.copy(cardsUpdate = partialState)
            is BloggingPromptUpdate -> uiState.copy(bloggingPromptsUpdate = partialState)
        }
    }

    private fun updateSnackbarStatusToShowOnlyOnce(partialState: PartialState) =
        if (partialState !is CardsUpdate) {
            this.copy(cardsUpdate = this.cardsUpdate?.copy(showSnackbarError = false))
        } else this
}
