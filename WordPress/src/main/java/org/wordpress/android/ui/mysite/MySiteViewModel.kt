@file:Suppress("DEPRECATION", "MaximumLineLength")

package org.wordpress.android.ui.mysite

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.GetShowJetpackFullPluginInstallOnboardingUseCase
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.cards.DashboardCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardViewModelSlice
import org.wordpress.android.ui.mysite.items.DashboardItemsViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.PhotoPickerActivity
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType.NewSiteQuickStartType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.LandOnTheEditorFeatureConfig
import org.wordpress.android.util.getEmailValidationMessage
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
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteIconUploadHandler: SiteIconUploadHandler,
    private val quickStartRepository: QuickStartRepository,
    private val homePageDataLoader: HomePageDataLoader,
    private val quickStartUtilsWrapper: QuickStartUtilsWrapper,
    private val snackbarSequencer: SnackbarSequencer,
    private val landOnTheEditorFeatureConfig: LandOnTheEditorFeatureConfig,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val quickStartTracker: QuickStartTracker,
    private val dispatcher: Dispatcher,
    private val jetpackFeatureRemovalUtils: JetpackFeatureRemovalOverlayUtil,
    private val getShowJetpackFullPluginInstallOnboardingUseCase: GetShowJetpackFullPluginInstallOnboardingUseCase,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
    private val siteInfoHeaderCardViewModelSlice: SiteInfoHeaderCardViewModelSlice,
    private val accountDataViewModelSlice: AccountDataViewModelSlice,
    private val dashboardCardsViewModelSlice: DashboardCardsViewModelSlice,
    private val dashboardItemsViewModelSlice: DashboardItemsViewModelSlice
) : ScopedViewModel(mainDispatcher) {
    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    private val _onOpenJetpackInstallFullPluginOnboarding = SingleLiveEvent<Event<Unit>>()
    private val _onShowJetpackIndividualPluginOverlay = SingleLiveEvent<Event<Unit>>()

    /* Capture and track the site selected event so we can circumvent refreshing sources on resume
       as they're already built on site select. */
    private var isSiteSelected = false

    val onScrollTo: MutableLiveData<Event<Int>> = MutableLiveData()

    val onSnackbarMessage = merge(
        _onSnackbarMessage,
        quickStartRepository.onSnackbar,
        dashboardItemsViewModelSlice.onSnackbarMessage,
        siteInfoHeaderCardViewModelSlice.onSnackbarMessage,
        dashboardCardsViewModelSlice.onSnackbarMessage
    )
    val onQuickStartMySitePrompts = quickStartRepository.onQuickStartMySitePrompts

    val onTextInputDialogShown = siteInfoHeaderCardViewModelSlice.onTextInputDialogShown

    val onBasicDialogShown = siteInfoHeaderCardViewModelSlice.onBasicDialogShown

    val onNavigation = merge(
        _onNavigation,
        siteInfoHeaderCardViewModelSlice.onNavigation,
        dashboardCardsViewModelSlice.onNavigation,
        dashboardItemsViewModelSlice.onNavigation
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
            dashboardCardsViewModelSlice.refresh
        )

    val isRefreshingOrLoading = merge(
        dashboardCardsViewModelSlice.isRefreshing,
        dashboardItemsViewModelSlice.isRefreshing,
        accountDataViewModelSlice.isRefreshing
    )

    private var shouldMarkUpdateSiteTitleTaskComplete = false

    val uiModel: LiveData<State> = merge(
        siteInfoHeaderCardViewModelSlice.uiModel,
        accountDataViewModelSlice.uiModel,
        dashboardCardsViewModelSlice.uiModel,
        dashboardItemsViewModelSlice.uiModel
    ) { siteInfoHeaderCard,
        accountData,
        dashboardCards,
        siteItems ->
        val nonNullSiteInfoHeaderCard =
            siteInfoHeaderCard ?: return@merge buildNoSiteState(accountData?.url, accountData?.name)
        return@merge if (!dashboardCards.isNullOrEmpty<MySiteCardAndItem>())
            SiteSelected(dashboardData = listOf(nonNullSiteInfoHeaderCard) + dashboardCards)
        else if (!siteItems.isNullOrEmpty<MySiteCardAndItem>())
            SiteSelected(dashboardData = listOf(nonNullSiteInfoHeaderCard) + siteItems)
        else
            SiteSelected(dashboardData = listOf(nonNullSiteInfoHeaderCard))
    }.distinctUntilChanged()

    init {
        dispatcher.register(this)
        siteInfoHeaderCardViewModelSlice.initialize(viewModelScope)
        dashboardCardsViewModelSlice.initialize(viewModelScope)
        dashboardItemsViewModelSlice.initialize(viewModelScope)
        accountDataViewModelSlice.initialize(viewModelScope)
    }

    private fun shouldShowDashboard(site: SiteModel): Boolean {
        return buildConfigWrapper.isJetpackApp && site.isUsingWpComRestApi
    }

    private fun buildNoSiteState(accountUrl: String?, accountName: String?): NoSites {
        return NoSites(
            avatarUrl = accountUrl,
            accountName = accountName,
        )
    }

    fun onQuickStartTaskCardClick(task: QuickStartTask) {
        onScrollTo.postValue(Event(0))
        quickStartRepository.setActiveTask(task)
    }

    fun onQuickStartFullScreenDialogDismiss() {
//        mySiteSourceManager.refreshQuickStart()
    }

    fun refresh(isPullToRefresh: Boolean = false) {
        if (isPullToRefresh) analyticsTrackerWrapper.track(Stat.MY_SITE_PULL_TO_REFRESH)
        selectedSiteRepository.getSelectedSite()?.let {
            buildDashboardOrSiteItems(it)
        } ?: run {
            accountDataViewModelSlice.onRefresh()
        }
    }

    fun onResume() {
        isSiteSelected = false
        checkAndShowJetpackFullPluginInstallOnboarding()
        checkAndShowQuickStartNotice()
        selectedSiteRepository.updateSiteSettingsIfNecessary()
        selectedSiteRepository.getSelectedSite()?.let {
            buildDashboardOrSiteItems(it)
        } ?: run {
            accountDataViewModelSlice.onResume()
        }
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
        quickStartRepository.clear()
        dispatcher.unregister(this)
        siteInfoHeaderCardViewModelSlice.onCleared()
        dashboardCardsViewModelSlice.onCleared()
        dashboardItemsViewModelSlice.onCleared()
        accountDataViewModelSlice.onCleared()
        super.onCleared()
    }

    fun onSitePicked() {
        selectedSiteRepository.getSelectedSite()?.let {
            val siteLocalId = it.id.toLong()
            val lastSelectedQuickStartType = appPrefsWrapper.getLastSelectedQuickStartTypeForSite(siteLocalId)
            quickStartRepository.checkAndSetQuickStartType(lastSelectedQuickStartType == NewSiteQuickStartType)
            onSitePicked(it)
        } ?: run {
            accountDataViewModelSlice.onResume()
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
            quickStartRepository.checkAndShowQuickStartNotice()
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
        selectedSiteRepository.getSelectedSite()?.let {
            quickStartTracker.track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
            startQuickStart(selectedSiteRepository.getSelectedSiteLocalId(), shouldMarkUpdateSiteTitleTaskComplete)
            shouldMarkUpdateSiteTitleTaskComplete = false
            dashboardCardsViewModelSlice.startQuickStart(it)
        }
    }

    fun ignoreQuickStart() {
        shouldMarkUpdateSiteTitleTaskComplete = false
        quickStartTracker.track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    private fun buildDashboardOrSiteItems(site: SiteModel) {
        siteInfoHeaderCardViewModelSlice.buildCard(site)
        if (shouldShowDashboard(site)) {
            dashboardCardsViewModelSlice.buildCards(site)
            dashboardItemsViewModelSlice.clearValue()
        } else {
            dashboardItemsViewModelSlice.buildItems(site)
            dashboardCardsViewModelSlice.clearValue()
        }
    }

    private fun onSitePicked(site: SiteModel) {
        siteInfoHeaderCardViewModelSlice.buildCard(site)
        dashboardItemsViewModelSlice.clearValue()
        dashboardCardsViewModelSlice.clearValue()
        dashboardCardsViewModelSlice.resetShownTracker()
        dashboardItemsViewModelSlice.resetShownTracker()
        if (shouldShowDashboard(site)) {
            dashboardCardsViewModelSlice.buildCards(site)
        } else {
            dashboardItemsViewModelSlice.buildItems(site)
        }
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
            val avatarUrl: String? = null,
            val accountName: String? = null,
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

    companion object {
        const val TAG_ADD_SITE_ICON_DIALOG = "TAG_ADD_SITE_ICON_DIALOG"
        const val TAG_CHANGE_SITE_ICON_DIALOG = "TAG_CHANGE_SITE_ICON_DIALOG"
        const val TAG_REMOVE_NEXT_STEPS_DIALOG = "TAG_REMOVE_NEXT_STEPS_DIALOG"
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
        const val ARG_QUICK_START_TASK = "ARG_QUICK_START_TASK"
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
        private const val DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY = 500L
    }
}
