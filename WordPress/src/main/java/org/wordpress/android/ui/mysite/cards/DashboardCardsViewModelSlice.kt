package org.wordpress.android.ui.mysite.cards

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.BlazeCardViewModelSlice
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.CardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.CardsState
import org.wordpress.android.ui.mysite.cards.dashboard.bloganuary.BloganuaryNudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.domainregistration.DomainRegistrationCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.migration.JpMigrationSuccessCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.nocards.NoCardsMessageViewModelSlice
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.plans.PlansCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.quicklinksitem.QuickLinksItemViewModelSlice
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class DashboardCardsViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val jpMigrationSuccessCardViewModelSlice: JpMigrationSuccessCardViewModelSlice,
    private val jetpackInstallFullPluginCardViewModelSlice: JetpackInstallFullPluginCardViewModelSlice,
    private val domainRegistrationCardViewModelSlice: DomainRegistrationCardViewModelSlice,
    private val blazeCardViewModelSlice: BlazeCardViewModelSlice,
    private val cardViewModelSlice: CardViewModelSlice,
    private val personalizeCardViewModelSlice: PersonalizeCardViewModelSlice,
    private val bloggingPromptCardViewModelSlice: BloggingPromptCardViewModelSlice,
    private val quickStartCardViewModelSlice: QuickStartCardViewModelSlice,
    private val noCardsMessageViewModelSlice: NoCardsMessageViewModelSlice,
    private val quickLinksItemViewModelSlice: QuickLinksItemViewModelSlice,
    private val bloganuaryNudgeCardViewModelSlice: BloganuaryNudgeCardViewModelSlice,
    private val plansCardViewModelSlice: PlansCardViewModelSlice,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    private lateinit var scope: CoroutineScope

    private var job: Job? = null

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
        quickStartCardViewModelSlice.onNavigation,
        domainRegistrationCardViewModelSlice.onNavigation,
    )

    val refresh = merge(
        cardViewModelSlice.refresh,
        bloggingPromptCardViewModelSlice.refresh
    )

    val isRefreshing = merge(
        blazeCardViewModelSlice.isRefreshing,
        cardViewModelSlice.isRefreshing,
        bloggingPromptCardViewModelSlice.isRefreshing,
        quickStartCardViewModelSlice.isRefreshing,
        domainRegistrationCardViewModelSlice.isRefreshing
    )

    val uiModel: MutableLiveData<List<MySiteCardAndItem>> = merge(
        quickLinksItemViewModelSlice.uiState,
        quickStartCardViewModelSlice.uiModel,
        blazeCardViewModelSlice.uiModel,
        cardViewModelSlice.uiModel,
        bloggingPromptCardViewModelSlice.uiModel,
        bloganuaryNudgeCardViewModelSlice.uiModel,
        jpMigrationSuccessCardViewModelSlice.uiModel,
        plansCardViewModelSlice.uiModel,
        personalizeCardViewModelSlice.uiModel,
        jetpackInstallFullPluginCardViewModelSlice.uiModel,
        domainRegistrationCardViewModelSlice.uiModel
    ) { quicklinks,
        quickStart,
        blazeCard,
        cardsState,
        bloggingPromptCard,
        bloganuaryNudgeCard,
        migrationSuccessCard,
        plansCard,
        personalizeCard,
        jpFullInstallFullPlugin,
        domainRegistrationCard ->
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
            jpFullInstallFullPlugin,
            domainRegistrationCard
        )
    }.distinctUntilChanged() as MutableLiveData<List<MySiteCardAndItem>>

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
        domainRegistrationCard: MySiteCardAndItem.Card.DomainRegistrationCard?,
    ): List<MySiteCardAndItem> {
        val cards = mutableListOf<MySiteCardAndItem>()
        quicklinks?.let { cards.add(it) }
        quickStart?.let { cards.add(it) }
        domainRegistrationCard?.let { cards.add(it) }
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
        // when clearing the values of all child VM Slices,
        // the no cards message will still be shown and hence we need to check if the personalize card
        // is shown or not, if the personalize card is not shown, then it means that
        // we are not showing dashboard at all
        personalizeCard?.let {
            noCardsMessageViewModelSlice.buildNoCardsMessage(cards)?.let { cards.add(it) }
            cards.add(it)
        }
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
        quickStartCardViewModelSlice.initialize(scope)
    }

    fun buildCards(site: SiteModel) {
        job?.cancel()
        job = scope.launch(bgDispatcher) {
            jpMigrationSuccessCardViewModelSlice.buildCard()
            jetpackInstallFullPluginCardViewModelSlice.buildCard(site)
            blazeCardViewModelSlice.buildCard(site)
            bloggingPromptCardViewModelSlice.buildCard(site)
            bloganuaryNudgeCardViewModelSlice.buildCard()
            personalizeCardViewModelSlice.buildCard()
            quickLinksItemViewModelSlice.buildCard(site)
            plansCardViewModelSlice.buildCard(site)
            cardViewModelSlice.buildCard(site)
            quickStartCardViewModelSlice.build(site)
        }
    }


    fun clearValue() {
        jpMigrationSuccessCardViewModelSlice.clearValue()
        jetpackInstallFullPluginCardViewModelSlice.clearValue()
        blazeCardViewModelSlice.clearValue()
        bloggingPromptCardViewModelSlice.clearValue()
        bloganuaryNudgeCardViewModelSlice.clearValue()
        personalizeCardViewModelSlice.clearValue()
        quickLinksItemViewModelSlice.clearValue()
        plansCardViewModelSlice.clearValue()
        cardViewModelSlice.clearValue()
        quickStartCardViewModelSlice.clearValue()
    }

    fun onCleared() {
        quickLinksItemViewModelSlice.onCleared()
        job?.cancel()
        scope.cancel()
    }

    fun refreshBloggingPrompt() {
        selectedSiteRepository.getSelectedSite()?.let {
            bloggingPromptCardViewModelSlice.buildCard(it)
        }
    }

    fun trackCardShown(dashboardData: List<MySiteCardAndItem>){
        dashboardData.filterIsInstance<MySiteCardAndItem.Card>().let {
            cardViewModelSlice.trackCardShown(it)
        }
        dashboardData.filterIsInstance<MySiteCardAndItem.Card.JetpackInstallFullPluginCard>()
            .forEach { jetpackInstallFullPluginCardViewModelSlice.trackShown(it) }

        dashboardData.filterIsInstance<MySiteCardAndItem.Card.DomainRegistrationCard>()
            .forEach { domainRegistrationCardViewModelSlice.trackShown(it) }

        dashboardData.filterIsInstance<MySiteCardAndItem.Card.PersonalizeCardModel>().forEach {
            personalizeCardViewModelSlice.trackShown(it)
        }
    }

    fun resetShownTracker() {
        cardViewModelSlice.resetShownTracker()
        jetpackInstallFullPluginCardViewModelSlice.resetShownTracker()
        domainRegistrationCardViewModelSlice.resetCardShown()
        personalizeCardViewModelSlice.resetShown()
    }

    fun startQuickStart(siteModel: SiteModel) {
        quickStartCardViewModelSlice.build(siteModel)
    }
}
