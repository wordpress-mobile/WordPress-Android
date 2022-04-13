package org.wordpress.android.ui.mysite

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.test
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.BloggingPromptCard.BloggingPromptCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ErrorCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.FooterLink
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PostCard.PostCardWithPostItems.PostItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.TodaysStatsCard.TodaysStatsCardWithData
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.DynamicCard.QuickStartDynamicCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard.IconState
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CurrentAvatarUrl
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.ShowSiteIconProgressBar
import org.wordpress.android.ui.mysite.MySiteViewModel.MySiteTrackWithTabSource
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.MySiteViewModel.TabNavigation
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.MySiteViewModel.UiModel
import org.wordpress.android.ui.mysite.SiteDialogModel.AddSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ChangeSiteIconDialogModel
import org.wordpress.android.ui.mysite.SiteDialogModel.ShowRemoveNextStepsDialog
import org.wordpress.android.ui.mysite.cards.CardsBuilder
import org.wordpress.android.ui.mysite.cards.DomainRegistrationCardShownTracker
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostCardType
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsCardBuilder.Companion.URL_GET_MORE_VIEWS_AND_TRAFFIC
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartSiteMenuStep
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardBuilder
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment.DynamicCardMenuModel
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardsBuilder
import org.wordpress.android.ui.mysite.items.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.SiteItemsTracker
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import org.wordpress.android.ui.mysite.tabs.MySiteTabType.DASHBOARD
import org.wordpress.android.ui.mysite.tabs.MySiteTabType.SITE_MENU
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.util.config.LandOnTheEditorFeatureConfig
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.viewmodel.ContextProvider
import java.util.Date

private const val DYNAMIC_CARDS_BUILDER_MORE_CLICK_PARAM_POSITION = 3

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
@RunWith(MockitoJUnitRunner::class)
class MySiteViewModelTest : BaseUnitTest() {
    @Mock lateinit var siteItemsBuilder: SiteItemsBuilder
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var wpMediaUtilsWrapper: WPMediaUtilsWrapper
    @Mock lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    @Mock lateinit var fluxCUtilsWrapper: FluxCUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var siteIconUploadHandler: SiteIconUploadHandler
    @Mock lateinit var siteStoriesHandler: SiteStoriesHandler
    @Mock lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    @Mock lateinit var quickStartRepository: QuickStartRepository
    @Mock lateinit var quickStartCardBuilder: QuickStartCardBuilder
    @Mock lateinit var siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder
    @Mock lateinit var homePageDataLoader: HomePageDataLoader
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper
    @Mock lateinit var snackbarSequencer: SnackbarSequencer
    @Mock lateinit var cardsBuilder: CardsBuilder
    @Mock lateinit var dynamicCardsBuilder: DynamicCardsBuilder
    @Mock lateinit var mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig
    @Mock lateinit var landOnTheEditorFeatureConfig: LandOnTheEditorFeatureConfig
    @Mock lateinit var mySiteSourceManager: MySiteSourceManager
    @Mock lateinit var cardsTracker: CardsTracker
    @Mock lateinit var siteItemsTracker: SiteItemsTracker
    @Mock lateinit var domainRegistrationCardShownTracker: DomainRegistrationCardShownTracker
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig
    @Mock lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var viewModel: MySiteViewModel
    private lateinit var uiModels: MutableList<UiModel>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var dynamicCardMenu: MutableList<DynamicCardMenuModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var showSwipeRefreshLayout: MutableList<Boolean>
    private lateinit var shareRequests: MutableList<String>
    private lateinit var trackWithTabSource: MutableList<MySiteTrackWithTabSource>
    private lateinit var tabNavigation: MutableList<TabNavigation>
    private val avatarUrl = "https://1.gravatar.com/avatar/1000?s=96&d=identicon"
    private val siteLocalId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private val emailAddress = "test@email.com"
    private val postId = 100
    private val localHomepageId = 1
    private val home = "home"
    private lateinit var site: SiteModel
    private lateinit var siteInfoHeader: SiteInfoHeaderCard
    private lateinit var homepage: PageModel
    private val onSiteChange = MutableLiveData<SiteModel>()
    private val onSiteSelected = MutableLiveData<Int>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val isDomainCreditAvailable = MutableLiveData(DomainCreditAvailable(false))
    private val showSiteIconProgressBar = MutableLiveData(ShowSiteIconProgressBar(false))
    private val selectedSite = MediatorLiveData<SelectedSite>()

