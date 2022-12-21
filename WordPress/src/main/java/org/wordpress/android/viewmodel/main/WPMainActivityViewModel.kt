@file:Suppress("MaximumLineLength")

package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
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
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.ANSWER_BLOGGING_PROMPT
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.AnswerBloggingPromptAction
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptAttribution
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.SiteUtils.hasFullAccessToContent
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
import org.wordpress.android.util.map
import org.wordpress.android.util.mapNullable
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.io.Serializable
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

private const val SWITCH_TO_MY_SITE_DELAY = 500L
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
    private val bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig,
    private val bloggingPromptsStore: BloggingPromptsStore,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
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

    private val _startLoginFlow = MutableLiveData<Event<Unit>>()
    val startLoginFlow: LiveData<Event<Unit>> = _startLoginFlow

    private val _switchToMySite = MutableLiveData<Event<Unit>>()
    val switchToMySite: LiveData<Event<Unit>> = _switchToMySite

    private val _onFeatureAnnouncementRequested = SingleLiveEvent<Unit>()
    val onFeatureAnnouncementRequested: LiveData<Unit> = _onFeatureAnnouncementRequested

    private val _createPostWithBloggingPrompt = SingleLiveEvent<Int>()
    val createPostWithBloggingPrompt: LiveData<Int> = _createPostWithBloggingPrompt

    private val _openBloggingPromptsOnboarding = SingleLiveEvent<Unit>()
    val openBloggingPromptsOnboarding: LiveData<Unit> = _openBloggingPromptsOnboarding

    val onFocusPointVisibilityChange = quickStartRepository.activeTask
            .mapNullable { getExternalFocusPointInfo(it) }
            .distinctUntilChanged()
            .map { Event(it) } as LiveData<Event<List<FocusPointInfo>>>

    val hasMultipleSites: Boolean
        get() = siteStore.sitesCount > ONE_SITE

    val firstSite: SiteModel?
        get() = if (siteStore.hasSite()) {
            siteStore.sites[0]
        } else null

    val isSignedInWPComOrHasWPOrgSite: Boolean
        get() = FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore)

    fun start(site: SiteModel?) {
        if (isStarted) return
        isStarted = true

        setMainFabUiState(false, site)

        loadMainActions(site)

        updateFeatureAnnouncements()
    }

    @Suppress("LongMethod")
    private fun loadMainActions(site: SiteModel?) = launch {
        val actionsList = ArrayList<MainActionListItem>()
        if (bloggingPromptsFeatureConfig.isEnabled()) {
            val prompt = site?.let {
                if (it.isUsingWpComRestApi) {
                    bloggingPromptsStore.getPromptForDate(it, Date()).firstOrNull()?.model
                } else {
                    null
                }
            }

            prompt?.let {
                actionsList.add(
                        AnswerBloggingPromptAction(
                                actionType = ANSWER_BLOGGING_PROMPT,
                                promptTitle = UiStringText(it.text),
                                isAnswered = prompt.isAnswered,
                                promptId = prompt.id,
                                attribution = BloggingPromptAttribution.fromString(prompt.attribution),
                                onClickAction = ::onAnswerPromptActionClicked,
                                onHelpAction = ::onHelpPrompActionClicked
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
        if (SiteUtils.supportsStoriesFeature(site)) {
            actionsList.add(
                    CreateAction(
                            actionType = CREATE_NEW_STORY,
                            iconRes = R.drawable.ic_story_icon_24dp,
                            labelRes = R.string.my_site_bottom_sheet_add_story,
                            onClickAction = ::onCreateActionClicked
                    )
            )
        }
        actionsList.add(
                CreateAction(
                        actionType = CREATE_NEW_POST,
                        iconRes = R.drawable.ic_posts_white_24dp,
                        labelRes = R.string.my_site_bottom_sheet_add_post,
                        onClickAction = ::onCreateActionClicked
                )
        )
        if (hasFullAccessToContent(site)) {
            actionsList.add(
                    CreateAction(
                            actionType = CREATE_NEW_PAGE,
                            iconRes = R.drawable.ic_pages_white_24dp,
                            labelRes = R.string.my_site_bottom_sheet_add_page,
                            onClickAction = ::onCreateActionClicked
                    )
            )
        }

        _mainActions.postValue(actionsList)
    }

    private fun onCreateActionClicked(actionType: ActionType) {
        val properties = mapOf("action" to actionType.name.lowercase(Locale.ROOT))
        analyticsTracker.track(Stat.MY_SITE_CREATE_SHEET_ACTION_TAPPED, properties)
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)

        _showQuickStarInBottomSheet.value?.let { showQuickStart ->
            if (showQuickStart) {
                if (actionType == CREATE_NEW_POST) quickStartRepository.completeTask(PUBLISH_POST)
                _showQuickStarInBottomSheet.postValue(false)
            }
        }
    }

    private fun onAnswerPromptActionClicked(promptId: Int) {
        analyticsTracker.track(Stat.MY_SITE_CREATE_SHEET_ANSWER_PROMPT_TAPPED)
        _isBottomSheetShowing.postValue(Event(false))
        _createPostWithBloggingPrompt.postValue(promptId)
    }

    private fun onHelpPrompActionClicked() {
        analyticsTracker.track(Stat.MY_SITE_CREATE_SHEET_PROMPT_HELP_TAPPED)
        _openBloggingPromptsOnboarding.call()
    }

    private fun disableTooltip(site: SiteModel?) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)

        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = false,
                    CreateContentMessageId = getCreateContentMessageId(site)
            )
        }
    }

    fun onFabClicked(site: SiteModel?) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)
        setMainFabUiState(true, site)

        _showQuickStarInBottomSheet.postValue(quickStartRepository.activeTask.value == PUBLISH_POST)

        if (SiteUtils.supportsStoriesFeature(site) || hasFullAccessToContent(site)) {
            // The user has at least two create options available for this site (pages and/or story posts),
            // so we should show a bottom sheet.
            // Creation options added in the future should also be weighed here.

            // Reload main actions, since the first time this is initialized the SiteModel may not contain the
            // latest info.
            loadMainActions(site)

            analyticsTracker.track(Stat.MY_SITE_CREATE_SHEET_SHOWN)
            _isBottomSheetShowing.value = Event(true)
        } else {
            // User only has one option - creating a post. Skip the bottom sheet and go straight to that action.
            _createAction.postValue(CREATE_NEW_POST)
        }
    }

    fun onPageChanged(isOnMySitePageWithValidSite: Boolean, site: SiteModel?) {
        val showFab = if (buildConfigWrapper.isCreateFabEnabled) isOnMySitePageWithValidSite else false
        setMainFabUiState(showFab, site)
    }

    fun onTooltipTapped(site: SiteModel?) {
        disableTooltip(site)
    }

    fun onFabLongPressed(site: SiteModel?) {
        disableTooltip(site)
    }

    fun onOpenLoginPage(mySitePosition: Int) = launch {
        _startLoginFlow.value = Event(Unit)
        appPrefsWrapper.setMainPageIndex(mySitePosition)
        delay(SWITCH_TO_MY_SITE_DELAY)
        _switchToMySite.value = Event(Unit)
    }

    fun onResume(site: SiteModel?, isOnMySitePageWithValidSite: Boolean) {
        val showFab = if (buildConfigWrapper.isCreateFabEnabled) isOnMySitePageWithValidSite else false
        setMainFabUiState(showFab, site)

        checkAndShowFeatureAnnouncement()
    }

    private fun checkAndShowFeatureAnnouncement() {
        if (buildConfigWrapper.isWhatsNewFeatureEnabled) {
            launch {
                val currentVersionCode = buildConfigWrapper.getAppVersionCode()
                val previousVersionCode = appPrefsWrapper.lastFeatureAnnouncementAppVersionCode

                // only proceed to feature announcement logic if we are upgrading the app
                if (previousVersionCode != 0 && previousVersionCode < currentVersionCode) {
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

    private fun setMainFabUiState(isFabVisible: Boolean, site: SiteModel?) {
        val newState = MainFabUiState(
                isFabVisible = isFabVisible,
                isFabTooltipVisible = if (appPrefsWrapper.isMainFabTooltipDisabled()) false else isFabVisible,
                CreateContentMessageId = getCreateContentMessageId(site)
        )

        _fabUiState.value = newState
    }

    fun getCreateContentMessageId(site: SiteModel?): Int {
        return if (SiteUtils.supportsStoriesFeature(site)) {
            getCreateContentMessageIdStoriesFlagOn(hasFullAccessToContent(site))
        } else {
            getCreateContentMessageIdStoriesFlagOff(hasFullAccessToContent(site))
        }
    }

    // create_post_page_fab_tooltip_stories_feature_flag_on
    private fun getCreateContentMessageIdStoriesFlagOn(hasFullAccessToContent: Boolean): Int {
        return if (hasFullAccessToContent) {
            R.string.create_post_page_fab_tooltip_stories_enabled
        } else {
            R.string.create_post_page_fab_tooltip_contributors_stories_enabled
        }
    }

    private fun getCreateContentMessageIdStoriesFlagOff(hasFullAccessToContent: Boolean): Int {
        return if (hasFullAccessToContent) {
            R.string.create_post_page_fab_tooltip
        } else {
            R.string.create_post_page_fab_tooltip_contributors
        }
    }

    private fun updateFeatureAnnouncements() {
        launch {
            featureAnnouncementProvider.getLatestFeatureAnnouncement(false)
        }
    }

    private suspend fun canShowFeatureAnnouncement(): Boolean {
        val cachedAnnouncement = featureAnnouncementProvider.getLatestFeatureAnnouncement(true)

        return cachedAnnouncement != null &&
                cachedAnnouncement.canBeDisplayedOnAppUpgrade(buildConfigWrapper.getAppVersionName()) &&
                appPrefsWrapper.featureAnnouncementShownVersion < cachedAnnouncement.announcementVersion
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

    fun onMainBottomSheetCreated() {
        _mainActions.value?.let { actions ->
            if (actions.any { it is AnswerBloggingPromptAction }) {
                analyticsTracker.track(Stat.BLOGGING_PROMPTS_CREATE_SHEET_CARD_VIEWED)
            }
        }
    }

    data class FocusPointInfo(
        val task: QuickStartTask,
        val isVisible: Boolean
    ) : Serializable {
        companion object {
            const val serialVersionUID = 1L
        }
    }
}
