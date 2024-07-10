@file:Suppress("MaximumLineLength")

package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartExistingSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask.PUBLISH_POST
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.debug.preferences.DebugPrefs
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.ANSWER_BLOGGING_PROMPT
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE_FROM_PAGES_CARD
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.main.analytics.MainCreateSheetTracker
import org.wordpress.android.ui.main.utils.MainCreateSheetHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.privacy.banner.domain.ShouldAskPrivacyConsent
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.SiteUtils.hasFullAccessToContent
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.mapNullable
import org.wordpress.android.util.mapSafe
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.io.Serializable
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

private const val ONE_SITE = 1

class WPMainActivityViewModel @Inject constructor(
    private val featureAnnouncementProvider: FeatureAnnouncementProvider,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val bloggingPromptsStore: BloggingPromptsStore,
    private val shouldAskPrivacyConsent: ShouldAskPrivacyConsent,
    private val mainCreateSheetHelper: MainCreateSheetHelper,
    private val mainCreateSheetTracker: MainCreateSheetTracker,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _fabUiState = MutableLiveData<MainFabUiState>()
    val fabUiState: LiveData<MainFabUiState> = merge(
        _fabUiState,
        quickStartRepository.activeTask
    ) { fabUiState, activeTask ->
        val isFocusPointVisible = activeTask == PUBLISH_POST && fabUiState?.isFabVisible == true
        if (isFocusPointVisible != fabUiState?.isFocusPointVisible) {
            fabUiState?.copy(isFocusPointVisible = isFocusPointVisible)
        } else {
            fabUiState
        }
    }

    private val _showQuickStarInBottomSheet = MutableLiveData<Boolean>()

    private val _mainActions = MutableLiveData<List<MainActionListItem>>()
    val mainActions: LiveData<List<MainActionListItem>> = merge(
        _mainActions,
        _showQuickStarInBottomSheet
    ) { mainActions, showQuickStart ->
        if (showQuickStart != null && mainActions != null) {
            mainActions.map {
                if (it is CreateAction && it.actionType == CREATE_NEW_POST) it.copy(
                    showQuickStartFocusPoint = showQuickStart
                ) else it
            }
        } else {
            mainActions
        }
    }
    private val _createAction = SingleLiveEvent<ActionType>()
    val createAction: LiveData<ActionType> = _createAction

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    private val _switchToMeTab = MutableLiveData<Event<Unit>>()
    val switchToMeTab: LiveData<Event<Unit>> = _switchToMeTab

    private val _onFeatureAnnouncementRequested = SingleLiveEvent<Unit?>()
    val onFeatureAnnouncementRequested: LiveData<Unit?> = _onFeatureAnnouncementRequested

    private val _createPostWithBloggingPrompt = SingleLiveEvent<Int>()
    val createPostWithBloggingPrompt: LiveData<Int> = _createPostWithBloggingPrompt

    private val _openBloggingPromptsOnboarding = SingleLiveEvent<Unit?>()
    val openBloggingPromptsOnboarding: LiveData<Unit?> = _openBloggingPromptsOnboarding

    private val _askForPrivacyConsent = SingleLiveEvent<Unit>()
    val askForPrivacyConsent: LiveData<Unit> = _askForPrivacyConsent

    private val _showPrivacySettings = SingleLiveEvent<Unit>()
    val showPrivacySettings: LiveData<Unit> = _showPrivacySettings

    private val _showPrivacySettingsWithError = SingleLiveEvent<Boolean?>()
    val showPrivacySettingsWithError: LiveData<Boolean?> = _showPrivacySettingsWithError

    private val _mySiteDashboardRefreshRequested = MutableLiveData<Event<Unit>>()
    val mySiteDashboardRefreshRequested: LiveData<Event<Unit>> = _mySiteDashboardRefreshRequested

    val onFocusPointVisibilityChange = quickStartRepository.activeTask
        .mapNullable { getExternalFocusPointInfo(it) }
        .distinctUntilChanged()
        .mapSafe { Event(it) } as LiveData<Event<List<FocusPointInfo>>>

    val hasMultipleSites: Boolean
        get() = siteStore.sitesCount > ONE_SITE

    val firstSite: SiteModel?
        get() = if (siteStore.hasSite()) {
            siteStore.sites[0]
        } else null

    val isSignedInWPComOrHasWPOrgSite: Boolean
        get() = FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore)

    fun start(site: SiteModel?, page: PageType) {
        if (isStarted) return
        isStarted = true

        launch {
            if (shouldAskPrivacyConsent()) {
                _askForPrivacyConsent.call()
            }
        }

        setMainFabUiState(false, site, page)

        launch { loadMainActions(site, page) }

        updateFeatureAnnouncements()
    }

    @Suppress("LongMethod")
    private suspend fun loadMainActions(site: SiteModel?, page: PageType, onFabClicked: Boolean = false) {
        val actionsList = ArrayList<MainActionListItem>()
        if (mainCreateSheetHelper.canCreatePromptAnswer()) {
            val prompt = site?.let {
                bloggingPromptsStore.getPromptForDate(it, Date()).firstOrNull()?.model
            }

            prompt?.let {
                actionsList.add(
                    AnswerBloggingPromptAction(
                        actionType = ANSWER_BLOGGING_PROMPT,
                        promptTitle = UiStringText(it.text),
                        isAnswered = prompt.isAnswered,
                        promptId = prompt.id,
                        attribution = BloggingPromptAttribution.fromPrompt(prompt),
                        onClickAction = { prompt, attribution ->
                            onAnswerPromptActionClicked(
                                prompt,
                                attribution,
                                page
                            )
                        },
                        onHelpAction = { onHelpPromptActionClicked(page) }
                    )
                )
            }
        }

        actionsList.add(
            CreateAction(
                actionType = NO_ACTION,
                iconRes = 0,
                labelRes = R.string.my_site_bottom_sheet_title,
                onClickAction = null
            )
        )

        if (mainCreateSheetHelper.canCreatePost()) {
            actionsList.add(
                CreateAction(
                    actionType = CREATE_NEW_POST,
                    iconRes = R.drawable.ic_posts_white_24dp,
                    labelRes = R.string.my_site_bottom_sheet_add_post,
                    onClickAction = { onCreateActionClicked(it, page) }
                )
            )
        }

        if (mainCreateSheetHelper.canCreatePostFromAudio(site)) {
            actionsList.add(
                CreateAction(
                    actionType = ActionType.CREATE_NEW_POST_FROM_AUDIO,
                    iconRes = R.drawable.ic_mic_white_24dp,
                    labelRes = R.string.my_site_bottom_sheet_add_post_from_audio,
                    onClickAction = { onCreateActionClicked(it, page) }
                )
            )
        }

        if (mainCreateSheetHelper.canCreatePage(site, page)) {
            actionsList.add(
                CreateAction(
                    actionType = CREATE_NEW_PAGE,
                    iconRes = R.drawable.ic_pages_white_24dp,
                    labelRes = R.string.my_site_bottom_sheet_add_page,
                    onClickAction = { onCreateActionClicked(it, page) }
                )
            )
        }

        _mainActions.postValue(actionsList)
        if (onFabClicked) mainCreateSheetTracker.trackCreateActionsSheetCard(actionsList)
    }

    private fun onCreateActionClicked(actionType: ActionType, page: PageType) {
        mainCreateSheetTracker.trackActionTapped(page, actionType)
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)

        _showQuickStarInBottomSheet.value?.let { showQuickStart ->
            if (showQuickStart) {
                if (actionType == CREATE_NEW_POST) quickStartRepository.completeTask(PUBLISH_POST)
                _showQuickStarInBottomSheet.postValue(false)
            }
        }
    }

    private fun onAnswerPromptActionClicked(promptId: Int, attribution: BloggingPromptAttribution, page: PageType) {
        mainCreateSheetTracker.trackAnswerPromptActionTapped(page, attribution)
        _isBottomSheetShowing.postValue(Event(false))
        _createPostWithBloggingPrompt.postValue(promptId)
    }

    private fun onHelpPromptActionClicked(page: PageType) {
        mainCreateSheetTracker.trackHelpPromptActionTapped(page)
        _openBloggingPromptsOnboarding.call()
    }

    fun onFabClicked(site: SiteModel?, page: PageType) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)

        _showQuickStarInBottomSheet.postValue(quickStartRepository.activeTask.value == PUBLISH_POST)

        if (hasFullAccessToContent(site)) {
            launch {
                // The user has at least two create options available for this site (pages and/or story posts),
                // so we should show a bottom sheet.
                // Creation options added in the future should also be weighed here.

                // Reload main actions, since the first time this is initialized the SiteModel may not contain the
                // latest info.
                loadMainActions(site, page, onFabClicked = true)

                mainCreateSheetTracker.trackSheetShown(page)
                _isBottomSheetShowing.postValue(Event(true))
            }
        } else {
            // User only has one option - creating a post. Skip the bottom sheet and go straight to that action.
            _createAction.postValue(CREATE_NEW_POST)
        }
    }

    fun onPageChanged(site: SiteModel?, hasValidSite: Boolean, page: PageType) {
        val showFab = hasValidSite && mainCreateSheetHelper.shouldShowFabForPage(page)
        setMainFabUiState(showFab, site, page)
    }

    fun onOpenLoginPage() = launch {
        _switchToMeTab.value = Event(Unit)
    }

    fun onResume(site: SiteModel?, hasValidSite: Boolean, page: PageType?) {
        val showFab = hasValidSite && mainCreateSheetHelper.shouldShowFabForPage(page)
        setMainFabUiState(showFab, site, page)

        checkAndShowFeatureAnnouncement()
    }

    private fun checkAndShowFeatureAnnouncement() {
        if (buildConfigWrapper.isWhatsNewFeatureEnabled) {
            launch {
                val currentVersionCode = buildConfigWrapper.getAppVersionCode()
                val previousVersionCode = appPrefsWrapper.lastFeatureAnnouncementAppVersionCode
                val alwaysShowAnnouncement = appPrefsWrapper.getDebugBooleanPref(
                    DebugPrefs.ALWAYS_SHOW_ANNOUNCEMENT.key
                )

                // only proceed to feature announcement logic if we are upgrading the app
                if (alwaysShowAnnouncement || previousVersionCode != 0 && previousVersionCode < currentVersionCode) {
                    if (canShowFeatureAnnouncement()) {
                        analyticsTracker.track(Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE)
                        _onFeatureAnnouncementRequested.call()
                    }
                } else {
                    appPrefsWrapper.lastFeatureAnnouncementAppVersionCode = currentVersionCode
                }
            }
        }
    }

    private fun setMainFabUiState(isFabVisible: Boolean, site: SiteModel?, page: PageType?) {
        if (isFabVisible && page != null) mainCreateSheetTracker.trackFabShown(page)

        val newState = MainFabUiState(
            isFabVisible = isFabVisible,
            isFabTooltipVisible = if (appPrefsWrapper.isMainFabTooltipDisabled()) false else isFabVisible,
            CreateContentMessageId = getCreateContentMessageId(site, page)
        )

        _fabUiState.value = newState
    }

    fun getCreateContentMessageId(site: SiteModel?, page: PageType?): Int =
        if (mainCreateSheetHelper.canCreatePage(site, page)) {
            R.string.create_post_page_fab_tooltip
        } else {
            R.string.create_post_page_fab_tooltip_contributors
        }


    private fun updateFeatureAnnouncements() {
        launch {
            featureAnnouncementProvider.getLatestFeatureAnnouncement(false)
        }
    }

    private suspend fun canShowFeatureAnnouncement(): Boolean {
        val cachedAnnouncement = featureAnnouncementProvider.getLatestFeatureAnnouncement(true)
        val alwaysShowAnnouncement = appPrefsWrapper.getDebugBooleanPref(DebugPrefs.ALWAYS_SHOW_ANNOUNCEMENT.key)
        return cachedAnnouncement != null &&
                (alwaysShowAnnouncement ||
                        cachedAnnouncement.canBeDisplayedOnAppUpgrade(buildConfigWrapper.getAppVersionName()) &&
                        appPrefsWrapper.featureAnnouncementShownVersion < cachedAnnouncement.announcementVersion)
    }

    private fun getExternalFocusPointInfo(task: QuickStartTask?): List<FocusPointInfo> {
        val followSiteTask = quickStartRepository.quickStartType
            .getTaskFromString(QuickStartStore.QUICK_START_FOLLOW_SITE_LABEL)
        val followSitesTaskFocusPointInfo = FocusPointInfo(followSiteTask, task == followSiteTask)
        val checkNotifsTaskFocusPointInfo = FocusPointInfo(
            QuickStartExistingSiteTask.CHECK_NOTIFICATIONS,
            task == QuickStartExistingSiteTask.CHECK_NOTIFICATIONS
        )
        return listOf(followSitesTaskFocusPointInfo, checkNotifsTaskFocusPointInfo)
    }

    fun handleSiteRemoved() {
        selectedSiteRepository.removeSite()
    }

    fun triggerCreatePageFlow() {
        _createAction.postValue(CREATE_NEW_PAGE_FROM_PAGES_CARD)
    }

    fun onPrivacySettingsTapped() = launch {
        _showPrivacySettings.call()
    }

    fun onSettingsPrivacyPreferenceUpdateFailed(requestedAnalyticsPreference: Boolean?) {
        _showPrivacySettingsWithError.value = requestedAnalyticsPreference
    }

    fun requestMySiteDashboardRefresh() {
        this._mySiteDashboardRefreshRequested.value = Event(Unit)
    }

    data class FocusPointInfo(
        val task: QuickStartTask,
        val isVisible: Boolean
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
