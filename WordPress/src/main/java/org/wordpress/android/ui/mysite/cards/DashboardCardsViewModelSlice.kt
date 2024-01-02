package org.wordpress.android.ui.mysite.cards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardVewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.BuildConfigWrapper
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
    private val quickStartCardVewModelSlice: QuickStartCardVewModelSlice,
    private val noCardsMessageViewModelSlice: NoCardsMessageViewModelSlice,
    private val quickLinksItemViewModelSlice: QuickLinksItemViewModelSlice,
    private val bloganuaryNudgeCardViewModelSlice: BloganuaryNudgeCardViewModelSlice,
    private val plansCardViewModelSlice: PlansCardViewModelSlice,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    private lateinit var scope: CoroutineScope

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
        jpMigrationSuccessCardViewModelSlice.onNavigation,
        quickStartCardVewModelSlice.onNavigation
    )

    val refresh = merge(
        cardViewModelSlice.refresh,
        bloggingPromptCardViewModelSlice.refresh
    )

    val isRefreshing = merge(
        blazeCardViewModelSlice.isRefreshing,
        cardViewModelSlice.isRefreshing,
        bloggingPromptCardViewModelSlice.isRefreshing,
        quickStartCardVewModelSlice.isRefreshing
    )

    val _uiModel = MutableLiveData<List<MySiteCardAndItem>>()
    val uiModel: LiveData<List<MySiteCardAndItem>> = merge(
        quickLinksItemViewModelSlice.uiState,
        quickStartCardVewModelSlice.uiModel,
        blazeCardViewModelSlice.uiModel,
        cardViewModelSlice.uiModel,
        bloggingPromptCardViewModelSlice.uiModel,
        bloganuaryNudgeCardViewModelSlice.uiModel,
        jpMigrationSuccessCardViewModelSlice.uiModel,
        plansCardViewModelSlice.uiModel,
        personalizeCardViewModelSlice.uiModel,
        jetpackInstallFullPluginCardViewModelSlice.uiModel
    ) { quicklinks,
        quickStart,
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
            quickStart,
            blazeCard,
            cardsState,
            bloggingPromptCard,
            bloganuaryNudgeCard,
            migrationSuccessCard,
            plansCard,
            personalizeCard,
            jpFullInstallFullPlugin
        )
    }.distinctUntilChanged()

    @SuppressWarnings("LongParameterList")
    private fun mergeUiModels(
        quicklinks: MySiteCardAndItem.Card.QuickLinksItem?,
        quickStart: MySiteCardAndItem.Card.QuickStartCard?,
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
        quickStart?.let { cards.add(it) }
        bloganuaryNudgeCard?.let { cards.add(it) }
        bloggingPromptCard?.let { cards.add(it) }
        blazeCard?.let { cards.add(it) }
        cardsState?.let {
            when (cardsState) {
                is CardsState.Success -> cards.addAll(cardsState.cards)
                is CardsState.ErrorState -> cards.add(cardsState.error)
            }
        }
        migrationSuccessCard?.let { cards.add(it) }
        plansCard?.let { cards.add(it) }
        jpFullInstallFullPlugin?.let { cards.add(it) }
        personalizeCard?.let { cards.add(it) }
        return cards.toList()
    }

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
        blazeCardViewModelSlice.initialize(scope)
        bloggingPromptCardViewModelSlice.initialize(scope)
        bloganuaryNudgeCardViewModelSlice.initialize(scope)
        personalizeCardViewModelSlice.initialize(scope)
        quickLinksItemViewModelSlice.initialization(scope)
        cardViewModelSlice.initialize(scope)
        quickStartCardVewModelSlice.initialize(scope)
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
        quickStartCardVewModelSlice.build(site)
    }

    fun onResume() {
        selectedSiteRepository.getSelectedSite()?.let {
            if(showDashboardCards(it))buildCards(it)
        }
    }

    fun onSiteChanged() {
        selectedSiteRepository.getSelectedSite()?.let {
            if(showDashboardCards(it))buildCards(it)
        }
    }

    fun onRefresh() {
        selectedSiteRepository.getSelectedSite()?.let {
            if(showDashboardCards(it))buildCards(it)
        }
    }

    fun onCleared() {
        quickLinksItemViewModelSlice.onCleared()
        scope.cancel()
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

    private fun showDashboardCards(site: SiteModel) =
        site.isUsingWpComRestApi && buildConfigWrapper.isJetpackApp
}
