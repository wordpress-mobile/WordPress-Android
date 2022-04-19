package org.wordpress.android.ui.mysite

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPLOAD_SITE_ICON
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.models.bloggingprompts.BloggingPrompt
import org.wordpress.android.models.bloggingprompts.BloggingPromptRespondent
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Type
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams.PostItemClickParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteItemsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.MySiteViewModel.TabsUiState.TabUiState
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
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource
import org.wordpress.android.ui.photopicker.PhotoPickerActivity.PhotoPickerMediaSource.ANDROID_CAMERA
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.WPMediaUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.util.config.LandOnTheEditorFeatureConfig
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.util.filter
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.util.map
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@Suppress("LargeClass", "LongMethod", "LongParameterList", "TooManyFunctions")
class MySiteViewModel @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val wpMediaUtilsWrapper: WPMediaUtilsWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val siteIconUploadHandler: SiteIconUploadHandler,
    private val siteStoriesHandler: SiteStoriesHandler,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val quickStartCardBuilder: QuickStartCardBuilder,
    private val siteInfoHeaderCardBuilder: SiteInfoHeaderCardBuilder,
    private val homePageDataLoader: HomePageDataLoader,
    private val quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val snackbarSequencer: SnackbarSequencer,
    private val cardsBuilder: CardsBuilder,
    private val dynamicCardsBuilder: DynamicCardsBuilder,
    private val landOnTheEditorFeatureConfig: LandOnTheEditorFeatureConfig,
    private val mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig,
    private val mySiteSourceManager: MySiteSourceManager,
    private val cardsTracker: CardsTracker,
    private val siteItemsTracker: SiteItemsTracker,
    private val domainRegistrationCardShownTracker: DomainRegistrationCardShownTracker,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val mySiteDashboardTabsFeatureConfig: MySiteDashboardTabsFeatureConfig,
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig,
    private val appPrefsWrapper: AppPrefsWrapper
) : ScopedViewModel(mainDispatcher) {
    private var isDefaultABExperimentTabSet: Boolean = false
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onTechInputDialogShown = MutableLiveData<Event<TextInputDialogModel>>()
    private val _onBasicDialogShown = MutableLiveData<Event<SiteDialogModel>>()
    private val _onDynamicCardMenuShown = MutableLiveData<Event<DynamicCardMenuModel>>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    private val _onMediaUpload = MutableLiveData<Event<MediaModel>>()
    private val _activeTaskPosition = MutableLiveData<Pair<QuickStartTask, Int>>()
    private val _onShowSwipeRefreshLayout = MutableLiveData<Event<Boolean>>()
    private val _onShare = MutableLiveData<Event<String>>()
    private val _onTrackWithTabSource = MutableLiveData<Event<MySiteTrackWithTabSource>>()
    private val _selectTab = MutableLiveData<Event<TabNavigation>>()

    private val tabsUiState: LiveData<TabsUiState> = quickStartRepository.onQuickStartSiteMenuStep
            .switchMap { quickStartSiteMenuStep ->
                val result = MutableLiveData<TabsUiState>()
                /* We want to filter out tabs state livedata update when state is not set in uiModel.
                   Without this check, tabs state livedata merge with state livedata may return a null state
                   when building UiModel. */
                uiModel.value?.state?.tabsUiState?.let {
                    result.value = it.copy(tabUiStates = it.update(quickStartSiteMenuStep))
                }
                result
            }

    /* Capture and track the site selected event so we can circumvent refreshing sources on resume
       as they're already built on site select. */
    private var isSiteSelected = false

    val isMySiteTabsEnabled: Boolean
        get() = mySiteDashboardTabsFeatureConfig.isEnabled() &&
                buildConfigWrapper.isMySiteTabsEnabled &&
                selectedSiteRepository.getSelectedSite()?.isUsingWpComRestApi ?: true

    val orderedTabTypes: List<MySiteTabType>
        get() = if (isMySiteTabsEnabled) {
            listOf(MySiteTabType.DASHBOARD, MySiteTabType.SITE_MENU)
        } else {
            listOf(MySiteTabType.ALL)
        }

    private val defaultABExperimentTab: MySiteTabType
        get() = if (isMySiteTabsEnabled) {
            if (appPrefsWrapper.getMySiteInitialScreen() == MySiteTabType.SITE_MENU.label) {
                MySiteTabType.SITE_MENU
            } else {
                MySiteTabType.DASHBOARD
            }
        } else {
            MySiteTabType.ALL
        }

    val onScrollTo: LiveData<Event<Int>> = merge(
            _activeTaskPosition.distinctUntilChanged(),
            quickStartRepository.activeTask
    ) { pair, activeTask ->
        if (pair != null && activeTask != null && pair.first == activeTask) {
            Event(pair.second)
        } else {
            null
        }
    }
    val onSnackbarMessage = merge(_onSnackbarMessage, siteStoriesHandler.onSnackbar, quickStartRepository.onSnackbar)
    val onQuickStartMySitePrompts = quickStartRepository.onQuickStartMySitePrompts
    val onTextInputDialogShown = _onTechInputDialogShown as LiveData<Event<TextInputDialogModel>>
    val onBasicDialogShown = _onBasicDialogShown as LiveData<Event<SiteDialogModel>>
    val onDynamicCardMenuShown = _onDynamicCardMenuShown as LiveData<Event<DynamicCardMenuModel>>
    val onNavigation = merge(_onNavigation, siteStoriesHandler.onNavigation)
    val onMediaUpload = _onMediaUpload as LiveData<Event<MediaModel>>
    val onUploadedItem = siteIconUploadHandler.onUploadedItem
    val onShowSwipeRefreshLayout = _onShowSwipeRefreshLayout
    val onShare = _onShare
    val onTrackWithTabSource = _onTrackWithTabSource as LiveData<Event<MySiteTrackWithTabSource>>
    val selectTab: LiveData<Event<TabNavigation>> = _selectTab
    private var shouldMarkUpdateSiteTitleTaskComplete = false

    val state: LiveData<MySiteUiState> =
            selectedSiteRepository.siteSelected.switchMap { siteLocalId ->
                isSiteSelected = true
                resetShownTrackers()
                val result = MediatorLiveData<SiteIdToState>()
                for (newSource in mySiteSourceManager.build(viewModelScope, siteLocalId)) {
                    result.addSource(newSource) { partialState ->
                        if (partialState != null) {
                            result.value = (result.value ?: SiteIdToState(siteLocalId)).update(partialState)
                        }
                    }
                }
                // We want to filter out the empty state where we have a site ID but site object is missing.
                // Without this check there is an emission of a NoSites state even if we have the site
                result.filter { it.siteId == null || it.state.site != null }.map { it.state }
            }.addDistinctUntilChangedIfNeeded(!mySiteDashboardPhase2FeatureConfig.isEnabled())

    val uiModel: LiveData<UiModel> = merge(tabsUiState, state) { tabsUiState, mySiteUiState ->
        with(requireNotNull(mySiteUiState)) {
            val state = if (site != null) {
                cardsUpdate?.checkAndShowSnackbarError()
                val state = buildSiteSelectedStateAndScroll(
                        tabsUiState,
                        site,
                        showSiteIconProgressBar,
                        activeTask,
                        isDomainCreditAvailable,
                        quickStartCategories,
                        pinnedDynamicCard,
                        visibleDynamicCards,
                        backupAvailable,
                        scanAvailable,
                        cardsUpdate
                )
                selectDefaultTabIfNeeded()
                trackCardsAndItemsShownIfNeeded(state)
                state
            } else {
                buildNoSiteState()
            }
            UiModel(currentAvatarUrl.orEmpty(), state)
        }
    }

    private fun CardsUpdate.checkAndShowSnackbarError() {
        if (showSnackbarError) {
            _onSnackbarMessage
                    .postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.my_site_dashboard_update_error))))
        }
    }

    @Suppress("LongParameterList")
    private fun buildSiteSelectedStateAndScroll(
        tabsUiState: TabsUiState?,
        site: SiteModel,
        showSiteIconProgressBar: Boolean,
        activeTask: QuickStartTask?,
        isDomainCreditAvailable: Boolean,
        quickStartCategories: List<QuickStartCategory>,
        pinnedDynamicCard: DynamicCardType?,
        visibleDynamicCards: List<DynamicCardType>,
        backupAvailable: Boolean,
        scanAvailable: Boolean,
        cardsUpdate: CardsUpdate?
    ): SiteSelected {
        val siteItems = buildSiteSelectedState(
                site,
                activeTask,
                isDomainCreditAvailable,
                quickStartCategories,
                pinnedDynamicCard,
                visibleDynamicCards,
                backupAvailable,
                scanAvailable,
                cardsUpdate
        )

        val siteInfoCardBuilderParams = SiteInfoCardBuilderParams(
                site = site,
                showSiteIconProgressBar = showSiteIconProgressBar,
                titleClick = this::titleClick,
                iconClick = this::iconClick,
                urlClick = this::urlClick,
                switchSiteClick = this::switchSiteClick,
                activeTask = activeTask
        )

        val siteInfo = siteInfoHeaderCardBuilder.buildSiteInfoCard(siteInfoCardBuilderParams)

        if (activeTask != null) {
            scrollToQuickStartTaskIfNecessary(
                    activeTask,
                    getPositionOfQuickStartItem(siteItems, activeTask)
            )
        }
        // It is okay to use !! here because we are explicitly creating the lists
        return SiteSelected(
                tabsUiState = tabsUiState?.copy(
                        showTabs = isMySiteTabsEnabled,
                        tabUiStates = orderedTabTypes.mapToTabUiStates(),
                        shouldUpdateViewPager = shouldUpdateViewPager()
                ) ?: createTabsUiState(),
                siteInfoToolbarViewParams = getSiteInfoToolbarViewParams(),
                siteInfoHeaderState = SiteInfoHeaderState(
                        hasUpdates = hasSiteHeaderUpdates(siteInfo),
                        siteInfoHeader = siteInfo
                ),
                cardAndItems = siteItems[MySiteTabType.ALL]!!,
                siteMenuCardsAndItems = siteItems[MySiteTabType.SITE_MENU]!!,
                dashboardCardsAndItems = siteItems[MySiteTabType.DASHBOARD]!!
        )
    }

    private fun getSiteInfoToolbarViewParams(): SiteInfoToolbarViewParams {
        return if (isMySiteTabsEnabled) {
            SiteInfoToolbarViewParams(
                    R.dimen.app_bar_with_site_info_tabs_height,
                    R.dimen.toolbar_bottom_margin_with_tabs
            )
        } else {
            SiteInfoToolbarViewParams(
                    R.dimen.app_bar_with_site_info_height,
                    R.dimen.toolbar_bottom_margin_with_no_tabs
            )
        }
    }

    private fun getPositionOfQuickStartItem(
        siteItems: Map<MySiteTabType, List<MySiteCardAndItem>>,
        activeTask: QuickStartTask
    ) = if (isMySiteTabsEnabled) {
        _selectTab.value?.let { tabEvent ->
            val currentTab = orderedTabTypes[tabEvent.peekContent().position]
            if (currentTab == MySiteTabType.DASHBOARD && activeTask.showInSiteMenu()) {
                (siteItems[MySiteTabType.SITE_MENU] as List<MySiteCardAndItem>)
                        .indexOfFirst { it.activeQuickStartItem }
            } else {
                (siteItems[currentTab] as List<MySiteCardAndItem>)
                        .indexOfFirst { it.activeQuickStartItem }
            }
        } ?: LIST_INDEX_NO_ACTIVE_QUICK_START_ITEM
    } else {
        (siteItems[MySiteTabType.ALL] as List<MySiteCardAndItem>)
                .indexOfFirst { it.activeQuickStartItem }
    }

    private fun QuickStartTask.showInSiteMenu() = when (this) {
        QuickStartTask.VIEW_SITE,
        QuickStartTask.ENABLE_POST_SHARING,
        QuickStartTask.EXPLORE_PLANS -> true
        else -> false
    }

    @Suppress("LongParameterList")
    private fun buildSiteSelectedState(
        site: SiteModel,
        activeTask: QuickStartTask?,
        isDomainCreditAvailable: Boolean,
        quickStartCategories: List<QuickStartCategory>,
        pinnedDynamicCard: DynamicCardType?,
        visibleDynamicCards: List<DynamicCardType>,
        backupAvailable: Boolean,
        scanAvailable: Boolean,
        cardsUpdate: CardsUpdate?
    ): Map<MySiteTabType, List<MySiteCardAndItem>> {
        val infoItem = siteItemsBuilder.build(
                InfoItemBuilderParams(
                        isStaleMessagePresent = cardsUpdate?.showStaleMessage ?: false
                )
        )
        val cardsResult = cardsBuilder.build(
                QuickActionsCardBuilderParams(
                        siteModel = site,
                        onQuickActionStatsClick = this::quickActionStatsClick,
                        onQuickActionPagesClick = this::quickActionPagesClick,
                        onQuickActionPostsClick = this::quickActionPostsClick,
                        onQuickActionMediaClick = this::quickActionMediaClick
                ),
                DomainRegistrationCardBuilderParams(
                        isDomainCreditAvailable = isDomainCreditAvailable,
                        domainRegistrationClick = this::domainRegistrationClick
                ),
                QuickStartCardBuilderParams(
                        quickStartCategories = quickStartCategories,
                        onQuickStartBlockRemoveMenuItemClick = this::onQuickStartBlockRemoveMenuItemClick,
                        onQuickStartTaskTypeItemClick = this::onQuickStartTaskTypeItemClick
                ),
                DashboardCardsBuilderParams(
                        showErrorCard = cardsUpdate?.showErrorCard == true,
                        onErrorRetryClick = this::onDashboardErrorRetry,
                        todaysStatsCardBuilderParams = TodaysStatsCardBuilderParams(
                                todaysStatsCard = cardsUpdate?.cards?.firstOrNull { it is TodaysStatsCardModel }
                                        as? TodaysStatsCardModel,
                                onTodaysStatsCardClick = this::onTodaysStatsCardClick,
                                onGetMoreViewsClick = this::onGetMoreViewsClick,
                                onFooterLinkClick = this::onTodaysStatsCardFooterLinkClick
                        ),
                        postCardBuilderParams = PostCardBuilderParams(
                                posts = cardsUpdate?.cards?.firstOrNull { it is PostsCardModel } as? PostsCardModel,
                                onPostItemClick = this::onPostItemClick,
                                onFooterLinkClick = this::onPostCardFooterLinkClick
                        ),
                        bloggingPromptCardBuilderParams = BloggingPromptCardBuilderParams(
                                // TODO @klymyam fetch the actual blogging prompt
                                bloggingPrompt = if (bloggingPromptsFeatureConfig.isEnabled()) {
                                    @Suppress("MagicNumber")
                                    val dummyRespondent = BloggingPromptRespondent(
                                            54279365,
                                            "https://0.gravatar.com/avatar/cec64efa352617" +
                                                    "c35743d8ed233ab410?s=96&d=identicon&r=G"
                                    )
                                    BloggingPrompt(
                                            "Cast the movie of your life",
                                            "",
                                            listOf(
                                                    dummyRespondent,
                                                    dummyRespondent,
                                                    dummyRespondent,
                                                    dummyRespondent,
                                                    dummyRespondent
                                            )
                                    )
                                } else null,
                                onShareClick = this::onBloggingPromptShareClick
                        )
                ),
                QuickLinkRibbonBuilderParams(
                        siteModel = site,
                        onPagesClick = this::onQuickLinkRibbonPagesClick,
                        onPostsClick = this::onQuickLinkRibbonPostsClick,
                        onMediaClick = this::onQuickLinkRibbonMediaClick,
                        onStatsClick = this::onQuickLinkRibbonStatsClick
                ),
                isMySiteTabsEnabled
        )
        val dynamicCards = dynamicCardsBuilder.build(
                quickStartCategories,
                pinnedDynamicCard,
                visibleDynamicCards,
                this::onDynamicCardMoreClick,
                this::onQuickStartTaskCardClick
        )

        val siteItems = siteItemsBuilder.build(
                SiteItemsBuilderParams(
                        site = site,
                        activeTask = activeTask,
                        backupAvailable = backupAvailable,
                        scanAvailable = scanAvailable,
                        onClick = this::onItemClick
                )
        )

        return mapOf(
                MySiteTabType.ALL to orderForDisplay(
                        infoItem,
                        cardsResult,
                        dynamicCards,
                        siteItems
                ),
                MySiteTabType.SITE_MENU to orderForDisplay(
                        infoItem,
                        cardsResult.filterNot {
                            getCardTypeExclusionFiltersForTab(MySiteTabType.SITE_MENU).contains(it.type)
                        },
                        if (shouldIncludeDynamicCards(MySiteTabType.SITE_MENU)) { dynamicCards } else { listOf() },
                        siteItems
                ),
                MySiteTabType.DASHBOARD to orderForDisplay(
                        infoItem,
                        cardsResult.filterNot {
                            getCardTypeExclusionFiltersForTab(MySiteTabType.DASHBOARD).contains(it.type)
                        },
                        if (shouldIncludeDynamicCards(MySiteTabType.DASHBOARD)) { dynamicCards } else { listOf() },
                        listOf()
                )
        )
    }

    private fun getCardTypeExclusionFiltersForTab(tabType: MySiteTabType) = when (tabType) {
        MySiteTabType.SITE_MENU -> mutableListOf<Type>().apply {
            add(Type.DASHBOARD_CARDS)
            if (defaultABExperimentTab == MySiteTabType.DASHBOARD) {
                add(Type.QUICK_START_CARD)
            }
            add(Type.QUICK_LINK_RIBBON)
        }
        MySiteTabType.DASHBOARD -> mutableListOf<Type>().apply {
            if (defaultABExperimentTab == MySiteTabType.SITE_MENU) {
                add(Type.QUICK_START_CARD)
            }
            add(Type.DOMAIN_REGISTRATION_CARD)
            add(Type.QUICK_ACTIONS_CARD)
        }
        MySiteTabType.ALL -> emptyList()
    }

    private fun shouldIncludeDynamicCards(tabType: MySiteTabType) = when (tabType) {
        MySiteTabType.SITE_MENU -> defaultABExperimentTab != MySiteTabType.DASHBOARD
        MySiteTabType.DASHBOARD -> defaultABExperimentTab != MySiteTabType.SITE_MENU
        MySiteTabType.ALL -> true
    }

    @Suppress("EmptyFunctionBlock")
    private fun onGetMoreViewsClick() {
        cardsTracker.trackTodaysStatsCardGetMoreViewsNudgeClicked()
        _onNavigation.value = Event(
                SiteNavigationAction.OpenTodaysStatsGetMoreViewsExternalUrl(URL_GET_MORE_VIEWS_AND_TRAFFIC)
        )
    }

    private fun onTodaysStatsCardFooterLinkClick() {
        cardsTracker.trackTodaysStatsCardFooterLinkClicked()
        navigateToTodaysStats()
    }

    private fun onTodaysStatsCardClick() {
        cardsTracker.trackTodaysStatsCardClicked()
        navigateToTodaysStats()
    }

    private fun navigateToTodaysStats() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(SiteNavigationAction.OpenStatsInsights(selectedSite))
    }

    private fun buildNoSiteState(): NoSites {
        // Hide actionable empty view image when screen height is under specified min height.
        val shouldShowImage = !buildConfigWrapper.isJetpackApp &&
                displayUtilsWrapper.getDisplayPixelHeight() >= MIN_DISPLAY_PX_HEIGHT_NO_SITE_IMAGE
        return NoSites(
                tabsUiState = TabsUiState(showTabs = false, tabUiStates = emptyList()),
                siteInfoToolbarViewParams = SiteInfoToolbarViewParams(
                        appBarHeight = R.dimen.app_bar_with_no_site_info_height,
                        toolbarBottomMargin = R.dimen.toolbar_bottom_margin_with_no_tabs,
                        headerVisible = false,
                        appBarLiftOnScroll = true

                ),
                shouldShowImage = shouldShowImage
        )
    }

    private fun orderForDisplay(
        infoItem: InfoItem?,
        cards: List<MySiteCardAndItem>,
        dynamicCards: List<MySiteCardAndItem>,
        siteItems: List<MySiteCardAndItem>
    ): List<MySiteCardAndItem> {
        val indexOfDashboardCards = cards.indexOfFirst { it is DashboardCards }
        return mutableListOf<MySiteCardAndItem>().apply {
            infoItem?.let { add(infoItem) }
            addAll(cards)
            if (indexOfDashboardCards == -1) {
                addAll(dynamicCards)
            } else {
                addAll(indexOfDashboardCards, dynamicCards)
            }
            addAll(siteItems)
        }.toList()
    }

    private fun scrollToQuickStartTaskIfNecessary(
        quickStartTask: QuickStartTask,
        position: Int
    ) {
        if (_activeTaskPosition.value?.first != quickStartTask && isSiteHeaderQuickStartTask(
                        quickStartTask,
                        position
                )) {
            _activeTaskPosition.postValue(quickStartTask to position)
        }
    }

    private fun isSiteHeaderQuickStartTask(quickStartTask: QuickStartTask, position: Int): Boolean {
        return if (position == LIST_INDEX_NO_ACTIVE_QUICK_START_ITEM &&
                (quickStartTask == UPDATE_SITE_TITLE || quickStartTask == UPLOAD_SITE_ICON)) true
        else position >= 0
    }

    fun onTabChanged(position: Int) {
        quickStartRepository.quickStartTaskOrigin = orderedTabTypes[position]
        if (position == orderedTabTypes.indexOf(MySiteTabType.SITE_MENU)) {
            findUiStateForTab(MySiteTabType.SITE_MENU)?.pendingTask?.let { requestSiteMenuStepPendingTask(it) }
        }
        trackTabChanged(position == orderedTabTypes.indexOf(MySiteTabType.SITE_MENU))
    }

    private fun requestSiteMenuStepPendingTask(pendingTask: QuickStartTask) {
        quickStartRepository.clearSiteMenuStep()
        launch {
            delay(LIST_SCROLL_DELAY_MS)
            quickStartRepository.setActiveTask(pendingTask)
        }
    }

    @Suppress("ComplexMethod")
    private fun onItemClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            siteItemsTracker.trackSiteItemClicked(action)
            val navigationAction = when (action) {
                ListItemAction.ACTIVITY_LOG -> SiteNavigationAction.OpenActivityLog(selectedSite)
                ListItemAction.BACKUP -> SiteNavigationAction.OpenBackup(selectedSite)
                ListItemAction.SCAN -> SiteNavigationAction.OpenScan(selectedSite)
                ListItemAction.PLAN -> {
                    quickStartRepository.completeTask(QuickStartTask.EXPLORE_PLANS)
                    SiteNavigationAction.OpenPlan(selectedSite)
                }
                ListItemAction.POSTS -> SiteNavigationAction.OpenPosts(selectedSite)
                ListItemAction.PAGES -> {
                    quickStartRepository.requestNextStepOfTask(QuickStartTask.EDIT_HOMEPAGE)
                    quickStartRepository.completeTask(QuickStartTask.REVIEW_PAGES)
                    SiteNavigationAction.OpenPages(selectedSite)
                }
                ListItemAction.ADMIN -> SiteNavigationAction.OpenAdmin(selectedSite)
                ListItemAction.PEOPLE -> SiteNavigationAction.OpenPeople(selectedSite)
                ListItemAction.SHARING -> {
                    quickStartRepository.requestNextStepOfTask(QuickStartTask.ENABLE_POST_SHARING)
                    SiteNavigationAction.OpenSharing(selectedSite)
                }
                ListItemAction.DOMAINS -> SiteNavigationAction.OpenDomains(selectedSite)
                ListItemAction.SITE_SETTINGS -> SiteNavigationAction.OpenSiteSettings(selectedSite)
                ListItemAction.THEMES -> SiteNavigationAction.OpenThemes(selectedSite)
                ListItemAction.PLUGINS -> SiteNavigationAction.OpenPlugins(selectedSite)
                ListItemAction.STATS -> {
                    quickStartRepository.completeTask(QuickStartTask.CHECK_STATS)
                    getStatsNavigationActionForSite(selectedSite)
                }
                ListItemAction.MEDIA -> SiteNavigationAction.OpenMedia(selectedSite)
                ListItemAction.COMMENTS -> SiteNavigationAction.OpenUnifiedComments(selectedSite)
                ListItemAction.VIEW_SITE -> {
                    quickStartRepository.completeTask(QuickStartTask.VIEW_SITE)
                    SiteNavigationAction.OpenSite(selectedSite)
                }
                ListItemAction.JETPACK_SETTINGS -> SiteNavigationAction.OpenJetpackSettings(selectedSite)
            }
            _onNavigation.postValue(Event(navigationAction))
        } ?: _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.site_cannot_be_loaded))))
    }

    private fun onDynamicCardMoreClick(model: DynamicCardMenuModel) {
        _onDynamicCardMenuShown.postValue(Event(model))
    }

    private fun onQuickStartBlockRemoveMenuItemClick() {
        _onBasicDialogShown.value = Event(ShowRemoveNextStepsDialog)
    }

    private fun onQuickStartTaskTypeItemClick(type: QuickStartTaskType) {
        clearActiveQuickStartTask()
        _onNavigation.value = Event(
                SiteNavigationAction.OpenQuickStartFullScreenDialog(type, quickStartCardBuilder.getTitle(type))
        )
    }

    fun onQuickStartTaskCardClick(task: QuickStartTask) {
        quickStartRepository.setActiveTask(task)
    }

    fun onQuickStartFullScreenDialogDismiss() {
        mySiteSourceManager.refreshQuickStart()
    }

    private fun titleClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(R.string.error_network_connection)))
        } else if (!SiteUtils.isAccessedViaWPComRest(selectedSite) || !selectedSite.hasCapabilityManageOptions) {
            _onSnackbarMessage.value = Event(
                    SnackbarMessageHolder(UiStringRes(R.string.my_site_title_changer_dialog_not_allowed_hint))
            )
        } else {
            _onTechInputDialogShown.value = Event(
                    TextInputDialogModel(
                            callbackId = SITE_NAME_CHANGE_CALLBACK_ID,
                            title = R.string.my_site_title_changer_dialog_title,
                            initialText = selectedSite.name,
                            hint = R.string.my_site_title_changer_dialog_hint,
                            isMultiline = false,
                            isInputEnabled = true
                    )
            )
        }
    }

    private fun iconClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.MY_SITE_ICON_TAPPED)
        val hasIcon = selectedSite.iconUrl != null
        if (selectedSite.hasCapabilityManageOptions && selectedSite.hasCapabilityUploadFiles) {
            if (hasIcon) {
                _onBasicDialogShown.value = Event(ChangeSiteIconDialogModel)
            } else {
                _onBasicDialogShown.value = Event(AddSiteIconDialogModel)
            }
        } else {
            val message = when {
                !selectedSite.isUsingWpComRestApi -> {
                    R.string.my_site_icon_dialog_change_requires_jetpack_message
                }
                hasIcon -> {
                    R.string.my_site_icon_dialog_change_requires_permission_message
                }
                else -> {
                    R.string.my_site_icon_dialog_add_requires_permission_message
                }
            }
            _onSnackbarMessage.value = Event(SnackbarMessageHolder(UiStringRes(message)))
        }
    }

    private fun urlClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(SiteNavigationAction.OpenSite(selectedSite))
    }

    private fun switchSiteClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        _onNavigation.value = Event(SiteNavigationAction.OpenSitePicker(selectedSite))
    }

    private fun quickActionStatsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_ACTION_STATS_TAPPED)
        quickStartRepository.completeTask(QuickStartTask.CHECK_STATS)
        _onNavigation.value = Event(getStatsNavigationActionForSite(selectedSite))
    }

    private fun quickActionPagesClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_ACTION_PAGES_TAPPED)
        quickStartRepository.requestNextStepOfTask(QuickStartTask.EDIT_HOMEPAGE)
        quickStartRepository.completeTask(QuickStartTask.REVIEW_PAGES)
        _onNavigation.value = Event(SiteNavigationAction.OpenPages(selectedSite))
    }

    private fun quickActionPostsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_ACTION_POSTS_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenPosts(selectedSite))
    }

    private fun quickActionMediaClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_ACTION_MEDIA_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenMedia(selectedSite))
    }

    private fun onQuickLinkRibbonStatsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_LINK_RIBBON_STATS_TAPPED)
        quickStartRepository.completeTask(QuickStartTask.CHECK_STATS)
        _onNavigation.value = Event(getStatsNavigationActionForSite(selectedSite))
    }

    private fun onQuickLinkRibbonPagesClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_LINK_RIBBON_PAGES_TAPPED)
        quickStartRepository.requestNextStepOfTask(QuickStartTask.EDIT_HOMEPAGE)
        quickStartRepository.completeTask(QuickStartTask.REVIEW_PAGES)
        _onNavigation.value = Event(SiteNavigationAction.OpenPages(selectedSite))
    }

    private fun onQuickLinkRibbonPostsClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_LINK_RIBBON_POSTS_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenPosts(selectedSite))
    }

    private fun onQuickLinkRibbonMediaClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        trackWithTabSourceIfNeeded(Stat.QUICK_LINK_RIBBON_MEDIA_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenMedia(selectedSite))
    }

    private fun domainRegistrationClick() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED, selectedSite)
        _onNavigation.value = Event(SiteNavigationAction.OpenDomainRegistration(selectedSite))
    }

    fun refresh(isPullToRefresh: Boolean = false) {
        if (isPullToRefresh) trackWithTabSourceIfNeeded(Stat.MY_SITE_PULL_TO_REFRESH)
        mySiteSourceManager.refresh()
    }

    fun onResume() {
        mySiteSourceManager.onResume(isSiteSelected)
        isSiteSelected = false
        checkAndShowQuickStartNotice()
        _onShowSwipeRefreshLayout.postValue(Event(mySiteDashboardPhase2FeatureConfig.isEnabled()))
    }

    fun clearActiveQuickStartTask() {
        quickStartRepository.clearActiveTask()
    }

    fun checkAndShowQuickStartNotice() {
        quickStartRepository.checkAndShowQuickStartNotice()
    }

    fun dismissQuickStartNotice() {
        if (quickStartRepository.isQuickStartNoticeShown) snackbarSequencer.dismissLastSnackbar()
    }

    fun onSiteNameChosen(input: String) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onSnackbarMessage.postValue(
                    Event(SnackbarMessageHolder(UiStringRes(R.string.error_update_site_title_network)))
            )
        } else {
            selectedSiteRepository.updateTitle(input)
        }
    }

    fun onSiteNameChooserDismissed() {
        // This callback is called even when the dialog interaction is positive,
        // otherwise we would need to call 'completeTask' on 'onSiteNameChosen' as well.
        quickStartRepository.completeTask(QuickStartTask.UPDATE_SITE_TITLE)
        quickStartRepository.checkAndShowQuickStartNotice()
    }

    fun onDialogInteraction(interaction: DialogInteraction) {
        when (interaction) {
            is Positive -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    _onNavigation.postValue(
                            Event(
                                    SiteNavigationAction.OpenMediaPicker(
                                            requireNotNull(selectedSiteRepository.getSelectedSite())
                                    )
                            )
                    )
                }
                TAG_REMOVE_NEXT_STEPS_DIALOG -> onRemoveNextStepsDialogPositiveButtonClicked()
            }
            is Negative -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
                TAG_CHANGE_SITE_ICON_DIALOG -> {
                    analyticsTrackerWrapper.track(Stat.MY_SITE_ICON_REMOVED)
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                    selectedSiteRepository.updateSiteIconMediaId(0, true)
                }
                TAG_REMOVE_NEXT_STEPS_DIALOG -> onRemoveNextStepsDialogNegativeButtonClicked()
            }
            is Dismissed -> when (interaction.tag) {
                TAG_ADD_SITE_ICON_DIALOG, TAG_CHANGE_SITE_ICON_DIALOG -> {
                    quickStartRepository.completeTask(QuickStartTask.UPLOAD_SITE_ICON)
                    quickStartRepository.checkAndShowQuickStartNotice()
                }
            }
        }
    }

    private fun onRemoveNextStepsDialogPositiveButtonClicked() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED)
        quickStartRepository.skipQuickStart()
        refresh()
        clearActiveQuickStartTask()
    }

    private fun onRemoveNextStepsDialogNegativeButtonClicked() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED)
    }

    fun handleTakenSiteIcon(iconUrl: String?, source: PhotoPickerMediaSource?) {
        val stat = if (source == ANDROID_CAMERA) Stat.MY_SITE_ICON_SHOT_NEW else Stat.MY_SITE_ICON_GALLERY_PICKED
        analyticsTrackerWrapper.track(stat)
        val imageUri = Uri.parse(iconUrl)?.let { UriWrapper(it) }
        if (imageUri != null) {
            launch(bgDispatcher) {
                val fetchMedia = wpMediaUtilsWrapper.fetchMediaToUriWrapper(imageUri)
                if (fetchMedia != null) {
                    _onNavigation.postValue(Event(SiteNavigationAction.OpenCropActivity(fetchMedia)))
                }
            }
        }
    }

    fun handleSelectedSiteIcon(mediaId: Long) {
        selectedSiteRepository.updateSiteIconMediaId(mediaId.toInt(), true)
    }

    fun handleCropResult(croppedUri: Uri?, success: Boolean) {
        if (success && croppedUri != null) {
            analyticsTrackerWrapper.track(Stat.MY_SITE_ICON_CROPPED)
            selectedSiteRepository.showSiteIconProgressBar(true)
            launch(bgDispatcher) {
                wpMediaUtilsWrapper.fetchMediaToUriWrapper(UriWrapper(croppedUri))?.let { fetchMedia ->
                    mediaUtilsWrapper.getRealPathFromURI(fetchMedia.uri)
                }?.let {
                    startSiteIconUpload(it)
                }
            }
        } else {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.error_cropping_image))))
        }
    }

    fun handleSuccessfulLoginResult() {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            _onNavigation.value = Event(
                    SiteNavigationAction.OpenStats(site)
            )
        }
    }

    fun handleSuccessfulDomainRegistrationResult(email: String?) {
        analyticsTrackerWrapper.track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS)
        _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(getEmailValidationMessage(email))))
    }

    @Suppress("ReturnCount")
    private fun startSiteIconUpload(filePath: String) {
        if (TextUtils.isEmpty(filePath)) {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.error_locating_image))))
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.file_error_create))))
            return
        }
        val selectedSite = selectedSiteRepository.getSelectedSite()
        if (selectedSite != null) {
            val media = buildMediaModel(file, selectedSite)
            if (media == null) {
                _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.file_not_found))))
                return
            }
            _onMediaUpload.postValue(Event(media))
        } else {
            _onSnackbarMessage.postValue(Event(SnackbarMessageHolder(UiStringRes(R.string.error_generic))))
        }
    }

    private fun buildMediaModel(file: File, site: SiteModel): MediaModel? {
        val uri = Uri.Builder().path(file.path).build()
        val mimeType = contextProvider.getContext().contentResolver.getType(uri)
        return fluxCUtilsWrapper.mediaModelFromLocalUri(uri, mimeType, site.id)
    }

    private fun getStatsNavigationActionForSite(site: SiteModel) = when {
        // If the user is not logged in and the site is already connected to Jetpack, ask to login.
        !accountStore.hasAccessToken() && site.isJetpackConnected -> SiteNavigationAction.StartWPComLoginForJetpackStats

        // If it's a WordPress.com or Jetpack site, show the Stats screen.
        site.isWPCom || site.isJetpackInstalled && site.isJetpackConnected -> SiteNavigationAction.OpenStats(site)

        // If it's a self-hosted site, ask to connect to Jetpack.
        else -> SiteNavigationAction.ConnectJetpackForStats(site)
    }

    fun onAvatarPressed() {
        _onNavigation.value = Event(SiteNavigationAction.OpenMeScreen)
    }

    fun onAddSitePressed() {
        _onNavigation.value = Event(SiteNavigationAction.AddNewSite(accountStore.hasAccessToken()))
        analyticsTrackerWrapper.track(Stat.MY_SITE_NO_SITES_VIEW_ACTION_TAPPED)
    }

    override fun onCleared() {
        siteIconUploadHandler.clear()
        siteStoriesHandler.clear()
        quickStartRepository.clear()
        mySiteSourceManager.clear()
        super.onCleared()
    }

    fun handleStoriesPhotoPickerResult(data: Intent) {
        selectedSiteRepository.getSelectedSite()?.let {
            siteStoriesHandler.handleStoriesResult(it, data, STORY_FROM_MY_SITE)
        }
    }

    fun onCreateSiteResult() {
        isDefaultABExperimentTabSet = false
        selectDefaultTabIfNeeded()
    }

    fun performFirstStepAfterSiteCreation(siteLocalId: Int, isSiteTitleTaskCompleted: Boolean) {
        if (landOnTheEditorFeatureConfig.isEnabled()) {
            checkAndStartLandOnTheEditor()
        } else {
            checkAndStartQuickStart(siteLocalId, isSiteTitleTaskCompleted)
        }
    }

    private fun checkAndStartLandOnTheEditor() {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            launch(bgDispatcher) {
                homePageDataLoader.loadHomepage(selectedSite)?.pageId?.let { localHomepageId ->
                    val landOnTheEditorAction = SiteNavigationAction.OpenHomepage(selectedSite, localHomepageId)
                    _onNavigation.postValue(Event(landOnTheEditorAction))
                    analyticsTrackerWrapper.track(Stat.LANDING_EDITOR_SHOWN)
                }
            }
        }
    }

    fun checkAndStartQuickStart(siteLocalId: Int, isSiteTitleTaskCompleted: Boolean) {
        if (quickStartDynamicCardsFeatureConfig.isEnabled()) {
            startQuickStart(siteLocalId, isSiteTitleTaskCompleted)
        } else {
            shouldMarkUpdateSiteTitleTaskComplete = isSiteTitleTaskCompleted
            showQuickStartDialog(selectedSiteRepository.getSelectedSite())
        }
    }

    private fun startQuickStart(siteLocalId: Int, isSiteTitleTaskCompleted: Boolean) {
        if (siteLocalId != SelectedSiteRepository.UNAVAILABLE) {
            quickStartUtilsWrapper.startQuickStart(siteLocalId, isSiteTitleTaskCompleted)
            mySiteSourceManager.refreshQuickStart()
        }
    }

    fun onQuickStartMenuInteraction(interaction: DynamicCardMenuInteraction) {
        launch { mySiteSourceManager.onQuickStartMenuInteraction(interaction) }
    }

    private fun showQuickStartDialog(siteModel: SiteModel?) {
        if (siteModel != null && quickStartUtilsWrapper.isQuickStartAvailableForTheSite(siteModel)) {
            _onNavigation.postValue(
                    Event(
                            SiteNavigationAction.ShowQuickStartDialog(
                                    R.string.quick_start_dialog_need_help_manage_site_title,
                                    R.string.quick_start_dialog_need_help_manage_site_message,
                                    R.string.quick_start_dialog_need_help_manage_site_button_positive,
                                    R.string.quick_start_dialog_need_help_button_negative
                            )
                    )
            )
        }
    }

    fun startQuickStart() {
        analyticsTrackerWrapper.track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
        startQuickStart(selectedSiteRepository.getSelectedSiteLocalId(), shouldMarkUpdateSiteTitleTaskComplete)
        shouldMarkUpdateSiteTitleTaskComplete = false
    }

    fun ignoreQuickStart() {
        shouldMarkUpdateSiteTitleTaskComplete = false
        analyticsTrackerWrapper.track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    private fun onPostItemClick(params: PostItemClickParams) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            cardsTracker.trackPostItemClicked(params.postCardType)
            when (params.postCardType) {
                PostCardType.DRAFT -> _onNavigation.value =
                        Event(SiteNavigationAction.EditDraftPost(site, params.postId))
                PostCardType.SCHEDULED -> _onNavigation.value =
                        Event(SiteNavigationAction.EditScheduledPost(site, params.postId))
                else -> Unit // Do nothing
            }
        }
    }

    private fun onDashboardErrorRetry() {
        mySiteSourceManager.refresh()
    }

    private fun onPostCardFooterLinkClick(postCardType: PostCardType) {
        selectedSiteRepository.getSelectedSite()?.let { site ->
            cardsTracker.trackPostCardFooterLinkClicked(postCardType)
            _onNavigation.value = when (postCardType) {
                PostCardType.CREATE_FIRST, PostCardType.CREATE_NEXT ->
                    Event(SiteNavigationAction.OpenEditorToCreateNewPost(site))
                PostCardType.DRAFT -> Event(SiteNavigationAction.OpenDraftsPosts(site))
                PostCardType.SCHEDULED -> Event(SiteNavigationAction.OpenScheduledPosts(site))
            }
        }
    }

    private fun onBloggingPromptShareClick(message: String) {
        onShare.postValue(Event(message))
    }

    fun isRefreshing() = mySiteSourceManager.isRefreshing()

    fun setActionableEmptyViewGone(isVisible: Boolean, setGone: () -> Unit) {
        if (isVisible) analyticsTrackerWrapper.track(Stat.MY_SITE_NO_SITES_VIEW_HIDDEN)
        setGone()
    }

    fun setActionableEmptyViewVisible(isVisible: Boolean, setVisible: () -> Unit) {
        if (!isVisible) analyticsTrackerWrapper.track(Stat.MY_SITE_NO_SITES_VIEW_DISPLAYED)
        setVisible()
    }

    fun trackWithTabSource(event: MySiteTrackWithTabSource) {
        if (event.currentTab == MySiteTabType.ALL) {
            analyticsTrackerWrapper.track(event.stat, event.properties ?: emptyMap())
        } else {
            val props: MutableMap<String, Any> = mutableMapOf(event.key to event.currentTab.trackingLabel)
            if (!event.properties.isNullOrEmpty()) {
                props.putAll(event.properties)
            }
            analyticsTrackerWrapper.track(event.stat, props)
        }
    }

    private fun trackWithTabSourceIfNeeded(stat: Stat, properties: HashMap<String, *>? = null) {
        if (mySiteDashboardTabsFeatureConfig.isEnabled()) {
            _onTrackWithTabSource.postValue(Event(MySiteTrackWithTabSource(stat, properties)))
        } else {
            analyticsTrackerWrapper.track(stat, properties ?: emptyMap())
        }
    }

    private fun selectDefaultTabIfNeeded() {
        if (!isMySiteTabsEnabled || isDefaultABExperimentTabSet) return
        val index = orderedTabTypes.indexOf(defaultABExperimentTab)
        if (index != -1) {
            _selectTab.postValue(Event(TabNavigation(index, smoothAnimation = false)))
            isDefaultABExperimentTabSet = true
        }
    }

    private fun trackCardsAndItemsShownIfNeeded(siteSelected: SiteSelected) {
        siteSelected.cardAndItems.filterIsInstance<DomainRegistrationCard>()
                .forEach { domainRegistrationCardShownTracker.trackShown(it.type) }
        siteSelected.cardAndItems.filterIsInstance<DashboardCards>().forEach { cardsTracker.trackShown(it) }
    }

    private fun resetShownTrackers() {
        domainRegistrationCardShownTracker.resetShown()
        cardsTracker.resetShown()
    }

    private fun trackTabChanged(isSiteMenu: Boolean) {
        if (isSiteMenu) {
            analyticsTrackerWrapper.track(
                    Stat.MY_SITE_TAB_TAPPED,
                    mapOf(MY_SITE_TAB to MySiteTabType.SITE_MENU.trackingLabel)
            )
            analyticsTrackerWrapper.track(Stat.MY_SITE_SITE_MENU_SHOWN)
        } else {
            analyticsTrackerWrapper.track(
                    Stat.MY_SITE_TAB_TAPPED,
                    mapOf(MY_SITE_TAB to MySiteTabType.DASHBOARD.trackingLabel)
            )
            analyticsTrackerWrapper.track(Stat.MY_SITE_DASHBOARD_SHOWN)
        }
    }

    private fun findUiStateForTab(tabType: MySiteTabType) =
            tabsUiState.value?.tabUiStates?.firstOrNull { it.tabType == tabType }

    private fun createTabsUiState() = TabsUiState(
            showTabs = isMySiteTabsEnabled,
            tabUiStates = orderedTabTypes.mapToTabUiStates(),
            shouldUpdateViewPager = shouldUpdateViewPager()
    )

    private fun List<MySiteTabType>.mapToTabUiStates() = map {
        TabUiState(
                label = UiStringRes(it.stringResId),
                tabType = it,
                showQuickStartFocusPoint = if (it == MySiteTabType.SITE_MENU) {
                    findUiStateForTab(MySiteTabType.SITE_MENU)?.showQuickStartFocusPoint ?: false
                } else {
                    false
                }
        )
    }

    private fun shouldUpdateViewPager() = uiModel.value?.state?.tabsUiState?.tabUiStates?.size != orderedTabTypes.size

    private fun hasSiteHeaderUpdates(nextSiteInfoHeaderCard: SiteInfoHeaderCard): Boolean {
        return !((uiModel.value?.state as? SiteSelected)?.siteInfoHeaderState?.siteInfoHeader?.equals(
                nextSiteInfoHeaderCard
        ) ?: false)
    }

    data class UiModel(
        val accountAvatarUrl: String,
        val state: State
    )

    sealed class State {
        abstract val tabsUiState: TabsUiState
        abstract val siteInfoToolbarViewParams: SiteInfoToolbarViewParams

        data class SiteSelected(
            override val tabsUiState: TabsUiState,
            override val siteInfoToolbarViewParams: SiteInfoToolbarViewParams,
            val siteInfoHeaderState: SiteInfoHeaderState,
            val cardAndItems: List<MySiteCardAndItem>,
            val siteMenuCardsAndItems: List<MySiteCardAndItem>,
            val dashboardCardsAndItems: List<MySiteCardAndItem>
        ) : State()

        data class NoSites(
            override val tabsUiState: TabsUiState,
            override val siteInfoToolbarViewParams: SiteInfoToolbarViewParams,
            val shouldShowImage: Boolean
        ) : State()
    }

    data class SiteInfoHeaderState(
        val hasUpdates: Boolean,
        val siteInfoHeader: SiteInfoHeaderCard
    )

    data class TabsUiState(
        val showTabs: Boolean = false,
        val tabUiStates: List<TabUiState>,
        val shouldUpdateViewPager: Boolean = false
    ) {
        data class TabUiState(
            val label: UiString,
            val tabType: MySiteTabType,
            val showQuickStartFocusPoint: Boolean = false,
            val pendingTask: QuickStartTask? = null
        )

        fun update(quickStartSiteMenuStep: QuickStartSiteMenuStep?) = tabUiStates.map { tabUiState ->
            if (tabUiState.tabType == MySiteTabType.SITE_MENU) {
                tabUiState.copy(
                        showQuickStartFocusPoint = quickStartSiteMenuStep?.isStarted ?: false,
                        pendingTask = quickStartSiteMenuStep?.task
                )
            } else {
                tabUiState
            }
        }
    }

    data class SiteInfoToolbarViewParams(
        @DimenRes val appBarHeight: Int,
        @DimenRes val toolbarBottomMargin: Int,
        val headerVisible: Boolean = true,
        val appBarLiftOnScroll: Boolean = false
    )

    data class TabNavigation(val position: Int, val smoothAnimation: Boolean)

    data class TextInputDialogModel(
        val callbackId: Int = SITE_NAME_CHANGE_CALLBACK_ID,
        @StringRes val title: Int,
        val initialText: String,
        @StringRes val hint: Int,
        val isMultiline: Boolean,
        val isInputEnabled: Boolean
    )

    private data class SiteIdToState(val siteId: Int?, val state: MySiteUiState = MySiteUiState()) {
        fun update(partialState: PartialState): SiteIdToState {
            return this.copy(state = state.update(partialState))
        }
    }

    data class MySiteTrackWithTabSource(
        val stat: Stat,
        val properties: HashMap<String, *>? = null,
        val key: String = TAB_SOURCE,
        val currentTab: MySiteTabType = MySiteTabType.ALL
    )

    companion object {
        private const val MIN_DISPLAY_PX_HEIGHT_NO_SITE_IMAGE = 600
        private const val LIST_INDEX_NO_ACTIVE_QUICK_START_ITEM = -1
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
        const val ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK"
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
        const val LIST_SCROLL_DELAY_MS = 500L
        const val MY_SITE_TAB = "tab"
        const val TAB_SOURCE = "tab_source"
    }
}