    private val jetpackCapabilities = MutableLiveData(
            JetpackCapabilities(
                    scanAvailable = false,
                    backupAvailable = false
            )
    )
    private val currentAvatar = MutableLiveData(CurrentAvatarUrl(""))
    private val quickStartUpdate = MutableLiveData(QuickStartUpdate())
    private val activeTask = MutableLiveData<QuickStartTask>()
    private val quickStartSiteMenuStep = MutableLiveData<QuickStartSiteMenuStep>()
    private val dynamicCards = MutableLiveData(
            DynamicCardsUpdate(
                    cards = listOf(
                            DynamicCardType.CUSTOMIZE_QUICK_START,
                            DynamicCardType.GROW_QUICK_START
                    )
            )
    )
    private var removeMenuItemClickAction: (() -> Unit)? = null
    private var quickStartTaskTypeItemClickAction: ((QuickStartTaskType) -> Unit)? = null
    private var dynamicCardMoreClick: ((DynamicCardMenuModel) -> Unit)? = null
    private var onPostCardFooterLinkClick: ((postCardType: PostCardType) -> Unit)? = null
    private var onPostItemClick: ((params: PostItemClickParams) -> Unit)? = null
    private var onTodaysStatsCardGetMoreViewsClick: (() -> Unit) = {}
    private var onTodaysStatsCardClick: (() -> Unit) = {}
    private var onTodaysStatsCardFooterLinkClick: (() -> Unit) = {}
    private var onDashboardErrorRetryClick: (() -> Unit)? = null
    private var onBloggingPromptShareClicked: ((message: String) -> Unit)? = null
    private val quickStartCategory: QuickStartCategory
        get() = QuickStartCategory(
                taskType = QuickStartTaskType.CUSTOMIZE,
                uncompletedTasks = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE),
                completedTasks = emptyList()
        )

    private val cardsUpdate = MutableLiveData(
            CardsUpdate(
                    cards = listOf(
                            PostsCardModel(
                                    hasPublished = true,
                                    draft = listOf(
                                            PostCardModel(
                                                    id = 1,
                                                    title = "draft",
                                                    content = "content",
                                                    featuredImage = "featuredImage",
                                                    date = Date()
                                            )
                                    ),
                                    scheduled = listOf(
                                            PostCardModel(
                                                    id = 2,
                                                    title = "scheduled",
                                                    content = "",
                                                    featuredImage = null,
                                                    date = Date()
                                            )
                                    )
                            )
                    )
            )
    )

    private var quickActionsStatsClickAction: (() -> Unit)? = null
    private var quickActionsPagesClickAction: (() -> Unit)? = null
    private var quickActionsPostsClickAction: (() -> Unit)? = null
    private var quickActionsMediaClickAction: (() -> Unit)? = null

    private var quickLinkRibbonStatsClickAction: (() -> Unit)? = null
    private var quickLinkRibbonPagesClickAction: (() -> Unit)? = null
    private var quickLinkRibbonPostsClickAction: (() -> Unit)? = null
    private var quickLinkRibbonMediaClickAction: (() -> Unit)? = null

    private val partialStates = listOf(
            isDomainCreditAvailable,
            jetpackCapabilities,
            currentAvatar,
            dynamicCards,
            cardsUpdate,
            quickStartUpdate,
            showSiteIconProgressBar,
            selectedSite
    )

    @InternalCoroutinesApi
    @Suppress("LongMethod")
    @Before
    fun setUp() {
        init()
    }

    @InternalCoroutinesApi
    @Suppress("LongMethod")
    fun init(enableMySiteDashboardConfig: Boolean = false) = test {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        onSiteSelected.value = null
        selectedSite.value = null
        whenever(mySiteSourceManager.build(any(), anyOrNull())).thenReturn(partialStates)
        whenever(selectedSiteRepository.siteSelected).thenReturn(onSiteSelected)
        whenever(quickStartRepository.activeTask).thenReturn(activeTask)
        whenever(quickStartRepository.onQuickStartSiteMenuStep).thenReturn(quickStartSiteMenuStep)
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(enableMySiteDashboardConfig)
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        viewModel = MySiteViewModel(
                networkUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                analyticsTrackerWrapper,
                siteItemsBuilder,
                accountStore,
                selectedSiteRepository,
                wpMediaUtilsWrapper,
                mediaUtilsWrapper,
                fluxCUtilsWrapper,
                contextProvider,
                siteIconUploadHandler,
                siteStoriesHandler,
                displayUtilsWrapper,
                quickStartRepository,
                quickStartCardBuilder,
                siteInfoHeaderCardBuilder,
                homePageDataLoader,
                quickStartDynamicCardsFeatureConfig,
                quickStartUtilsWrapper,
                snackbarSequencer,
                cardsBuilder,
                dynamicCardsBuilder,
                landOnTheEditorFeatureConfig,
                mySiteDashboardPhase2FeatureConfig,
                mySiteSourceManager,
                cardsTracker,
                siteItemsTracker,
                domainRegistrationCardShownTracker,
                buildConfigWrapper,
                mySiteDashboardTabsFeatureConfig,
                bloggingPromptsFeatureConfig,
                appPrefsWrapper
        )
        uiModels = mutableListOf()
        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        dynamicCardMenu = mutableListOf()
        showSwipeRefreshLayout = mutableListOf()
        shareRequests = mutableListOf()
        trackWithTabSource = mutableListOf()
        tabNavigation = mutableListOf()
        launch(Dispatchers.Default) {
            viewModel.uiModel.observeForever {
                uiModels.add(it)
            }
        }
        viewModel.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
            }
        }
        viewModel.onTextInputDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                textInputDialogModels.add(it)
            }
        }
        viewModel.onBasicDialogShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                dialogModels.add(it)
            }
        }
        viewModel.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        viewModel.onDynamicCardMenuShown.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                dynamicCardMenu.add(it)
            }
        }
        viewModel.onShowSwipeRefreshLayout.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                showSwipeRefreshLayout.add(it)
            }
        }
        viewModel.onShare.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                shareRequests.add(it)
            }
        }
        viewModel.onTrackWithTabSource.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                trackWithTabSource.add(it)
            }
        }
        viewModel.selectTab.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                tabNavigation.add(it)
            }
        }
        site = SiteModel()
        site.id = siteLocalId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        site.siteId = siteLocalId.toLong()

        homepage = PageModel(PostModel(), site, localHomepageId, "home", PUBLISHED, Date(), false, 0L, null, 0L)

        setUpCardsBuilder()

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(homePageDataLoader.loadHomepage(site)).thenReturn(homepage)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    /* SITE STATE */

    @Test
    fun `given my site tabs feature flag not enabled, when site is selected, then tabs are not visible`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false)

        assertThat((uiModels.last().state as SiteSelected).tabsUiState.showTabs).isFalse
    }

    @Test
    fun `given my site tabs build config not enabled, when site is selected, then tabs are not visible`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = false)

        assertThat((uiModels.last().state as SiteSelected).tabsUiState.showTabs).isFalse
    }

    @Test
    fun `given my site tabs build config with flag enabled, when site is selected, then tabs are visible`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        assertThat((uiModels.last().state as SiteSelected).tabsUiState.showTabs).isTrue
    }

    @Test
    fun `given my site tabs build, when site is selected, then site header is visible`() {
        initSelectedSite()

        assertThat((uiModels.last().state as SiteSelected).siteInfoToolbarViewParams.headerVisible).isTrue
    }

    @Test
    fun `model is empty with no selected site`() {
        onSiteSelected.value = null
        currentAvatar.value = CurrentAvatarUrl("")

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
    }

    @Test
    fun `model contains header of selected site`() {
        initSelectedSite()

        assertThat(uiModels.last().state).isInstanceOf(SiteSelected::class.java)

        assertThat(getSiteInfoHeaderCard()).isInstanceOf(SiteInfoHeaderCard::class.java)
    }

    @Test
    fun `when selected site is changed, then cardTracker is reset`() = test {
        initSelectedSite()

        verify(cardsTracker, atLeastOnce()).resetShown()
    }

    @Test
    fun `when selected site is changed, then cardShownTracker is reset`() = test {
        initSelectedSite()

        verify(domainRegistrationCardShownTracker, atLeastOnce()).resetShown()
    }

    /* SELECTED SITE - DEFAULT TAB */

    @Test
    fun `given tabs not enabled, when site is selected, then default tab is not set`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false)

        assertThat(tabNavigation).isEmpty()
    }

    @Test
    fun `given tabs enabled + initial screen is home, when site is selected, then default tab is dashboard`() {
        whenever(appPrefsWrapper.getMySiteInitialScreen()).thenReturn(home)

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = home
        )

        assertThat(tabNavigation)
                .containsOnly(TabNavigation(viewModel.orderedTabTypes.indexOf(DASHBOARD), false))
    }

    @Test
    fun `given tabs enabled + initial screen is site_menu, when site is selected, then default tab is site menu`() {
        whenever(appPrefsWrapper.getMySiteInitialScreen()).thenReturn(SITE_MENU.label)
        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = SITE_MENU.label
        )

        assertThat(tabNavigation)
                .containsOnly(TabNavigation(viewModel.orderedTabTypes.indexOf(SITE_MENU), false))
    }

    /* CREATE SITE - DEFAULT TAB */

    @Test
    fun `given tabs enabled, when site is created, then default tab is set`() {
        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = SITE_MENU.label
        )

        viewModel.onCreateSiteResult()

        assertThat(tabNavigation).size().isEqualTo(2)
        /* First time default tab is set when My Site screen is shown and site is selected.
           When site is created then again it sets the default tab. */
        assertThat(tabNavigation.last())
                .isEqualTo(TabNavigation(viewModel.orderedTabTypes.indexOf(SITE_MENU), false))
    }

    /* AVATAR */

    @Test
    fun `account avatar url value is emitted and updated from the source`() {
        initSelectedSite()

        currentAvatar.value = CurrentAvatarUrl(avatarUrl)

        assertThat(uiModels.last().accountAvatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenMeScreen)
    }

    /* LOGIN - NAVIGATION TO STATS */

    @Test
    fun `handling successful login result opens stats screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    /* EMPTY VIEW */

    @Test
    fun `when no site is selected, then tabs are not visible`() {
        onSiteSelected.value = null

        assertThat((uiModels.last().state as NoSites).tabsUiState.showTabs).isFalse
    }

    @Test
    fun `given no selected site, then site info header is not visible `() {
        onSiteSelected.value = null

        assertThat((uiModels.last().state as NoSites).siteInfoToolbarViewParams.headerVisible).isFalse
    }

    @Test
    fun `given wp app, when no site is selected and screen height is higher than 600 pixels, show empty view image`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(600)

        onSiteSelected.value = null

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
        assertThat((uiModels.last().state as NoSites).shouldShowImage).isTrue
    }

    @Test
    fun `given wp app, when no site is selected and screen height is lower than 600 pixels, hide empty view image`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        whenever(displayUtilsWrapper.getDisplayPixelHeight()).thenReturn(500)

        onSiteSelected.value = null

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
        assertThat((uiModels.last().state as NoSites).shouldShowImage).isFalse
    }

    @Test
    fun `given jp app, when no site is selected, hide empty view image`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        onSiteSelected.value = null

        assertThat(uiModels.last().state).isInstanceOf(NoSites::class.java)
        assertThat((uiModels.last().state as NoSites).shouldShowImage).isFalse
    }

    /* EMPTY VIEW - ADD SITE */

    @Test
    fun `add new site press is handled correctly`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onAddSitePressed()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.AddNewSite(true))
    }

    /* ON RESUME */
    @Test
    fun `given not first resume, when on resume is triggered, then mySiteSourceManager onResume is invoked`() {
        viewModel.onResume() // first call

        viewModel.onResume() // second call

        verify(mySiteSourceManager).onResume(false)
    }

    @Test
    fun `given first resume, when on resume is triggered, then mySiteSourceManager onResume is invoked`() {
        viewModel.onResume()

        verify(mySiteSourceManager).onResume(true)
    }

    @Test
    fun `when first onResume is triggered, then checkAndShowQuickStartNotice is invoked`() {
        viewModel.onResume()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* REFRESH */
    @Test
    fun `when sources are refreshing, then refresh indicator should show`() {
        whenever(mySiteSourceManager.isRefreshing()).thenReturn(true)

        val result = viewModel.isRefreshing()

        assertThat(result).isTrue
    }

    @Test
    fun `when sources are not refreshing, then refresh indicator should not show`() {
        whenever(mySiteSourceManager.isRefreshing()).thenReturn(false)

        val result = viewModel.isRefreshing()

        assertThat(result).isFalse
    }

    @Test
    fun `when clear active quick start task is triggered, then clear active quick start task`() {
        viewModel.clearActiveQuickStartTask()

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `when check and show quick start notice is triggered, then check and show quick start notice`() {
        viewModel.checkAndShowQuickStartNotice()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* SITE INFO CARD */

    @Test
    fun `site info card title click shows snackbar message when network not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        )
    }

    @Test
    fun `site info card title click shows snackbar message when hasCapabilityManageOptions is false`() = test {
        site.hasCapabilityManageOptions = false
        site.origin = SiteModel.ORIGIN_WPCOM_REST

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(
                        UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint)
                )
        )
    }

    @Test
    fun `site info card title click shows snackbar message when origin not ORIGIN_WPCOM_REST`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_XMLRPC

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        assertThat(textInputDialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
        )
    }

    @Test
    fun `site info card title click shows input dialog when editing allowed`() = test {
        site.hasCapabilityManageOptions = true
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.TITLE_CLICK)

        assertThat(snackbars).isEmpty()
        assertThat(textInputDialogModels.last()).isEqualTo(
                TextInputDialogModel(
                        callbackId = MySiteViewModel.SITE_NAME_CHANGE_CALLBACK_ID,
                        title = R.string.my_site_title_changer_dialog_title,
                        initialText = siteName,
                        hint = R.string.my_site_title_changer_dialog_hint,
                        isMultiline = false,
                        isInputEnabled = true
                )
        )
    }

    @Test
    fun `site info card icon click shows change icon dialog when site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = siteIcon

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(ChangeSiteIconDialogModel)
    }

    @Test
    fun `site info card icon click shows add icon dialog when site doesn't have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = true
        site.iconUrl = null

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        assertThat(dialogModels.last()).isEqualTo(AddSiteIconDialogModel)
    }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site doesn't have Jetpack`() =
            test {
                site.hasCapabilityManageOptions = true
                site.hasCapabilityUploadFiles = false
                site.setIsWPCom(false)

                invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

                assertThat(dialogModels).isEmpty()
                assertThat(snackbars).containsOnly(
                        SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_jetpack_message))
                )
            }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site has icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = siteIcon

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_change_requires_permission_message))
        )
    }

    @Test
    fun `site info card icon click shows snackbar when upload files not allowed and site does not have icon`() = test {
        site.hasCapabilityManageOptions = true
        site.hasCapabilityUploadFiles = false
        site.setIsWPCom(true)
        site.iconUrl = null

        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.ICON_CLICK)

        assertThat(dialogModels).isEmpty()
        assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_icon_dialog_add_requires_permission_message))
        )
    }

    @Test
    fun `on site name chosen updates title if network available `() = test {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository).updateTitle(title)
    }

    @Test
    fun `on site name chosen shows snackbar if network not available `() = test {
        val title = "updated site name"
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onSiteNameChosen(title)

        verify(selectedSiteRepository, never()).updateTitle(any())
        assertThat(snackbars).containsOnly(SnackbarMessageHolder(UiStringRes(R.string.error_update_site_title_network)))
    }

    @Test
    fun `site info card url click opens site`() = test {
        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.URL_CLICK)

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenSite(site))
    }

    @Test
    fun `site info card switch click opens site picker`() = test {
        invokeSiteInfoCardAction(SiteInfoHeaderCardAction.SWITCH_SITE_CLICK)

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenSitePicker(site))
    }

    /* QUICK ACTIONS CARD */
    @Test
    fun `quick actions does not show pages button when site doesn't have the required capability`() {
        site.hasCapabilityEditPages = false

        initSelectedSite()

        val quickActionsCard = findQuickActionsCard()

        assertThat(quickActionsCard).isNotNull
        assertThat(quickActionsCard?.showPages).isFalse
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is WPCOM`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsWPCom(true)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `quick action stats click opens stats screen when user is logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click starts login when user is not logged in and site is Jetpack`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.StartWPComLoginForJetpackStats)
    }

    @Test
    fun `quick action stats click opens connect jetpack screen when user is not logged in and site is self-hosted`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.ConnectJetpackForStats(site))
    }

    @Test
    fun `quick action stats click completes CHECK_STATS task`() {
        initSelectedSite()

        requireNotNull(quickActionsStatsClickAction).invoke()

        verify(quickStartRepository).completeTask(QuickStartTask.CHECK_STATS)
    }

    @Test
    fun `quick action pages click opens pages screen and requests next step of EDIT_HOMEPAGE task`() {
        initSelectedSite()

        requireNotNull(quickActionsPagesClickAction).invoke()

        verify(quickStartRepository).requestNextStepOfTask(QuickStartTask.EDIT_HOMEPAGE)
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `quick action pages click opens pages screen and completes REVIEW_PAGES task`() {
        initSelectedSite()

        requireNotNull(quickActionsPagesClickAction).invoke()

        verify(quickStartRepository).completeTask(QuickStartTask.REVIEW_PAGES)
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `quick action posts click opens posts screen`() {
        initSelectedSite()

        requireNotNull(quickActionsPostsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPosts(site))
    }

    @Test
    fun `quick action media click opens media screen`() {
        initSelectedSite()

        requireNotNull(quickActionsMediaClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenMedia(site))
    }

    /* DOMAIN REGISTRATION CARD */
    @Test
    fun `domain registration item click opens domain registration`() {
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        findDomainRegistrationCard()?.onClick?.click()

        verify(analyticsTrackerWrapper).track(Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED, site)

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenDomainRegistration(site))
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result without email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(null)

        verify(analyticsTrackerWrapper).track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringRes(R.string.my_site_verify_your_email_without_email)

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result with email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(emailAddress)

        verify(analyticsTrackerWrapper).track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringResWithParams(R.string.my_site_verify_your_email, listOf(UiStringText(emailAddress)))

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `when domain registration card is shown, then card shown event is tracked`() = test {
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        verify(
                domainRegistrationCardShownTracker,
                atLeastOnce()
        ).trackShown(MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD)
    }

    /* QUICK START CARD */
    @Test
    fun `when quick start task type item is clicked, then quick start full screen dialog is opened`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        assertThat(navigationActions.last())
                .isInstanceOf(SiteNavigationAction.OpenQuickStartFullScreenDialog::class.java)
    }

    @Test
    fun `when quick start task type item is clicked, then quick start active task is cleared`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `given dynamic card disabled, when QS remove menu item is clicked, then remove next steps dialog shown`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        requireNotNull(removeMenuItemClickAction).invoke()

        assertThat(dialogModels.last()).isEqualTo(ShowRemoveNextStepsDialog)
    }

    @Test
    fun `when remove next steps dialog negative btn clicked, then QS is not skipped`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository, never()).skipQuickStart()
    }

    @Test
    fun `when remove next steps dialog positive btn clicked, then QS is skipped`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository).skipQuickStart()
    }

    @Test
    fun `when remove next steps dialog positive btn clicked, then QS repo refreshed`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(mySiteSourceManager).refresh()
    }

    @Test
    fun `when remove next steps dialog positive btn clicked, then QS active task cleared`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `when QS fullscreen dialog dismiss is triggered, then quick start repository is refreshed`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onQuickStartFullScreenDialogDismiss()

        verify(mySiteSourceManager).refreshQuickStart()
    }

    @Test
    fun `when quick start task is clicked, then task is set as active task`() {
        val task = QuickStartTask.VIEW_SITE
        initSelectedSite(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        viewModel.onQuickStartTaskCardClick(task)

        verify(quickStartRepository).setActiveTask(task)
    }

    /* START/IGNORE QUICK START + QUICK START DIALOG */

    @Test
    fun `given QS dynamic cards cards feature is on, when check and start QS is triggered, then QS starts`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.checkAndStartQuickStart(siteLocalId)

        verify(quickStartUtilsWrapper).startQuickStart(siteLocalId)
        verify(mySiteSourceManager).refreshQuickStart()
    }

    @Test
    fun `given no selected site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.checkAndStartQuickStart(siteLocalId)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given QS is not available for the site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)

        viewModel.checkAndStartQuickStart(siteLocalId)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `when check and start QS is triggered, then QSP is shown`() {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)

        viewModel.checkAndStartQuickStart(siteLocalId)

        assertThat(navigationActions).containsExactly(
                SiteNavigationAction.ShowQuickStartDialog(
                        R.string.quick_start_dialog_need_help_manage_site_title,
                        R.string.quick_start_dialog_need_help_manage_site_message,
                        R.string.quick_start_dialog_need_help_manage_site_button_positive,
                        R.string.quick_start_dialog_need_help_button_negative
                )
        )
    }

    @Test
    fun `when start QS is triggered, then QS request dialog positive tapped is tracked`() {
        viewModel.startQuickStart()

        verify(analyticsTrackerWrapper).track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
    }

    @Test
    fun `when start QS is triggered, then QS starts`() {
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(site.id)

        viewModel.startQuickStart()

        verify(quickStartUtilsWrapper).startQuickStart(site.id)
        verify(mySiteSourceManager).refreshQuickStart()
    }

    @Test
    fun `when ignore QS is triggered, then QS request dialog negative tapped is tracked`() {
        viewModel.ignoreQuickStart()

        verify(analyticsTrackerWrapper).track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    /* QUICK START SITE MENU STEP */

    @Test
    fun `when quick start menu step is triggered, then site menu tab has quick start focus point`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        quickStartSiteMenuStep.value = QuickStartSiteMenuStep(true, QuickStartTask.VIEW_SITE)

        assertThat((uiModels.last().state as SiteSelected).findSiteMenuTabUiState().showQuickStartFocusPoint).isTrue
    }

    @Test
    fun `given site menu tab has qs focus point, when tab is changed, then qs focus point is cleared`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)
        val pendingTask = QuickStartTask.VIEW_SITE
        quickStartSiteMenuStep.value = QuickStartSiteMenuStep(true, pendingTask)

        viewModel.onTabChanged(viewModel.orderedTabTypes.indexOf(MySiteTabType.SITE_MENU))

        verify(quickStartRepository).clearSiteMenuStep()
    }

    @Test
    fun `given site menu tab has qs focus point, when tab is changed, then site menu pending task is active`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)
        val pendingTask = QuickStartTask.VIEW_SITE
        quickStartSiteMenuStep.value = QuickStartSiteMenuStep(true, pendingTask)

        viewModel.onTabChanged(viewModel.orderedTabTypes.indexOf(MySiteTabType.SITE_MENU))

        verify(quickStartRepository).setActiveTask(pendingTask)
    }

    /* DYNAMIC QUICK START CARD */

    @Test
    fun `when dynamic quick start more menu is clicked, then dynamic card menu is shown`() {
        initSelectedSite(isQuickStartDynamicCardEnabled = true)

        findQuickStartDynamicCard()!!.onMoreClick.click()

        assertThat(dynamicCardMenu.last()).isNotNull
    }

    @Test
    fun `when dynamic QS hide menu item is clicked, then the request is passed to MySiteSourceManager`() = test {
        val id = DynamicCardType.CUSTOMIZE_QUICK_START
        viewModel.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Hide(id))

        verify(mySiteSourceManager).onQuickStartMenuInteraction(DynamicCardMenuInteraction.Hide(id))
    }

    @Test
    fun `when dynamic QS remove menu item is clicked, then the request is passed to MySiteSourceManager`() = test {
        val id = DynamicCardType.CUSTOMIZE_QUICK_START
        viewModel.onQuickStartMenuInteraction(DynamicCardMenuInteraction.Remove(id))

        verify(mySiteSourceManager).onQuickStartMenuInteraction(DynamicCardMenuInteraction.Remove(id))
    }

    /* DASHBOARD TODAYS STATS CARD */

    @Test
    fun `given todays stat card, when card item is clicked, then stats page is opened`() =
            test {
                initSelectedSite()

                onTodaysStatsCardClick.invoke()

                assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStatsInsights(site))
            }

    @Test
    fun `given todays stat card, when footer link is clicked, then stats page is opened`() =
            test {
                initSelectedSite()

                onTodaysStatsCardFooterLinkClick.invoke()

                assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStatsInsights(site))
            }

    @Test
    fun `given todays stat card, when get more views url is clicked, then external link is opened`() =
            test {
                initSelectedSite()

                onTodaysStatsCardGetMoreViewsClick.invoke()

                assertThat(navigationActions)
                        .containsOnly(
                                SiteNavigationAction.OpenTodaysStatsGetMoreViewsExternalUrl(
                                        URL_GET_MORE_VIEWS_AND_TRAFFIC
                                )
                        )
            }

    /* DASHBOARD POST CARD - FOOTER LINK */

    @Test
    fun `given create first card, when footer link is clicked, then editor is opened to create new post`() =
            test {
                initSelectedSite()

                requireNotNull(onPostCardFooterLinkClick).invoke(PostCardType.CREATE_FIRST)

                assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenEditorToCreateNewPost(site))
            }

    @Test
    fun `given create next card, when footer link is clicked, then editor is opened to create new post`() =
            test {
                initSelectedSite()

                requireNotNull(onPostCardFooterLinkClick).invoke(PostCardType.CREATE_NEXT)

                assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenEditorToCreateNewPost(site))
            }

    @Test
    fun `given draft post card, when footer link is clicked, then draft posts screen is opened`() = test {
        initSelectedSite()

        requireNotNull(onPostCardFooterLinkClick).invoke(PostCardType.DRAFT)

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenDraftsPosts(site))
    }

    @Test
    fun `given scheduled post card, when footer link is clicked, then scheduled posts screen is opened`() =
            test {
                initSelectedSite()

                requireNotNull(onPostCardFooterLinkClick).invoke(PostCardType.SCHEDULED)

                assertThat(navigationActions)
                        .containsOnly(SiteNavigationAction.OpenScheduledPosts(site))
            }

    @Test
    fun `given post card, when footer link is clicked, then event is tracked`() = test {
        initSelectedSite()

        requireNotNull(onPostCardFooterLinkClick).invoke(PostCardType.SCHEDULED)

        verify(cardsTracker).trackPostCardFooterLinkClicked(PostCardType.SCHEDULED)
    }

    @Test
    fun `when dashboard cards are shown, then card shown event is tracked`() = test {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite()

        verify(cardsTracker, atLeastOnce()).trackShown(any())
    }

    @Test
    fun `when dashboard cards are not shown, then card shown events are not tracked`() = test {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)
        initSelectedSite()

        verify(cardsTracker, never()).trackShown(any())
    }

    /* DASHBOARD POST CARD - POST ITEM */

    @Test
    fun `given draft post card, when post item is clicked, then post is opened for edit draft`() =
            test {
                initSelectedSite()

                requireNotNull(onPostItemClick).invoke(PostItemClickParams(PostCardType.DRAFT, postId))

                assertThat(navigationActions).containsOnly(SiteNavigationAction.EditDraftPost(site, postId))
            }

    @Test
    fun `given scheduled post card, when post item is clicked, then post is opened for edit scheduled`() =
            test {
                initSelectedSite()

                requireNotNull(onPostItemClick).invoke(PostItemClickParams(PostCardType.SCHEDULED, postId))

                assertThat(navigationActions).containsOnly(SiteNavigationAction.EditScheduledPost(site, postId))
            }

    @Test
    fun `given post card, when item is clicked, then event is tracked`() = test {
        initSelectedSite()

        requireNotNull(onPostItemClick).invoke(PostItemClickParams(PostCardType.SCHEDULED, postId))

        verify(cardsTracker).trackPostItemClicked(PostCardType.SCHEDULED)
    }

    /* DASHBOARD BLOGGING PROMPT CARD */

    @Test
    fun `blogging prompt card is added to the dashboard when FF is ON`() = test {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)

        initSelectedSite()

        verify(cardsBuilder).build(
                any(), any(), any(),
                argWhere {
                    it.bloggingPromptCardBuilderParams.bloggingPrompt != null
                },
                any()
        )
    }

    @Test
    fun `blogging prompt card is not added to the dashboard when FF is OFF`() = test {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)

        initSelectedSite()

        verify(cardsBuilder).build(
                any(), any(), any(),
                argWhere {
                    it.bloggingPromptCardBuilderParams.bloggingPrompt == null
                },
                any()
        )
    }

    @Test
    fun `given blogging prompt card, when share button is clicked, share action is called`() = test {
        initSelectedSite()

        val expectedShareMessage = "Test prompt"

        requireNotNull(onBloggingPromptShareClicked).invoke(expectedShareMessage)

        assertThat(shareRequests.last()).isEqualTo(expectedShareMessage)
    }

    /* DASHBOARD ERROR SNACKBAR */

    @Test
    fun `given show snackbar in cards update, when dashboard cards updated, then dashboard snackbar shown`() =
            test {
                initSelectedSite()

                cardsUpdate.value = cardsUpdate.value?.copy(showSnackbarError = true)

                assertThat(snackbars).containsOnly(
                        SnackbarMessageHolder(UiStringRes(R.string.my_site_dashboard_update_error))
                )
            }

    @Test
    fun `given show snackbar not in cards update, when dashboard cards updated, then dashboard snackbar not shown`() =
            test {
                initSelectedSite()

                cardsUpdate.value = cardsUpdate.value?.copy(showSnackbarError = false)

                assertThat(snackbars).doesNotContain(
                        SnackbarMessageHolder(UiStringRes(R.string.my_site_dashboard_update_error))
                )
            }

    /* DASHBOARD ERROR CARD - RETRY */

    @Test
    fun `given error dashboard card, when retry is clicked, then refresh is triggered`() =
            test {
                initSelectedSite()
                cardsUpdate.value = cardsUpdate.value?.copy(showErrorCard = true)

                requireNotNull(onDashboardErrorRetryClick).invoke()

                verify(mySiteSourceManager).refresh()
            }

    /* INFO ITEM */

    @Test
    fun `given show stale msg not in cards update, when dashboard cards updated, then info item not shown`() {
        initSelectedSite(showStaleMessage = false)

        cardsUpdate.value = cardsUpdate.value?.copy(showStaleMessage = false)

        assertThat((uiModels.last().state as SiteSelected).cardAndItems.filterIsInstance(InfoItem::class.java))
                .isEmpty()
    }

    @Test
    fun `given show stale msg in cards update, when dashboard cards updated, then info item shown`() {
        initSelectedSite(showStaleMessage = true)

        cardsUpdate.value = cardsUpdate.value?.copy(showStaleMessage = true)

        assertThat((uiModels.last().state as SiteSelected).cardAndItems.filterIsInstance(InfoItem::class.java))
                .isNotEmpty
    }

    /* ITEM CLICK */

    @Test
    fun `activity item click emits OpenActivity navigation event`() {
        invokeItemClickAction(ListItemAction.ACTIVITY_LOG)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenActivityLog(site))
    }

    @Test
    fun `scan item click emits OpenScan navigation event`() {
        invokeItemClickAction(ListItemAction.SCAN)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenScan(site))
    }

    @Test
    fun `plan item click emits OpenPlan navigation event and completes EXPLORE_PLANS quick task`() {
        invokeItemClickAction(ListItemAction.PLAN)

        verify(quickStartRepository).completeTask(QuickStartTask.EXPLORE_PLANS)
        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPlan(site))
    }

    @Test
    fun `posts item click emits OpenPosts navigation event`() {
        invokeItemClickAction(ListItemAction.POSTS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPosts(site))
    }

    @Test
    fun `pages item click emits OpenPages navigation event`() {
        invokeItemClickAction(ListItemAction.PAGES)

        verify(quickStartRepository).completeTask(QuickStartTask.REVIEW_PAGES)
        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `admin item click emits OpenAdmin navigation event`() {
        invokeItemClickAction(ListItemAction.ADMIN)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenAdmin(site))
    }

    @Test
    fun `sharing item click emits OpenSharing navigation event`() {
        invokeItemClickAction(ListItemAction.SHARING)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenSharing(site))
    }

    @Test
    fun `site settings item click emits OpenSiteSettings navigation event`() {
        invokeItemClickAction(ListItemAction.SITE_SETTINGS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenSiteSettings(site))
    }

    @Test
    fun `themes item click emits OpenThemes navigation event`() {
        invokeItemClickAction(ListItemAction.THEMES)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenThemes(site))
    }

    @Test
    fun `plugins item click emits OpenPlugins navigation event`() {
        invokeItemClickAction(ListItemAction.PLUGINS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenPlugins(site))
    }

    @Test
    fun `media item click emits OpenMedia navigation event`() {
        invokeItemClickAction(ListItemAction.MEDIA)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenMedia(site))
    }

    @Test
    fun `comments item click emits OpenUnifiedComments navigation event`() {
        invokeItemClickAction(ListItemAction.COMMENTS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenUnifiedComments(site))
    }

    @Test
    fun `view site item click emits OpenSite navigation event`() {
        invokeItemClickAction(ListItemAction.VIEW_SITE)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenSite(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is WPCom and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsWPCom(true)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `stats item click emits OpenStats navigation event if site is Jetpack and has access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        site.setIsJetpackConnected(true)
        site.setIsJetpackInstalled(true)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `stats item click completes CHECK_STATS task`() {
        invokeItemClickAction(ListItemAction.STATS)

        verify(quickStartRepository).completeTask(QuickStartTask.CHECK_STATS)
    }

    @Test
    fun `stats item click emits StartWPComLoginForJetpackStats if site is Jetpack and doesn't have access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(true)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.StartWPComLoginForJetpackStats)
    }

    @Test
    fun `stats item click emits ConnectJetpackForStats if neither Jetpack, nor WPCom and no access token`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        site.setIsJetpackConnected(false)
        site.setIsWPCom(false)

        invokeItemClickAction(ListItemAction.STATS)

        assertThat(navigationActions).containsExactly(SiteNavigationAction.ConnectJetpackForStats(site))
    }

    @Test
    fun `when site item is clicked, then event is tracked`() = test {
        invokeItemClickAction(ListItemAction.POSTS)

        verify(siteItemsTracker).trackSiteItemClicked(ListItemAction.POSTS)
    }

    /* ITEM VISIBILITY */

    @Test
    fun `backup menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = false)

        verify(siteItemsBuilder, times(1)).build(any<SiteItemsBuilderParams>())
    }

    @Test
    fun `scan menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = false)

        verify(siteItemsBuilder, times(1)).build(any<SiteItemsBuilderParams>())
    }

    @Test
    fun `scan menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = true, backupAvailable = false)

        verify(siteItemsBuilder, times(2)).build(any<SiteItemsBuilderParams>())
    }

    @Test
    fun `backup menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = true)

        verify(siteItemsBuilder, times(2)).build(any<SiteItemsBuilderParams>())
    }

    /* ADD SITE ICON DIALOG */

    @Test
    fun `when add site icon dialog +ve btn is clicked, then upload site icon task marked complete without refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = QuickStartTask.UPLOAD_SITE_ICON)
        verify(mySiteSourceManager, never()).refreshQuickStart()
    }

    @Test
    fun `when change site icon dialog +ve btn clicked, then upload site icon task marked complete without refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = QuickStartTask.UPLOAD_SITE_ICON)
        verify(mySiteSourceManager, never()).refreshQuickStart()
    }

    @Test
    fun `when add site icon dialog -ve btn is clicked, then upload site icon task marked complete without refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = QuickStartTask.UPLOAD_SITE_ICON)
        verify(mySiteSourceManager, never()).refreshQuickStart()
    }

    @Test
    fun `when change site icon dialog -ve btn is clicked, then upload site icon task marked complete no refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = QuickStartTask.UPLOAD_SITE_ICON)
        verify(mySiteSourceManager, never()).refreshQuickStart()
    }

    @Test
    fun `when site icon dialog is dismissed, then upload site icon task is marked complete without refresh`() {
        viewModel.onDialogInteraction(DialogInteraction.Dismissed(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).completeTask(task = QuickStartTask.UPLOAD_SITE_ICON)
        verify(mySiteSourceManager, never()).refreshQuickStart()
    }

    @Test
    fun `when add site icon dialog positive button is clicked, then media picker is opened`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenMediaPicker(site))
    }

    @Test
    fun `when change site icon dialog positive button is clicked, then media picker is opened`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.onDialogInteraction(DialogInteraction.Positive(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenMediaPicker(site))
    }

    @Test
    fun `when add site icon dialog negative button is clicked, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when change site icon dialog negative button is clicked, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when add site icon dialog is dismissed, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Dismissed(MySiteViewModel.TAG_ADD_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    @Test
    fun `when change site icon dialog is dismissed, then check and show quick start notice`() {
        viewModel.onDialogInteraction(DialogInteraction.Dismissed(MySiteViewModel.TAG_CHANGE_SITE_ICON_DIALOG))

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* SITE CHOOSER DIALOG */

    @Test
    fun `when site chooser is dismissed, then check and show quick start notice`() {
        viewModel.onSiteNameChooserDismissed()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* SWIPE REFRESH */
    @InternalCoroutinesApi
    @Test
    fun `given not first resume and phase 2 enabled, when on resume, then swipe refresh layout is enabled`() = test {
        init(enableMySiteDashboardConfig = true)
        viewModel.onResume() // first call

        viewModel.onResume() // second call

        assertThat(showSwipeRefreshLayout.last()).isEqualTo(true)
    }

    @InternalCoroutinesApi
    @Test
    fun `given not first resume and phase 2 disabled, when on resume, then swipe refresh layout is disabled`() = test {
        init(enableMySiteDashboardConfig = false)
        viewModel.onResume() // first call

        viewModel.onResume() // second call

        assertThat(showSwipeRefreshLayout.last()).isEqualTo(false)
    }

    @InternalCoroutinesApi
    @Test
    fun `given first resume and phase 2 enabled, when on resume, then swipe refresh layout is enabled`() = test {
        init(enableMySiteDashboardConfig = true)

        viewModel.onResume()

        assertThat(showSwipeRefreshLayout.last()).isEqualTo(true)
    }

    @InternalCoroutinesApi
    @Test
    fun `given first resume and phase 2 disabled, when on resume, then swipe refresh layout is disabled`() = test {
        init(enableMySiteDashboardConfig = false)

        viewModel.onResume()

        assertThat(showSwipeRefreshLayout.last()).isEqualTo(false)
    }

    @Test
    fun `given refresh, when not invoked as PTR, then pull-to-refresh request is not tracked`() {
        initSelectedSite()

        viewModel.refresh()

        verify(analyticsTrackerWrapper, times(0)).track(Stat.MY_SITE_PULL_TO_REFRESH)
    }

    /* CLEARED */
    @Test
    fun `when vm cleared() is invoked, then MySiteSource clear() is invoked`() {
        viewModel.invokeOnCleared()

        verify(mySiteSourceManager).clear()
    }

    /* LAND ON THE EDITOR A/B EXPERIMENT */
    @Test
    fun `given the land on the editor feature is enabled, then the home page editor is shown`() = test {
        whenever(landOnTheEditorFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.performFirstStepAfterSiteCreation(siteLocalId)

        verify(analyticsTrackerWrapper).track(Stat.LANDING_EDITOR_SHOWN)
        assertThat(navigationActions).containsExactly(SiteNavigationAction.OpenHomepage(site, localHomepageId))
    }

    @Test
    fun `given the land on the editor feature is not enabled, then the home page editor is not shown`() = test {
        whenever(landOnTheEditorFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.performFirstStepAfterSiteCreation(siteLocalId)

        assertThat(navigationActions).isEmpty()
    }

    /* ORDERED LIST */

    @InternalCoroutinesApi
    @Test
    fun `given info item exist, when cardAndItems list is ordered, then info item succeeds site info card`() {
        initSelectedSite(showStaleMessage = true)
        cardsUpdate.value = cardsUpdate.value?.copy(showStaleMessage = true)

        val siteInfoCardIndex = getLastItems().indexOfFirst { it is SiteInfoHeaderCard }
        val infoItemIndex = getLastItems().indexOfFirst { it is InfoItem }

        assertThat(infoItemIndex).isEqualTo(siteInfoCardIndex + 1)
    }

    @InternalCoroutinesApi
    @Test
    fun `given no post cards exist, when cardAndItems list is ordered, then dynamic card follow all cards`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(false)
        initSelectedSite(isQuickStartDynamicCardEnabled = true)

        assertThat(getLastItems().last()).isInstanceOf(DynamicCard::class.java)
    }

    @InternalCoroutinesApi
    @Test
    fun `given dashboard cards exist, when cardAndItems list is ordered, then dynamic cards precede dashboard cards`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite(isQuickStartDynamicCardEnabled = true)

        val dashboardCardsIndex = getLastItems().indexOfFirst { it is DashboardCards }
        val dynamicCardIndex = getLastItems().indexOfFirst { it is DynamicCard }

        assertThat(dynamicCardIndex).isLessThan(dashboardCardsIndex)
    }

    /* STATE LISTS */
    @Test
    fun `given site select exists, then cardAndItem lists are not empty`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite(isQuickStartDynamicCardEnabled = false)

        assertThat(getLastItems().isNotEmpty())
        assertThat(getDashboardTabLastItems().isNotEmpty())
        assertThat(getSiteMenuTabLastItems().isNotEmpty())
    }

    @Test
    fun `given selected site with tabs disabled, when all cards and items, then qs card exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false)

        val items = (uiModels.last().state as SiteSelected).cardAndItems

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isNotEmpty
    }

    @Test
    fun `given selected site, when dashboard cards and items, then dashboard cards exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite()

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(DashboardCards::class.java)).isNotEmpty
    }

    @Test
    fun `given selected site, when dashboard cards and items, then list items not exist`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()
        initSelectedSite()

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(ListItem::class.java)).isEmpty()
    }

    @Test
    fun `given tabs enabled + dashboard variant, when dashboard cards items, then qs card exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = home
        )

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isNotEmpty
    }

    @Test
    fun `given tabs enabled + site menu default tab variant, when dashboard cards items, then qs card not exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = SITE_MENU.label
        )

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isEmpty()
    }

    @Test
    fun `given selected site, when site menu cards and items, then dashboard cards not exist`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()
        initSelectedSite()

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(DashboardCards::class.java)).isEmpty()
    }

    @Test
    fun `given selected site, when site menu cards and items, then list items exist`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()
        initSelectedSite()

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(ListItem::class.java)).isNotEmpty
    }

    @Test
    fun `given tabs enabled + dashboard default tab variant, when site menu cards + items, then qs card not exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = home
        )

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isEmpty()
    }

    @Test
    fun `given tabs enabled + site menu default tab variant, when site menu cards and items, then qs card exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = MySiteTabType.SITE_MENU.label
        )

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isNotEmpty
    }

    @Test
    fun `given selected site with domain credit, when dashboard cards + items, then domain reg card does not exist`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(DomainRegistrationCard::class.java)).isEmpty()
    }

    @Test
    fun `given selected site with domain credit, when site menu cards and items, then domain reg card exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(DomainRegistrationCard::class.java)).isNotEmpty
    }

    @Test
    fun `given site menu tab is selected, when tab is changed, then site menu events are tracked`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        viewModel.onTabChanged(viewModel.orderedTabTypes.indexOf(MySiteTabType.SITE_MENU))

        verify(analyticsTrackerWrapper, atLeastOnce()).track(
                Stat.MY_SITE_TAB_TAPPED,
                mapOf(MySiteViewModel.MY_SITE_TAB to MySiteTabType.SITE_MENU.label)
        )
        verify(analyticsTrackerWrapper, atLeastOnce()).track(Stat.MY_SITE_SITE_MENU_SHOWN)
    }

    @Test
    fun `given dashboard tab is selected, when tab is changed, then dashboard events are tracked`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        viewModel.onTabChanged(viewModel.orderedTabTypes.indexOf(MySiteTabType.DASHBOARD))

        verify(analyticsTrackerWrapper, atLeastOnce()).track(
                Stat.MY_SITE_TAB_TAPPED,
                mapOf(MySiteViewModel.MY_SITE_TAB to MySiteTabType.DASHBOARD.label)
        )
        verify(analyticsTrackerWrapper, atLeastOnce()).track(Stat.MY_SITE_DASHBOARD_SHOWN)
    }

    @Test
    fun `given selected site, when site menu cards and items, then site info header has updates`() {
        initSelectedSite()

        val siteInfoHeaderCard = (uiModels.last().state as SiteSelected).siteInfoHeaderState.hasUpdates

        assertThat(siteInfoHeaderCard).isTrue
    }

    @Test
    fun `given tabs enabled + site menu initial screen, when site menu cards + items, then dynamic card exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = SITE_MENU.label,
                isQuickStartDynamicCardEnabled = true
        )

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(DynamicCard::class.java)).isNotEmpty
    }

    @Test
    fun `given tabs enabled + dashboard initial screen, when dashboard cards + items, then dynamic card exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = home,
                isQuickStartDynamicCardEnabled = true
        )

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(DynamicCard::class.java)).isNotEmpty
    }

    @Test
    fun `given tabs enabled + site menu initial screen, when dashboard cards + items, then dynamic card not exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = SITE_MENU.label,
                isQuickStartDynamicCardEnabled = true
        )

        val items = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

        assertThat(items.filterIsInstance(DynamicCard::class.java)).isEmpty()
    }

    @Test
    fun `given tabs enabled + dashboard initial screen, when site menu cards + items, then dynamic card not exists`() {
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(true)
        setUpSiteItemBuilder()

        initSelectedSite(
                isMySiteDashboardTabsFeatureFlagEnabled = true,
                isMySiteTabsBuildConfigEnabled = true,
                initialScreen = home,
                isQuickStartDynamicCardEnabled = true
        )

        val items = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

        assertThat(items.filterIsInstance(DynamicCard::class.java)).isEmpty()
    }

    /* TRACK WITH TAB SOURCE */
    @Test
    fun `given tabs are enabled, when pull to refresh invoked, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        viewModel.refresh(true)

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.MY_SITE_PULL_TO_REFRESH)
    }

    @Test
    fun `given tabs are disabled, when pull to refresh invoked, then track with tab source is not requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false, isMySiteTabsBuildConfigEnabled = false)

        viewModel.refresh(true)

        assertThat(trackWithTabSource).isEmpty()
    }

    @Test
    fun `given tabs are disabled, when pull to refresh invoked, then pull-to-refresh is tracked`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false, isMySiteTabsBuildConfigEnabled = false)

        viewModel.refresh(true)

        assertThat(analyticsTrackerWrapper.track(Stat.MY_SITE_PULL_TO_REFRESH))
    }

    @Test
    fun `given tabs are enabled, when quick link stats tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_ACTION_STATS_TAPPED)
    }

    @Test
    fun `given tabs are enabled, when quick link pages tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickActionsPagesClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_ACTION_PAGES_TAPPED)
    }

    @Test
    fun `given tabs are enabled, when quick link posts tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickActionsPostsClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_ACTION_POSTS_TAPPED)
    }

    @Test
    fun `given tabs are enabled, when quick link media tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickActionsMediaClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_ACTION_MEDIA_TAPPED)
    }

    @Test
    fun `given tabs are disabled, when quick link stats tapped, then track with tab source is not requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false, isMySiteTabsBuildConfigEnabled = false)

        requireNotNull(quickActionsStatsClickAction).invoke()

        assertThat(trackWithTabSource).isEmpty()
        assertThat(analyticsTrackerWrapper.track(Stat.QUICK_ACTION_STATS_TAPPED))
    }

    @Test
    fun `given tabs are disabled, when quick link pages tapped, then track with tab source is not requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false, isMySiteTabsBuildConfigEnabled = false)

        requireNotNull(quickActionsPagesClickAction).invoke()

        assertThat(trackWithTabSource).isEmpty()
        assertThat(analyticsTrackerWrapper.track(Stat.QUICK_ACTION_PAGES_TAPPED))
    }

    @Test
    fun `given tabs are disabled, when quick link posts tapped, then track with tab source is not requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false, isMySiteTabsBuildConfigEnabled = false)

        requireNotNull(quickActionsPostsClickAction).invoke()

        assertThat(trackWithTabSource).isEmpty()
        assertThat(analyticsTrackerWrapper.track(Stat.QUICK_ACTION_POSTS_TAPPED))
    }

    @Test
    fun `given tabs are disabled, when quick link media tapped, then track with tab source is not requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = false, isMySiteTabsBuildConfigEnabled = false)

        requireNotNull(quickActionsMediaClickAction).invoke()

        assertThat(trackWithTabSource).isEmpty()
        assertThat(analyticsTrackerWrapper.track(Stat.QUICK_ACTION_MEDIA_TAPPED))
    }

    @Test
    fun `given tabs are enabled, when quick link ribbon pages tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickLinkRibbonPagesClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_LINK_RIBBON_PAGES_TAPPED)
    }

    @Test
    fun `given tabs are enabled, when quick link ribbon posts tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickLinkRibbonPostsClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_LINK_RIBBON_POSTS_TAPPED)
    }

    @Test
    fun `given tabs are enabled, when quick link ribbon stats tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickLinkRibbonStatsClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_LINK_RIBBON_STATS_TAPPED)
    }

    @Test
    fun `given tabs are enabled, when quick link ribbon media tapped, then track with tab source is requested`() {
        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true, isMySiteTabsBuildConfigEnabled = true)

        requireNotNull(quickLinkRibbonMediaClickAction).invoke()

        assertThat(trackWithTabSource.last().stat).isEqualTo(Stat.QUICK_LINK_RIBBON_MEDIA_TAPPED)
    }

    @Test
    fun `given site is WPCOM, when quick link ribbon stats click, then stats screen is shown`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsWPCom(true)

        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true)

        requireNotNull(quickLinkRibbonStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `given site is Jetpack, when quick link ribbon stats click, then stats screen is shown`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true)

        requireNotNull(quickLinkRibbonStatsClickAction).invoke()

        verify(quickStartRepository).completeTask(QuickStartTask.CHECK_STATS)
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    @Test
    fun `given self-hosted site, when quick link ribbon stats click, then shows connect jetpack screen`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        site.setIsJetpackInstalled(false)
        site.setIsJetpackConnected(false)

        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true)

        requireNotNull(quickLinkRibbonStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.ConnectJetpackForStats(site))
    }

    @Test
    fun `given user is not logged in jetpack site, when quick link ribbon stats click, then login screen is shown`() {
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        site.setIsJetpackInstalled(true)
        site.setIsJetpackConnected(true)

        initSelectedSite(isMySiteDashboardTabsFeatureFlagEnabled = true)

        requireNotNull(quickLinkRibbonStatsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.StartWPComLoginForJetpackStats)
    }

    @Test
    fun `when quick link ribbon pages click, then pages screen is shown and completes REVIEW_PAGES task `() {
        initSelectedSite()

        requireNotNull(quickActionsPagesClickAction).invoke()

        verify(quickStartRepository).completeTask(QuickStartTask.REVIEW_PAGES)
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `when quick link ribbon pages click, then pages screen is shown + requests next step of EDIT_HOMEPAGE task`() {
        initSelectedSite()

        requireNotNull(quickActionsPagesClickAction).invoke()

        verify(quickStartRepository).requestNextStepOfTask(QuickStartTask.EDIT_HOMEPAGE)
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPages(site))
    }

    @Test
    fun `when quick link ribbon posts click, then posts screen is shown `() {
        initSelectedSite()

        requireNotNull(quickActionsPostsClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenPosts(site))
    }

    @Test
    fun `when quick link ribbon media click, then media screen is shown`() {
        initSelectedSite()

        requireNotNull(quickActionsMediaClickAction).invoke()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenMedia(site))
    }

    private fun findQuickActionsCard() = getLastItems().find { it is QuickActionsCard } as QuickActionsCard?

    private fun findQuickStartDynamicCard() = getLastItems().find { it is DynamicCard } as DynamicCard?

    private fun findDomainRegistrationCard() =
            getLastItems().find { it is DomainRegistrationCard } as DomainRegistrationCard?

    private fun SiteSelected.findSiteMenuTabUiState() =
            tabsUiState.tabUiStates.first { it.tabType == MySiteTabType.SITE_MENU }

    private fun getLastItems() = (uiModels.last().state as SiteSelected).cardAndItems

    private fun getDashboardTabLastItems() = (uiModels.last().state as SiteSelected).dashboardCardsAndItems

    private fun getSiteMenuTabLastItems() = (uiModels.last().state as SiteSelected).siteMenuCardsAndItems

    private fun getSiteInfoHeaderCard() = (uiModels.last().state as SiteSelected).siteInfoHeaderState.siteInfoHeader

    private suspend fun invokeSiteInfoCardAction(action: SiteInfoHeaderCardAction) {
        onSiteChange.value = site
        onSiteSelected.value = siteLocalId
        selectedSite.value = SelectedSite(site)
        while (uiModels.last().state is NoSites) {
            delay(100)
        }
        val siteInfoCard = getSiteInfoHeaderCard()
        when (action) {
            SiteInfoHeaderCardAction.TITLE_CLICK -> siteInfoCard.onTitleClick!!.click()
            SiteInfoHeaderCardAction.ICON_CLICK -> siteInfoCard.onIconClick.click()
            SiteInfoHeaderCardAction.URL_CLICK -> siteInfoCard.onUrlClick.click()
            SiteInfoHeaderCardAction.SWITCH_SITE_CLICK -> siteInfoCard.onSwitchSiteClick.click()
        }
    }

    private fun invokeItemClickAction(action: ListItemAction) {
        var clickAction: ((ListItemAction) -> Unit)? = null
        doAnswer {
            val params = (it.arguments.filterIsInstance<SiteItemsBuilderParams>()).first()
            clickAction = params.onClick
            listOf<MySiteCardAndItem>()
        }.whenever(siteItemsBuilder).build(any<SiteItemsBuilderParams>())

        initSelectedSite()

        assertThat(clickAction).isNotNull
        clickAction!!.invoke(action)
    }

    private fun initSelectedSite(
        isMySiteDashboardTabsFeatureFlagEnabled: Boolean = false,
        isMySiteTabsBuildConfigEnabled: Boolean = false,
        isQuickStartDynamicCardEnabled: Boolean = false,
        isQuickStartInProgress: Boolean = false,
        showStaleMessage: Boolean = false,
        initialScreen: String = SITE_MENU.label
    ) {
        setUpDynamicCardsBuilder(isQuickStartDynamicCardEnabled)
        whenever(
                siteItemsBuilder.build(InfoItemBuilderParams(isStaleMessagePresent = showStaleMessage))
        ).thenReturn(if (showStaleMessage) InfoItem(title = UiStringText("")) else null)
        quickStartUpdate.value = QuickStartUpdate(
                categories = if (isQuickStartInProgress) listOf(quickStartCategory) else emptyList()
        )
        whenever(mySiteDashboardTabsFeatureConfig.isEnabled()).thenReturn(isMySiteDashboardTabsFeatureFlagEnabled)
        whenever(buildConfigWrapper.isMySiteTabsEnabled).thenReturn(isMySiteTabsBuildConfigEnabled)
        whenever(appPrefsWrapper.getMySiteInitialScreen()).thenReturn(initialScreen)
        onSiteSelected.value = siteLocalId
        onSiteChange.value = site
        selectedSite.value = SelectedSite(site)
    }

    private enum class SiteInfoHeaderCardAction {
        TITLE_CLICK, ICON_CLICK, URL_CLICK, SWITCH_SITE_CLICK
    }

    private fun setUpCardsBuilder() {
        doAnswer {
            val quickActionsCard = initQuickActionsCard(it)
            val quickLinkRibbons = initQuickLinkRibbons(it)
            val domainRegistrationCard = initDomainRegistrationCard(it)
            val quickStartCard = initQuickStartCard(it)
            val dashboardCards = initDashboardCards(it)
            val listOfCards = arrayListOf<MySiteCardAndItem>(
                    quickActionsCard,
                    domainRegistrationCard,
                    quickStartCard
            )

            if (mySiteDashboardPhase2FeatureConfig.isEnabled())
                listOfCards.add(dashboardCards)
            if (mySiteDashboardTabsFeatureConfig.isEnabled())
                listOfCards.add(quickLinkRibbons)
            listOfCards
        }.whenever(cardsBuilder).build(
                quickActionsCardBuilderParams = any(),
                domainRegistrationCardBuilderParams = any(),
                quickStartCardBuilderParams = any(),
                dashboardCardsBuilderParams = any(),
                quickLinkRibbonsBuilderParams = any()
        )

        doAnswer {
            siteInfoHeader = initSiteInfoCard(it)
            siteInfoHeader
        }.whenever(siteInfoHeaderCardBuilder).buildSiteInfoCard(any())
    }

    private fun setUpDynamicCardsBuilder(isQuickStartDynamicCardEnabled: Boolean) {
        doAnswer {
            mutableListOf<DynamicCard>().apply {
                if (isQuickStartDynamicCardEnabled) add(initDynamicQuickStartCard(it))
            }.toList()
        }.whenever(dynamicCardsBuilder).build(
                quickStartCategories = any(),
                pinnedDynamicCard = anyOrNull(),
                visibleDynamicCards = any(),
                onDynamicCardMoreClick = any(),
                onQuickStartTaskCardClick = any()
        )
    }

    private fun setUpSiteItemBuilder() {
        doAnswer {
            val items = initSiteItem(it)
            listOf<MySiteCardAndItem>(items)
        }.whenever(siteItemsBuilder).build(any<SiteItemsBuilderParams>())
    }

    private fun initSiteInfoCard(mockInvocation: InvocationOnMock): SiteInfoHeaderCard {
        val params = (mockInvocation.arguments.filterIsInstance<SiteInfoCardBuilderParams>()).first()
        return SiteInfoHeaderCard(
                title = siteName,
                url = siteUrl,
                iconState = IconState.Visible(siteIcon),
                showTitleFocusPoint = false,
                showIconFocusPoint = false,
                onTitleClick = ListItemInteraction.create { params.titleClick.invoke() },
                onIconClick = ListItemInteraction.create { params.iconClick.invoke() },
                onUrlClick = ListItemInteraction.create { params.urlClick.invoke() },
                onSwitchSiteClick = ListItemInteraction.create { params.switchSiteClick.invoke() }
        )
    }

    private fun initQuickActionsCard(mockInvocation: InvocationOnMock): QuickActionsCard {
        val params = (mockInvocation.arguments.filterIsInstance<QuickActionsCardBuilderParams>()).first()
        quickActionsStatsClickAction = params.onQuickActionStatsClick
        quickActionsPagesClickAction = params.onQuickActionPagesClick
        quickActionsPostsClickAction = params.onQuickActionPostsClick
        quickActionsMediaClickAction = params.onQuickActionMediaClick
        return QuickActionsCard(
                title = UiStringText(""),
                onStatsClick = ListItemInteraction.create { params.onQuickActionStatsClick.invoke() },
                onPagesClick = ListItemInteraction.create { params.onQuickActionPagesClick.invoke() },
                onPostsClick = ListItemInteraction.create { params.onQuickActionPostsClick.invoke() },
                onMediaClick = ListItemInteraction.create { params.onQuickActionMediaClick.invoke() },
                showPages = site.isSelfHostedAdmin || site.hasCapabilityEditPages,
                showPagesFocusPoint = false,
                showStatsFocusPoint = false
        )
    }

    private fun initQuickLinkRibbons(mockInvocation: InvocationOnMock): QuickLinkRibbon {
        val params = (mockInvocation.arguments.filterIsInstance<QuickLinkRibbonBuilderParams>()).first()
        quickLinkRibbonPagesClickAction = params.onPagesClick
        quickLinkRibbonPostsClickAction = params.onPostsClick
        quickLinkRibbonMediaClickAction = params.onMediaClick
        quickLinkRibbonStatsClickAction = params.onStatsClick
        return QuickLinkRibbon(
            quickLinkRibbonItems = mock(),
            showStatsFocusPoint = false,
            showPagesFocusPoint = false
        )
    }

    private fun initDomainRegistrationCard(mockInvocation: InvocationOnMock) = DomainRegistrationCard(
            ListItemInteraction.create {
                (mockInvocation.arguments.filterIsInstance<DomainRegistrationCardBuilderParams>()).first()
                        .domainRegistrationClick.invoke()
            }
    )

    private fun initQuickStartCard(mockInvocation: InvocationOnMock): QuickStartCard {
        val params = (mockInvocation.arguments.filterIsInstance<QuickStartCardBuilderParams>()).first()
        removeMenuItemClickAction = params.onQuickStartBlockRemoveMenuItemClick
        quickStartTaskTypeItemClickAction = params.onQuickStartTaskTypeItemClick
        return QuickStartCard(
                title = UiStringText(""),
                onRemoveMenuItemClick = ListItemInteraction.create {
                    params.onQuickStartBlockRemoveMenuItemClick.invoke()
                },
                taskTypeItems = listOf(
                        QuickStartTaskTypeItem(
                                quickStartTaskType = mock(),
                                title = UiStringText(""),
                                titleEnabled = true,
                                subtitle = UiStringText(""),
                                strikeThroughTitle = false,
                                progressColor = 0,
                                progress = 0,
                                onClick = ListItemInteraction.create(
                                        mock(),
                                        (quickStartTaskTypeItemClickAction as ((QuickStartTaskType) -> Unit))
                                )
                        )
                )
        )
    }

    private fun initDynamicQuickStartCard(mockInvocation: InvocationOnMock): QuickStartDynamicCard {
        dynamicCardMoreClick = mockInvocation.getArgument(DYNAMIC_CARDS_BUILDER_MORE_CLICK_PARAM_POSITION)
        val dynamicCardType = DynamicCardType.CUSTOMIZE_QUICK_START
        return QuickStartDynamicCard(
                id = dynamicCardType,
                title = UiStringRes(0),
                taskCards = mock(),
                accentColor = 0,
                progress = 0,
                onMoreClick = ListItemInteraction.create(
                        DynamicCardMenuModel(dynamicCardType, true),
                        dynamicCardMoreClick as ((DynamicCardMenuModel) -> Unit)
                )
        )
    }

    private fun initDashboardCards(mockInvocation: InvocationOnMock): DashboardCards {
        val params = (mockInvocation.arguments.filterIsInstance<DashboardCardsBuilderParams>()).first()
        return DashboardCards(
                cards = mutableListOf<DashboardCard>().apply {
                    if (params.showErrorCard) {
                        add(initErrorCard(mockInvocation))
                    } else {
                        add(initPostCard(mockInvocation))
                        add(initTodaysStatsCard(mockInvocation))
                        add(initBloggingPromptCard(mockInvocation))
                    }
                }
        )
    }

    private fun initTodaysStatsCard(mockInvocation: InvocationOnMock): TodaysStatsCard {
        val params = (mockInvocation.arguments.filterIsInstance<DashboardCardsBuilderParams>()).first()
        onTodaysStatsCardGetMoreViewsClick = params.todaysStatsCardBuilderParams.onGetMoreViewsClick
        onTodaysStatsCardClick = params.todaysStatsCardBuilderParams.onTodaysStatsCardClick
        onTodaysStatsCardFooterLinkClick = params.todaysStatsCardBuilderParams.onFooterLinkClick
        return TodaysStatsCardWithData(
                views = UiStringText(mock()),
                visitors = UiStringText(mock()),
                likes = UiStringText(mock()),
                onCardClick = onTodaysStatsCardClick,
                footerLink = TodaysStatsCard.FooterLink(
                        label = UiStringRes(R.string.my_site_todays_stats_card_footer_link_go_to_stats),
                        onClick = onTodaysStatsCardFooterLinkClick
                )
        )
    }

    private fun initErrorCard(mockInvocation: InvocationOnMock): ErrorCard {
        val params = (mockInvocation.arguments.filterIsInstance<DashboardCardsBuilderParams>()).first()
        onDashboardErrorRetryClick = params.onErrorRetryClick
        return ErrorCard(onRetryClick = ListItemInteraction.create { onDashboardErrorRetryClick })
    }

    private fun initPostCard(mockInvocation: InvocationOnMock): PostCardWithPostItems {
        val params = (mockInvocation.arguments.filterIsInstance<DashboardCardsBuilderParams>()).first()
        onPostItemClick = params.postCardBuilderParams.onPostItemClick
        onPostCardFooterLinkClick = params.postCardBuilderParams.onFooterLinkClick
        return PostCardWithPostItems(
                postCardType = PostCardType.DRAFT,
                title = UiStringRes(0),
                postItems = listOf(
                        PostItem(
                                title = UiStringRes(0),
                                excerpt = UiStringRes(0),
                                featuredImageUrl = "",
                                onClick = ListItemInteraction.create {
                                    (onPostItemClick as (PostItemClickParams) -> Unit).invoke(
                                            PostItemClickParams(PostCardType.DRAFT, postId)
                                    )
                                }
                        )
                ),
                footerLink = FooterLink(
                        label = UiStringRes(0),
                        onClick = onPostCardFooterLinkClick as ((postCardType: PostCardType) -> Unit)
                )
        )
    }

    private fun initBloggingPromptCard(mockInvocation: InvocationOnMock): BloggingPromptCardWithData {
        val params = (mockInvocation.arguments.filterIsInstance<DashboardCardsBuilderParams>()).first()
        onBloggingPromptShareClicked = params.bloggingPromptCardBuilderParams.onShareClick
        return BloggingPromptCardWithData(
                prompt = UiStringText("Test prompt"),
                respondents = emptyList(),
                numberOfAnswers = 5,
                isAnswered = false,
                onShareClick = onBloggingPromptShareClicked as ((message: String) -> Unit)
        )
    }

    private fun initSiteItem(mockInvocation: InvocationOnMock): ListItem {
        val params = (mockInvocation.arguments.filterIsInstance<SiteItemsBuilderParams>()).first()
        return ListItem(
                0,
                UiStringRes(0),
                onClick = ListItemInteraction.create(ListItemAction.POSTS, params.onClick)
        )
    }

    fun ViewModel.invokeOnCleared() {
        val viewModelStore = ViewModelStore()
        val viewModelProvider = ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = this@invokeOnCleared as T
        })
        viewModelProvider.get(this@invokeOnCleared::class.java)
        viewModelStore.clear()
    }
}
