package org.wordpress.android.ui.mysite

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.BloggingPromptUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar

data class MySiteUiState(
    val currentAvatarUrl: String? = null,
    val avatarName: String? = null,
    val site: SiteModel? = null,
    val showSiteIconProgressBar: Boolean = false,
    val isDomainCreditAvailable: Boolean = false,
    val scanAvailable: Boolean = false,
    val backupAvailable: Boolean = false,
    val cardsUpdate: CardsUpdate? = null,
    val bloggingPromptsUpdate: BloggingPromptUpdate? = null,
    val blazeCardUpdate: PartialState.BlazeCardUpdate? = null,
) {
    sealed class PartialState {
        data class AccountData(val url: String, val name: String) : PartialState()
        data class SelectedSite(val site: SiteModel?) : PartialState()
        data class ShowSiteIconProgressBar(val showSiteIconProgressBar: Boolean) : PartialState()
        data class DomainCreditAvailable(val isDomainCreditAvailable: Boolean) : PartialState()
        data class JetpackCapabilities(val scanAvailable: Boolean, val backupAvailable: Boolean) : PartialState()

        data class CardsUpdate(
            val cards: List<CardModel>? = null,
            val showErrorCard: Boolean = false,
            val showSnackbarError: Boolean = false,
            val showStaleMessage: Boolean = false
        ) : PartialState()

        data class BloggingPromptUpdate(
            val promptModel: BloggingPromptModel?
        ) : PartialState()

        data class BlazeCardUpdate(
            val blazeEligible: Boolean = false,
            val campaign: BlazeCampaignModel? = null
        ) : PartialState()
    }

    fun update(partialState: PartialState): MySiteUiState {
        val uiState = updateSnackbarStatusToShowOnlyOnce(partialState)

        return when (partialState) {
            is AccountData -> uiState.copy(currentAvatarUrl = partialState.url, avatarName = partialState.name)
            is SelectedSite -> uiState.copy(site = partialState.site)
            is ShowSiteIconProgressBar -> uiState.copy(showSiteIconProgressBar = partialState.showSiteIconProgressBar)
            is DomainCreditAvailable -> uiState.copy(isDomainCreditAvailable = partialState.isDomainCreditAvailable)
            is JetpackCapabilities -> uiState.copy(
                scanAvailable = partialState.scanAvailable,
                backupAvailable = partialState.backupAvailable
            )
            is CardsUpdate -> uiState.copy(cardsUpdate = partialState)
            is BloggingPromptUpdate -> uiState.copy(bloggingPromptsUpdate = partialState)
            is PartialState.BlazeCardUpdate -> uiState.copy(blazeCardUpdate = partialState)
        }
    }

    private fun updateSnackbarStatusToShowOnlyOnce(partialState: PartialState) =
        if (partialState !is CardsUpdate) {
            this.copy(cardsUpdate = this.cardsUpdate?.copy(showSnackbarError = false))
        } else this
}
