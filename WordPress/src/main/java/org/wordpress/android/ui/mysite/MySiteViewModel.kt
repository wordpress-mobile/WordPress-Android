@file:Suppress("MaximumLineLength")

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
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.GetShowJetpackFullPluginInstallOnboardingUseCase
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.cards.DashboardCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardViewModelSlice
import org.wordpress.android.ui.mysite.items.DashboardItemsViewModelSlice
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.mediapicker.MediaPickerActivity
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.getEmailValidationMessage
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import org.wordpress.android.ui.mysite.cards.applicationpassword.ApplicationPasswordViewModelSlice
import org.wordpress.android.ui.posts.GutenbergKitWarmupHelper
import org.wordpress.android.ui.utils.UiString

@Suppress("LargeClass", "LongMethod", "LongParameterList")
class MySiteViewModel @Inject constructor(
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val accountStore: AccountStore,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteIconUploadHandler: SiteIconUploadHandler,
    private val homePageDataLoader: HomePageDataLoader,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val dispatcher: Dispatcher,
    private val getShowJetpackFullPluginInstallOnboardingUseCase: GetShowJetpackFullPluginInstallOnboardingUseCase,
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
    private val siteInfoHeaderCardViewModelSlice: SiteInfoHeaderCardViewModelSlice,
    private val accountDataViewModelSlice: AccountDataViewModelSlice,
    private val dashboardCardsViewModelSlice: DashboardCardsViewModelSlice,
    private val dashboardItemsViewModelSlice: DashboardItemsViewModelSlice,
    private val applicationPasswordViewModelSlice: ApplicationPasswordViewModelSlice,
    private val gutenbergKitWarmupHelper: GutenbergKitWarmupHelper,
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
        dashboardItemsViewModelSlice.onSnackbarMessage,
        siteInfoHeaderCardViewModelSlice.onSnackbarMessage,
        dashboardCardsViewModelSlice.onSnackbarMessage,
        applicationPasswordViewModelSlice.onSnackbarMessage
    )

    val onTextInputDialogShown = siteInfoHeaderCardViewModelSlice.onTextInputDialogShown

    val onBasicDialogShown = siteInfoHeaderCardViewModelSlice.onBasicDialogShown

    val onNavigation = merge(
        _onNavigation,
        applicationPasswordViewModelSlice.onNavigation,
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

    val uiModel: LiveData<State> = merge(
        siteInfoHeaderCardViewModelSlice.uiModel,
        applicationPasswordViewModelSlice.uiModel,
        accountDataViewModelSlice.uiModel,
        dashboardCardsViewModelSlice.uiModel,
        dashboardItemsViewModelSlice.uiModel
    ) { siteInfoHeaderCard,
        applicationPAsswordModel,
        accountData,
        dashboardCards,
        siteItems ->
        val nonNullSiteInfoHeaderCard =
            siteInfoHeaderCard ?: return@merge buildNoSiteState(accountData?.url, accountData?.name)
        val headerList = listOfNotNull(nonNullSiteInfoHeaderCard, applicationPAsswordModel)
        return@merge if (!dashboardCards.isNullOrEmpty<MySiteCardAndItem>())
            SiteSelected(dashboardData = headerList + dashboardCards)
        else if (!siteItems.isNullOrEmpty<MySiteCardAndItem>())
            SiteSelected(dashboardData = headerList + siteItems)
        else
            SiteSelected(dashboardData = headerList)
    }.distinctUntilChanged()

    init {
        dispatcher.register(this)
        siteInfoHeaderCardViewModelSlice.initialize(viewModelScope)
        applicationPasswordViewModelSlice.initialize(viewModelScope)
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

    fun handleTakenSiteIcon(iconUrl: String, source: MediaPickerActivity.MediaPickerMediaSource?) {
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
        dispatcher.unregister(this)
        siteInfoHeaderCardViewModelSlice.onCleared()
        dashboardCardsViewModelSlice.onCleared()
        dashboardItemsViewModelSlice.onCleared()
        accountDataViewModelSlice.onCleared()
        gutenbergKitWarmupHelper.clearWarmupState()
        super.onCleared()
    }

    fun onSitePicked() {
        selectedSiteRepository.getSelectedSite()?.let {
            onSitePicked(it)
        } ?: run {
            accountDataViewModelSlice.onResume()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun performFirstStepAfterSiteCreation(
        isSiteTitleTaskCompleted: Boolean,
        isNewSite: Boolean
    ) {
        checkAndStartLandOnTheEditor(isNewSite)
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

    private fun buildDashboardOrSiteItems(site: SiteModel) {
        siteInfoHeaderCardViewModelSlice.buildCard(site)
        applicationPasswordViewModelSlice.buildCard(site)
        if (shouldShowDashboard(site)) {
            dashboardCardsViewModelSlice.buildCards(site)
            dashboardItemsViewModelSlice.clearValue()
        } else {
            dashboardItemsViewModelSlice.buildItems(site)
            dashboardCardsViewModelSlice.clearValue()
        }
        // Trigger GutenbergView warmup for the selected site
        gutenbergKitWarmupHelper.warmupIfNeeded(site, viewModelScope)
    }

    private fun onSitePicked(site: SiteModel) {
        siteInfoHeaderCardViewModelSlice.buildCard(site)
        applicationPasswordViewModelSlice.buildCard(site)
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

    fun onBloggingPromptsAttributionClicked() {
        _onNavigation.value = Event(
            SiteNavigationAction.OpenExternalUrl(DAY_ONE_EXTERNAL_URL)
        )
    }

    fun onApplicationPasswordCreated(site: SiteModel) {
        // Hide the Application Password creation card
        applicationPasswordViewModelSlice.uiModelMutable.postValue(null)
        _onSnackbarMessage.postValue(
            Event(
                SnackbarMessageHolder(
                    UiString.UiStringResWithParams(
                        R.string.application_password_credentials_stored,
                        UiString.UiStringText(site.url)
                    )
                )
            )
        )
    }

    fun onApplicationPasswordCreationError(site: SiteModel) {
        _onSnackbarMessage.postValue(
            Event(
                SnackbarMessageHolder(
                    UiString.UiStringResWithParams(
                        R.string.application_password_credentials_storing_error,
                        UiString.UiStringText(site.url)
                    )
                )
            )
        )
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
        const val SITE_NAME_CHANGE_CALLBACK_ID = 1
        const val HIDE_WP_ADMIN_GMT_TIME_ZONE = "GMT"
        private const val DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY = 500L
        private const val DAY_ONE_EXTERNAL_URL = "https://dayoneapp.com/?utm_source=jetpack&utm_medium=prompts"
    }
}
