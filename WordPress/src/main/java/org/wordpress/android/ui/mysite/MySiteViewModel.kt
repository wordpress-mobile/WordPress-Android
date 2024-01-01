@file:Suppress("DEPRECATION", "MaximumLineLength")

package org.wordpress.android.ui.mysite

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.QuickStartStore.Companion.QUICK_START_VIEW_SITE_LABEL
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.models.JetpackPoweredScreen
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.PagePostCreationSourcesDetail.STORY_FROM_MY_SITE
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource.FEATURE_CARD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.GetShowJetpackFullPluginInstallOnboardingUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.cards.DashboardCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.DomainRegistrationCardShownTracker
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.plans.PlansCardUtils
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardShownTracker
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginShownTracker
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.sotw2023.WpSotw2023NudgeCardViewModelSlice
import org.wordpress.android.ui.mysite.items.infoitem.MySiteInfoItemBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.LandOnTheEditorFeatureConfig
import org.wordpress.android.util.filter
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.util.mapSafe
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

@Suppress("LargeClass", "LongMethod", "LongParameterList")
class MySiteViewModel @Inject constructor(
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteIconUploadHandler: SiteIconUploadHandler,
    private val siteStoriesHandler: SiteStoriesHandler,
    private val displayUtilsWrapper: DisplayUtilsWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val homePageDataLoader: HomePageDataLoader,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val snackbarSequencer: SnackbarSequencer,
    private val landOnTheEditorFeatureConfig: LandOnTheEditorFeatureConfig,
    private val cardsTracker: CardsTracker,
    private val domainRegistrationCardShownTracker: DomainRegistrationCardShownTracker,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val jetpackBrandingUtils: JetpackBrandingUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val quickStartTracker: QuickStartTracker,
    private val dispatcher: Dispatcher,
    private val jetpackFeatureCardShownTracker: JetpackFeatureCardShownTracker,
    private val jetpackFeatureRemovalUtils: JetpackFeatureRemovalOverlayUtil,
    private val jetpackFeatureCardHelper: JetpackFeatureCardHelper,
    private val getShowJetpackFullPluginInstallOnboardingUseCase: GetShowJetpackFullPluginInstallOnboardingUseCase,
    private val jetpackInstallFullPluginShownTracker: JetpackInstallFullPluginShownTracker,
    private val dashboardCardPlansUtils: PlansCardUtils,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
    private val siteItemsViewModelSlice: SiteItemsViewModelSlice,
    private val mySiteInfoItemBuilder: MySiteInfoItemBuilder,
    private val siteInfoHeaderCardViewModelSlice: SiteInfoHeaderCardViewModelSlice,
    private val sotw2023NudgeCardViewModelSlice: WpSotw2023NudgeCardViewModelSlice,
    private val dashboardCardsViewModelSlice: DashboardCardsViewModelSlice
) : ScopedViewModel(mainDispatcher) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    private val _activeTaskPosition = MutableLiveData<Pair<QuickStartTask, Int>>()
    private val _onOpenJetpackInstallFullPluginOnboarding = SingleLiveEvent<Event<Unit>>()
    private val _onShowJetpackIndividualPluginOverlay = SingleLiveEvent<Event<Unit>>()

    /* Capture and track the site selected event so we can circumvent refreshing sources on resume
       as they're already built on site select. */
    private var isSiteSelected = false

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
    val onSnackbarMessage = merge(
        _onSnackbarMessage,
        siteStoriesHandler.onSnackbar,
        quickStartRepository.onSnackbar,
        siteItemsViewModelSlice.onSnackbarMessage,
        siteInfoHeaderCardViewModelSlice.onSnackbarMessage,
        dashboardCardsViewModelSlice.onSnackbarMessage
    )
    val onQuickStartMySitePrompts = quickStartRepository.onQuickStartMySitePrompts

    val onTextInputDialogShown = siteInfoHeaderCardViewModelSlice.onTextInputDialogShown

    val onBasicDialogShown = siteInfoHeaderCardViewModelSlice.onBasicDialogShown

    val onNavigation = merge(
        _onNavigation,
        siteStoriesHandler.onNavigation,
        siteItemsViewModelSlice.onNavigation,
        siteInfoHeaderCardViewModelSlice.onNavigation,
        sotw2023NudgeCardViewModelSlice.onNavigation,
        dashboardCardsViewModelSlice.onNavigation
    )

    val onMediaUpload = siteInfoHeaderCardViewModelSlice.onMediaUpload
    val onUploadedItem = siteIconUploadHandler.onUploadedItem

    val onOpenJetpackInstallFullPluginOnboarding: LiveData<Event<Unit>> = merge(
        _onOpenJetpackInstallFullPluginOnboarding,
        dashboardCardsViewModelSlice.onOpenJetpackInstallFullPluginOnboarding
    )

    val onShowJetpackIndividualPluginOverlay = _onShowJetpackIndividualPluginOverlay as LiveData<Event<Unit>>

    val refresh =
        merge(
            sotw2023NudgeCardViewModelSlice.refresh,
            dashboardCardsViewModelSlice.refresh
        )

    val isRefreshingOrLoading = dashboardCardsViewModelSlice.isRefreshing

    private var shouldMarkUpdateSiteTitleTaskComplete = false

    val state: LiveData<MySiteUiState> =
        selectedSiteRepository.siteSelected.switchMap { siteLocalId ->
            isSiteSelected = true
            dashboardCardsViewModelSlice.onSiteChanged()
            resetShownTrackers()
            val result = MediatorLiveData<SiteIdToState>()

            // We want to filter out the empty state where we have a site ID but site object is missing.
            // Without this check there is an emission of a NoSites state even if we have the site
            result.filter { it.siteId == null || it.state.site != null }.mapSafe { it.state }
        }

    val uiModel: LiveData<State> = merge(
        siteInfoHeaderCardViewModelSlice.uiModel,
        dashboardCardsViewModelSlice.uiModel
    ) { siteInfoHeaderCard,
        dashboardCards ->
        val nonNullSiteInfoHeaderCard = siteInfoHeaderCard ?: return@merge buildNoSiteState(null, null)
        val state = dashboardCards?.let {
            SiteSelected(
                dashboardData = mutableListOf(nonNullSiteInfoHeaderCard) + dashboardCards
            )
        } ?: SiteSelected(
            dashboardData = mutableListOf(nonNullSiteInfoHeaderCard)
        )
        return@merge state
    }

    init {
        dispatcher.register(this)
        siteInfoHeaderCardViewModelSlice.initialize(viewModelScope)
        sotw2023NudgeCardViewModelSlice.initialize(viewModelScope)
        dashboardCardsViewModelSlice.initialize(viewModelScope)
    }

    private fun getPositionOfQuickStartItem(
        siteItems: List<MySiteCardAndItem>,
    ): Int {
        return siteItems.indexOfFirst { it.activeQuickStartItem }
    }

    private fun buildSiteItemsMenu(
        site: SiteModel,
        activeTask: QuickStartTask?,
        backupAvailable: Boolean,
        scanAvailable: Boolean,
        cardsUpdate: CardsUpdate?
    ): List<MySiteCardAndItem> {
        val infoItem = mySiteInfoItemBuilder.build(
            InfoItemBuilderParams(
                isStaleMessagePresent = cardsUpdate?.showStaleMessage ?: false
            )
        )
        val jetpackFeatureCard = getJetpackFeatureCard()

        val jetpackSwitchMenu = getJetpackSwitchMenu()

        val jetpackBadge = buildJetpackBadgeIfEnabled()

        val siteItems = getSiteItems(site, activeTask, backupAvailable, scanAvailable)

        val sotw2023Card = sotw2023NudgeCardViewModelSlice.buildCard()

        return mutableListOf<MySiteCardAndItem>().apply {
            infoItem?.let { add(infoItem) }
            sotw2023Card?.let { add(it) }
            addAll(siteItems)
            jetpackSwitchMenu?.let { add(jetpackSwitchMenu) }
            if (jetpackFeatureCardHelper.shouldShowFeatureCardAtTop())
                jetpackFeatureCard?.let { add(0, jetpackFeatureCard) }
            else jetpackFeatureCard?.let { add(jetpackFeatureCard) }
            jetpackBadge?.let { add(jetpackBadge) }
        }.toList()
    }

    private fun shouldShowDashboard(site: SiteModel): Boolean {
        return buildConfigWrapper.isJetpackApp && site.isUsingWpComRestApi
    }

    private fun getSiteItems(
        site: SiteModel,
        activeTask: QuickStartTask?,
        backupAvailable: Boolean,
        scanAvailable: Boolean
    ): List<MySiteCardAndItem> {
        if (shouldShowDashboard(site)) return emptyList()
        return siteItemsBuilder.build(
            siteItemsViewModelSlice.buildItems(
                shouldEnableFocusPoints = false,
                site = site,
                activeTask = activeTask,
                backupAvailable = backupAvailable,
                scanAvailable = scanAvailable
            )
        )
    }

    private fun getJetpackSwitchMenu(): MySiteCardAndItem.Card.JetpackSwitchMenu? {
        if (!jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()) return null
        return MySiteCardAndItem.Card.JetpackSwitchMenu(
            onClick = ListItemInteraction.create(this::onJetpackFeatureCardClick),
            onRemindMeLaterItemClick = ListItemInteraction.create(this::onSwitchToJetpackMenuCardRemindMeLaterClick),
            onHideMenuItemClick = ListItemInteraction.create(this::onSwitchToJetpackMenuCardHideMenuItemClick),
            onMoreMenuClick = ListItemInteraction.create(this::onJetpackFeatureCardMoreMenuClick)
        )
    }

    private fun getJetpackFeatureCard(): JetpackFeatureCard? {
        if (!jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()) return null
        return JetpackFeatureCard(
            content = jetpackFeatureCardHelper.getCardContent(),
            onClick = ListItemInteraction.create(this::onJetpackFeatureCardClick),
            onHideMenuItemClick = ListItemInteraction.create(this::onJetpackFeatureCardHideMenuItemClick),
            onLearnMoreClick = ListItemInteraction.create(this::onJetpackFeatureCardLearnMoreClick),
            onRemindMeLaterItemClick = ListItemInteraction.create(this::onJetpackFeatureCardRemindMeLaterClick),
            onMoreMenuClick = ListItemInteraction.create(this::onJetpackFeatureCardMoreMenuClick),
            learnMoreUrl = jetpackFeatureCardHelper.getLearnMoreUrl()
        )
    }

    private fun buildJetpackBadgeIfEnabled(): JetpackBadge? {
        val screen = JetpackPoweredScreen.WithStaticText.HOME
        return JetpackBadge(
            text = jetpackBrandingUtils.getBrandingTextForScreen(screen),
            onClick = if (jetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                ListItemInteraction.create(screen, this::onJetpackBadgeClick)
            } else {
                null
            }
        ).takeIf {
            jetpackBrandingUtils.shouldShowJetpackBrandingInDashboard()
        }
    }

    private fun onJetpackBadgeClick(screen: JetpackPoweredScreen) {
        jetpackBrandingUtils.trackBadgeTapped(screen)
        _onNavigation.value = Event(SiteNavigationAction.OpenJetpackPoweredBottomSheet)
    }

    private fun buildNoSiteState(accountUrl: String?, accountName: String?): NoSites {
        // Hide actionable empty view image when screen height is under specified min height.
        val shouldShowImage = !buildConfigWrapper.isJetpackApp &&
                displayUtilsWrapper.getWindowPixelHeight() >= MIN_DISPLAY_PX_HEIGHT_NO_SITE_IMAGE

        val shouldShowAccountSettings = jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()
        return NoSites(
            shouldShowImage = shouldShowImage,
            avatarUrl = accountUrl,
            accountName = accountName,
            shouldShowAccountSettings = shouldShowAccountSettings
        )
    }

    private fun scrollToQuickStartTaskIfNecessary(
        quickStartTask: QuickStartTask,
        position: Int
    ) {
        if (_activeTaskPosition.value?.first != quickStartTask && isValidQuickStartFocusPosition(
                quickStartTask,
                position
            )
        ) {
            _activeTaskPosition.postValue(quickStartTask to position)
        }
    }

    private fun isValidQuickStartFocusPosition(quickStartTask: QuickStartTask, position: Int): Boolean {
        return if (position == LIST_INDEX_NO_ACTIVE_QUICK_START_ITEM && isSiteHeaderQuickStartTask(quickStartTask)) {
            true
        } else {
            position >= 0
        }
    }

    private fun isSiteHeaderQuickStartTask(quickStartTask: QuickStartTask): Boolean {
        return when (quickStartTask) {
            QuickStartNewSiteTask.UPDATE_SITE_TITLE,
            QuickStartNewSiteTask.UPLOAD_SITE_ICON,
            quickStartRepository.quickStartType.getTaskFromString(QUICK_START_VIEW_SITE_LABEL) -> true

            else -> false
        }
    }

    fun onQuickStartTaskCardClick(task: QuickStartTask) {
        quickStartRepository.setActiveTask(task)
    }

    fun onQuickStartFullScreenDialogDismiss() {
//        mySiteSourceManager.refreshQuickStart()
    }

    fun refresh(isPullToRefresh: Boolean = false) {
        Log.e("MySiteViewModel", "refresh")
        if (isPullToRefresh) analyticsTrackerWrapper.track(Stat.MY_SITE_PULL_TO_REFRESH)
        dashboardCardsViewModelSlice.onRefresh()
    }

    fun onResume() {
//        mySiteSourceManager.onResume(isSiteSelected)
        isSiteSelected = false
        checkAndShowJetpackFullPluginInstallOnboarding()
        checkAndShowQuickStartNotice()
//        bloggingPromptCardViewModelSlice.onResume(uiModel.value as? SiteSelected)
//        dashboardCardPlansUtils.onResume(uiModel.value as? SiteSelected)
        siteInfoHeaderCardViewModelSlice.onResume()
        dashboardCardsViewModelSlice.onResume()
    }

    private fun checkAndShowJetpackFullPluginInstallOnboarding() {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            if (getShowJetpackFullPluginInstallOnboardingUseCase.execute(selectedSite)) {
                _onOpenJetpackInstallFullPluginOnboarding.postValue(Event(Unit))
            }
        }
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
        siteInfoHeaderCardViewModelSlice.onSiteNameChosen(input)
    }

    fun onSiteNameChooserDismissed() {
        siteInfoHeaderCardViewModelSlice.onSiteNameChooserDismissed()
    }

    fun onDialogInteraction(interaction: BasicDialogViewModel.DialogInteraction) {
        siteInfoHeaderCardViewModelSlice.onDialogInteraction(interaction)
    }

    fun handleCropResult(output: Uri?, success: Boolean) {
        siteInfoHeaderCardViewModelSlice.handleCropResult(output, success)
    }

    fun handleSelectedSiteIcon(mediaId: Long) = siteInfoHeaderCardViewModelSlice.handleSelectedSiteIcon(mediaId)

    fun handleTakenSiteIcon(iconUrl: String, source: PhotoPickerActivity.PhotoPickerMediaSource?) {
        siteInfoHeaderCardViewModelSlice.handleTakenSiteIcon(iconUrl, source)
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

    fun onAvatarPressed() {
        _onNavigation.value = Event(SiteNavigationAction.OpenMeScreen)
    }

    fun onAddSitePressed() {
        _onNavigation.value = Event(
            SiteNavigationAction.AddNewSite(
                accountStore.hasAccessToken(),
                SiteCreationSource.MY_SITE_NO_SITES
            )
        )
        analyticsTrackerWrapper.track(Stat.MY_SITE_NO_SITES_VIEW_ACTION_TAPPED)
    }

    override fun onCleared() {
        siteIconUploadHandler.clear()
        siteStoriesHandler.clear()
        quickStartRepository.clear()
        dispatcher.unregister(this)
        dashboardCardsViewModelSlice.onCleared()
        super.onCleared()
    }

    fun handleStoriesPhotoPickerResult(data: Intent) {
        selectedSiteRepository.getSelectedSite()?.let {
            siteStoriesHandler.handleStoriesResult(it, data, STORY_FROM_MY_SITE)
        }
    }

    fun onSitePicked() {
        selectedSiteRepository.getSelectedSite()?.let {
            val siteLocalId = it.id.toLong()
            val lastSelectedQuickStartType = appPrefsWrapper.getLastSelectedQuickStartTypeForSite(siteLocalId)
            quickStartRepository.checkAndSetQuickStartType(lastSelectedQuickStartType == NewSiteQuickStartType)
        }
    }

    fun performFirstStepAfterSiteCreation(
        isSiteTitleTaskCompleted: Boolean,
        isNewSite: Boolean
    ) {
        if (landOnTheEditorFeatureConfig.isEnabled()) {
            checkAndStartLandOnTheEditor(isNewSite)
        } else {
            checkAndStartQuickStart(isSiteTitleTaskCompleted, isNewSite)
        }
    }

    private fun checkAndStartLandOnTheEditor(isNewSite: Boolean) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            launch(bgDispatcher) {
                homePageDataLoader.loadHomepage(selectedSite)?.pageId?.let { localHomepageId ->
                    val landOnTheEditorAction = SiteNavigationAction
                        .OpenHomepage(selectedSite, localHomepageId, isNewSite)
                    _onNavigation.postValue(Event(landOnTheEditorAction))
                    analyticsTrackerWrapper.track(Stat.LANDING_EDITOR_SHOWN)
                }
            }
        }
    }

    fun checkAndStartQuickStart(
        isSiteTitleTaskCompleted: Boolean,
        isNewSite: Boolean
    ) {
        if (!jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()) return
        quickStartRepository.checkAndSetQuickStartType(isNewSite = isNewSite)
        shouldMarkUpdateSiteTitleTaskComplete = isSiteTitleTaskCompleted
        showQuickStartDialog(selectedSiteRepository.getSelectedSite(), isNewSite)
    }

    private fun startQuickStart(siteLocalId: Int, isSiteTitleTaskCompleted: Boolean) {
        if (siteLocalId != SelectedSiteRepository.UNAVAILABLE) {
            quickStartUtilsWrapper
                .startQuickStart(
                    siteLocalId,
                    isSiteTitleTaskCompleted,
                    quickStartRepository.quickStartType,
                    quickStartTracker
                )
//            mySiteSourceManager.refreshQuickStart()
        }
    }

    private fun showQuickStartDialog(siteModel: SiteModel?, isNewSite: Boolean) {
        if (siteModel != null && quickStartUtilsWrapper.isQuickStartAvailableForTheSite(siteModel) &&
            !jetpackFeatureRemovalUtils.shouldHideJetpackFeatures()
        ) {
            _onNavigation.postValue(
                Event(
                    SiteNavigationAction.ShowQuickStartDialog(
                        R.string.quick_start_dialog_need_help_manage_site_title,
                        R.string.quick_start_dialog_need_help_manage_site_message,
                        R.string.quick_start_dialog_need_help_manage_site_button_positive,
                        R.string.quick_start_dialog_need_help_button_negative,
                        isNewSite
                    )
                )
            )
        }
    }

    fun startQuickStart() {
        quickStartTracker.track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
        startQuickStart(selectedSiteRepository.getSelectedSiteLocalId(), shouldMarkUpdateSiteTitleTaskComplete)
        shouldMarkUpdateSiteTitleTaskComplete = false
    }

    fun ignoreQuickStart() {
        shouldMarkUpdateSiteTitleTaskComplete = false
        quickStartTracker.track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    private fun onDashboardErrorRetry() {
//        mySiteSourceManager.refresh()
    }


    private fun onJetpackFeatureCardClick() {
        jetpackFeatureCardHelper.track(Stat.REMOVE_FEATURE_CARD_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenJetpackFeatureOverlay(source = FEATURE_CARD))
    }

    private fun onJetpackFeatureCardHideMenuItemClick() {
        jetpackFeatureCardHelper.hideJetpackFeatureCard()
        refresh()
    }

    private fun onJetpackFeatureCardLearnMoreClick() {
        jetpackFeatureCardHelper.track(Stat.REMOVE_FEATURE_CARD_LINK_TAPPED)
        _onNavigation.value = Event(SiteNavigationAction.OpenJetpackFeatureOverlay(source = FEATURE_CARD))
    }

    private fun onJetpackFeatureCardRemindMeLaterClick() {
        jetpackFeatureCardHelper.setJetpackFeatureCardLastShownTimeStamp(System.currentTimeMillis())
        refresh()
    }

    private fun onSwitchToJetpackMenuCardRemindMeLaterClick() {
        jetpackFeatureCardHelper.track(Stat.REMOVE_FEATURE_CARD_REMIND_LATER_TAPPED)
        appPrefsWrapper.setSwitchToJetpackMenuCardLastShownTimestamp(System.currentTimeMillis())
        refresh()
    }

    private fun onSwitchToJetpackMenuCardHideMenuItemClick() {
        jetpackFeatureCardHelper.hideSwitchToJetpackMenuCard()
        refresh()
    }

    private fun onJetpackFeatureCardMoreMenuClick() {
        jetpackFeatureCardHelper.track(Stat.REMOVE_FEATURE_CARD_MENU_ACCESSED)
    }

    fun onActionableEmptyViewVisible() {
        analyticsTrackerWrapper.track(Stat.MY_SITE_NO_SITES_VIEW_DISPLAYED)
        checkJetpackIndividualPluginOverlayShouldShow()
    }

    private fun checkJetpackIndividualPluginOverlayShouldShow() {
        // don't check if already shown
        if (_onShowJetpackIndividualPluginOverlay.value?.peekContent() == Unit) return

        viewModelScope.launch {
            val showOverlay = wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()
            if (showOverlay) {
                delay(DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
                _onShowJetpackIndividualPluginOverlay.value = Event(Unit)
            }
        }
    }

    fun onBloggingPromptsLearnMoreClicked() {
        _onNavigation.postValue(Event(BloggingPromptCardNavigationAction.LearnMore))
    }

    private fun trackCardsAndItemsShownIfNeeded(siteSelected: SiteSelected) {
        siteSelected.dashboardData.filterIsInstance<DomainRegistrationCard>()
            .forEach { domainRegistrationCardShownTracker.trackShown(it.type) }
        siteSelected.dashboardData.filterIsInstance<MySiteCardAndItem.Card>()
            .let { cardsTracker.trackShown(it) }
        siteSelected.dashboardData.filterIsInstance<QuickStartCard>()
            .firstOrNull()?.let { quickStartTracker.trackShown(it.type) }
        siteSelected.dashboardData.filterIsInstance<QuickStartCard>()
            .firstOrNull()?.let { cardsTracker.trackQuickStartCardShown(quickStartRepository.quickStartType) }
        siteSelected.dashboardData.filterIsInstance<JetpackFeatureCard>()
            .forEach { jetpackFeatureCardShownTracker.trackShown(it.type) }
        siteSelected.dashboardData.filterIsInstance<JetpackInstallFullPluginCard>()
            .forEach { jetpackInstallFullPluginShownTracker.trackShown(it.type) }
        dashboardCardPlansUtils.trackCardShown(viewModelScope, siteSelected)
//        siteSelected.dashboardData.filterIsInstance<MySiteCardAndItem.Card.PersonalizeCardModel>()
//            .forEach { personalizeCardViewModelSlice.trackShown(it.type) }
//        siteSelected.dashboardData.filterIsInstance<MySiteCardAndItem.Card.NoCardsMessage>()
//            .forEach { noCardsMessageViewModelSlice.trackShown(it.type) }
        siteSelected.dashboardData.filterIsInstance<MySiteCardAndItem.Card.WpSotw2023NudgeCardModel>()
            .forEach { _ -> sotw2023NudgeCardViewModelSlice.trackShown() }
//        siteSelected.dashboardData.filterIsInstance<MySiteCardAndItem.Card.Dynamic>()
//            .forEach { dynamicCardsViewModelSlice.trackShown(it.id) }
    }

    private fun resetShownTrackers() {
        domainRegistrationCardShownTracker.resetShown()
        cardsTracker.resetShown()
        quickStartTracker.resetShown()
        jetpackFeatureCardShownTracker.resetShown()
        jetpackInstallFullPluginShownTracker.resetShown()
        dashboardCardsViewModelSlice.resetShownTracker()
//        personalizeCardViewModelSlice.resetShown()
        sotw2023NudgeCardViewModelSlice.resetShown()
//        dynamicCardsViewModelSlice.resetShown()
    }

    // FluxC events
    @Subscribe(threadMode = MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (!event.isError) {
            event.post?.let {
                if (event.post.answeredPromptId > 0 && event.isFirstTimePublish) {
                    dashboardCardsViewModelSlice.refreshBloggingPrompt()
                }
            }
        }
    }

    sealed class State {
        data class SiteSelected(
            val dashboardData: List<MySiteCardAndItem>,
        ) : State()

        data class NoSites(
            val shouldShowImage: Boolean,
            val avatarUrl: String? = null,
            val accountName: String? = null,
            val shouldShowAccountSettings: Boolean = false
        ) : State()
    }

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

    companion object {
        private const val MIN_DISPLAY_PX_HEIGHT_NO_SITE_IMAGE = 600
        private const val LIST_INDEX_NO_ACTIVE_QUICK_START_ITEM = -1
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
        const val ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK"
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
        private const val DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY = 500L
    }
}
