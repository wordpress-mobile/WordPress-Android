package org.wordpress.android.ui.mysite.cards

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

@SuppressWarnings("LongParameterList")
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

    private var trackingJob: Job? = null

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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
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
        migrationSuccessCard?.let { cards.add(it) }
        jpFullInstallFullPlugin?.let { cards.add(it) }
        domainRegistrationCard?.let { cards.add(it) }
        quicklinks?.let { cards.add(it) }
        quickStart?.let { cards.add(it) }
        cardsState?.let {
            if (cardsState is CardsState.Success) {
                cards.addAll(cardsState.topCards)
            }
        }
        bloganuaryNudgeCard?.let { cards.add(it) }
        bloggingPromptCard?.let { cards.add(it) }
        blazeCard?.let { cards.add(it) }
        plansCard?.let { cards.add(it) }
        cardsState?.let {
            when (cardsState) {
                is CardsState.Success -> {
                    cards.addAll(cardsState.cards)
                    cards.addAll(cardsState.bottomCards)
                }
                is CardsState.ErrorState -> cards.add(cardsState.error)
            }
        }
        // when clearing the values of all child VM Slices,
        // the no cards message will still be shown and hence we need to check if the personalize card
        // is shown or not, if the personalize card is not shown, then it means that
        // we are not showing dashboard at all
        personalizeCard?.let { personalize ->
            noCardsMessageViewModelSlice.buildNoCardsMessage(cards)?.let { noCardsMessage ->
                cards.add(noCardsMessage)
            }
            cards.add(personalize)
        }
        if(cards.isNotEmpty()) trackCardShown(cards)
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
        domainRegistrationCardViewModelSlice.initialize(scope)
    }

    fun buildCards(site: SiteModel) {
        job?.cancel()
        job = scope.launch(bgDispatcher) {
            jpMigrationSuccessCardViewModelSlice.buildCard()
            jetpackInstallFullPluginCardViewModelSlice.buildCard(site)
            blazeCardViewModelSlice.buildCard(site)
            bloggingPromptCardViewModelSlice.fetchBloggingPrompt(site)
            bloganuaryNudgeCardViewModelSlice.buildCard()
            personalizeCardViewModelSlice.buildCard()
            quickLinksItemViewModelSlice.buildCard(site)
            plansCardViewModelSlice.buildCard(site)
            cardViewModelSlice.buildCard(site)
            quickStartCardViewModelSlice.build(site)
            domainRegistrationCardViewModelSlice.buildCard(site)
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
        domainRegistrationCardViewModelSlice.clearValue()
    }

    fun onCleared() {
        quickLinksItemViewModelSlice.onCleared()
        bloggingPromptCardViewModelSlice.onCleared()
        job?.cancel()
        trackingJob?.cancel()
        scope.cancel()
    }

    fun refreshBloggingPrompt() {
        selectedSiteRepository.getSelectedSite()?.let {
            bloggingPromptCardViewModelSlice.fetchBloggingPrompt(it)
        }
    }

    private fun trackCardShown(dashboardData: List<MySiteCardAndItem>) = with(dashboardData) {
        trackingJob?.cancel()
        trackingJob = scope.launch(bgDispatcher) {
            delay(TRACKING_JOB_DEBOUNCE_DELAY)

            filterIsInstance<MySiteCardAndItem.Card>().let {
                cardViewModelSlice.trackCardShown(it)
            }
            filterIsInstance<MySiteCardAndItem.Card.JetpackInstallFullPluginCard>()
                .forEach { jetpackInstallFullPluginCardViewModelSlice.trackShown(it) }

            filterIsInstance<MySiteCardAndItem.Card.DomainRegistrationCard>()
                .forEach { domainRegistrationCardViewModelSlice.trackShown(it) }

            filterIsInstance<MySiteCardAndItem.Card.PersonalizeCardModel>().forEach {
                personalizeCardViewModelSlice.trackShown()
            }

            filterIsInstance<MySiteCardAndItem.Card.QuickStartCard>().forEach {
                quickStartCardViewModelSlice.trackShown(it)
            }

            filterIsInstance<MySiteCardAndItem.Card.NoCardsMessage>().forEach {
                noCardsMessageViewModelSlice.trackShown(it.type)
            }

            filterIsInstance<MySiteCardAndItem.Card.DashboardPlansCard>().forEachIndexed { index, _ ->
                plansCardViewModelSlice.trackShown(index)
            }

            filterIsInstance<MySiteCardAndItem.Card.BloggingPromptCard>().let {
                bloggingPromptCardViewModelSlice.onDashboardCardsUpdated(scope, it)
            }
        }
    }

    fun resetShownTracker() {
        cardViewModelSlice.resetShownTracker()
        jetpackInstallFullPluginCardViewModelSlice.resetShownTracker()
        domainRegistrationCardViewModelSlice.resetCardShown()
        personalizeCardViewModelSlice.resetShown()
        quickStartCardViewModelSlice.resetShown()
        noCardsMessageViewModelSlice.resetShown()
        plansCardViewModelSlice.resetShown()
    }

    fun startQuickStart(siteModel: SiteModel) {
        quickStartCardViewModelSlice.build(siteModel)
    }
}

const val TRACKING_JOB_DEBOUNCE_DELAY = 600L
