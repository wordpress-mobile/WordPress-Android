package org.wordpress.android.ui.mysite.cards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.BlazeCardViewModelSlice
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.CardsState
import org.wordpress.android.ui.mysite.cards.dashboard.bloganuary.BloganuaryNudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.migration.JpMigrationSuccessCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.nocards.NoCardsMessageViewModelSlice
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.plans.PlansCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.quicklinksitem.QuickLinksItemViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DashboardCardsViewModelSlice @Inject constructor(
    private val jpMigrationSuccessCardViewModelSlice: JpMigrationSuccessCardViewModelSlice,
    private val jetpackInstallFullPluginCardViewModelSlice: JetpackInstallFullPluginCardViewModelSlice,
    private val blazeCardViewModelSlice: BlazeCardViewModelSlice,
    private val cardViewModelSlice: CardViewModelSlice,
    private val personalizeCardViewModelSlice: PersonalizeCardViewModelSlice,
    private val bloggingPromptCardViewModelSlice: BloggingPromptCardViewModelSlice,
    private val noCardsMessageViewModelSlice: NoCardsMessageViewModelSlice,
    private val quickLinksItemViewModelSlice: QuickLinksItemViewModelSlice,
    private val bloganuaryNudgeCardViewModelSlice: BloganuaryNudgeCardViewModelSlice,
    private val plansCardViewModelSlice: PlansCardViewModelSlice,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private val _onSnackbar = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = merge(
        _onSnackbar,
        bloggingPromptCardViewModelSlice.onSnackbarMessage,
        quickLinksItemViewModelSlice.onSnackbarMessage,
    )

    val onOpenJetpackInstallFullPluginOnboarding =
        jetpackInstallFullPluginCardViewModelSlice.onOpenJetpackInstallFullPluginOnboarding

    val onNavigation = merge(
        blazeCardViewModelSlice.onNavigation,
        cardViewModelSlice.onNavigation,
        bloggingPromptCardViewModelSlice.onNavigation,
        bloganuaryNudgeCardViewModelSlice.onNavigation,
        personalizeCardViewModelSlice.onNavigation,
        quickLinksItemViewModelSlice.navigation,
        plansCardViewModelSlice.onNavigation,
        jpMigrationSuccessCardViewModelSlice.onNavigation
    )

    val refresh = merge(
        blazeCardViewModelSlice.refresh,
        bloganuaryNudgeCardViewModelSlice.refresh,
        plansCardViewModelSlice.refresh,
        cardViewModelSlice.refresh,
        jetpackInstallFullPluginCardViewModelSlice.refresh
    )

    val isRefreshing = merge(
        blazeCardViewModelSlice.isRefreshing,
        cardViewModelSlice.isRefreshing,
        bloggingPromptCardViewModelSlice.isRefreshing
    )

    val uiModel: LiveData<List<MySiteCardAndItem>> = merge(
        quickLinksItemViewModelSlice.uiState,
        blazeCardViewModelSlice.uiModel,
        cardViewModelSlice.uiModel,
        bloggingPromptCardViewModelSlice.uiModel,
        bloganuaryNudgeCardViewModelSlice.uiModel,
        jpMigrationSuccessCardViewModelSlice.uiModel,
        plansCardViewModelSlice.uiModel,
        personalizeCardViewModelSlice.uiModel,
        jetpackInstallFullPluginCardViewModelSlice.uiModel
    ) { quicklinks,
        blazeCard,
        cardsState,
        bloggingPromptCard,
        bloganuaryNudgeCard,
        migrationSuccessCard,
        plansCard,
        personalizeCard,
        jpFullInstallFullPlugin ->
        return@merge mergeUiModels(
            quicklinks,
            blazeCard,
            cardsState,
            bloggingPromptCard,
            bloganuaryNudgeCard,
            migrationSuccessCard,
            plansCard,
            personalizeCard,
            jpFullInstallFullPlugin
        )
    }

    @SuppressWarnings("LongParameterList")
    fun mergeUiModels(
        quicklinks: MySiteCardAndItem.Card.QuickLinksItem?,
        blazeCard: MySiteCardAndItem.Card.BlazeCard?,
        cardsState: CardsState?,
        bloggingPromptCard: MySiteCardAndItem.Card.BloggingPromptCard.BloggingPromptCardWithData?,
        bloganuaryNudgeCard: MySiteCardAndItem.Card.BloganuaryNudgeCardModel?,
        migrationSuccessCard: MySiteCardAndItem.Item.SingleActionCard?,
        plansCard: MySiteCardAndItem.Card.DashboardPlansCard?,
        personalizeCard: MySiteCardAndItem.Card.PersonalizeCardModel?,
        jpFullInstallFullPlugin: MySiteCardAndItem.Card.JetpackInstallFullPluginCard?,
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        quicklinks?.let { cards.add(it) }
        bloganuaryNudgeCard?.let { cards.add(it) }
        bloggingPromptCard?.let { cards.add(it) }
        blazeCard?.let { cards.add(it) }
        cardsState?.let {
            when (cardsState) {
                is CardsState.Success -> cards.addAll(cardsState.cards)
                is CardsState.ErrorState -> cards.add(cardsState.error)
            }
        }
        personalizeCard?.let { cards.add(it) }
        migrationSuccessCard?.let { cards.add(it) }
        plansCard?.let { cards.add(it) }
        jpFullInstallFullPlugin?.let { cards.add(it) }
        return cards.toList()
    }

    fun initialize(scope: CoroutineScope) {
        blazeCardViewModelSlice.initialize(scope)
        bloggingPromptCardViewModelSlice.initialize(scope)
        bloganuaryNudgeCardViewModelSlice.initialize(scope)
        personalizeCardViewModelSlice.initialize(scope)
        quickLinksItemViewModelSlice.initialization(scope)
        cardViewModelSlice.initialize(scope)
    }

    private fun buildCards(site: SiteModel) {
        jpMigrationSuccessCardViewModelSlice.buildCard()
        jetpackInstallFullPluginCardViewModelSlice.buildCard(site)
        blazeCardViewModelSlice.buildCard(site)
        bloggingPromptCardViewModelSlice.buildCard(site)
        bloganuaryNudgeCardViewModelSlice.buildCard()
        personalizeCardViewModelSlice.buildCard()
        quickLinksItemViewModelSlice.buildCard(site)
        plansCardViewModelSlice.buildCard(site)
        cardViewModelSlice.buildCard(site)
    }

    fun onResume() {
        buildCards(selectedSiteRepository.getSelectedSite() ?: return)
    }

    fun onSiteChanged() {
        selectedSiteRepository.getSelectedSite()?.let {
            buildCards(it)
        }
    }

    fun onRefresh() {
        selectedSiteRepository.getSelectedSite()?.let {
            buildCards(it)
        }
    }

    fun onCleared() {
        quickLinksItemViewModelSlice.onCleared()
    }

    fun refreshBloggingPrompt() {
        selectedSiteRepository.getSelectedSite()?.let {
            bloggingPromptCardViewModelSlice.buildCard(it)
        }
    }

    fun resetShownTracker() {
        personalizeCardViewModelSlice.resetShown()
//        dynamicCardsViewModelSlice.resetShown()
//        domainRegistrationCardShownTracker.resetShown()
//        cardsTracker.resetShown()
//        quickStartTracker.resetShown()
    }
}
