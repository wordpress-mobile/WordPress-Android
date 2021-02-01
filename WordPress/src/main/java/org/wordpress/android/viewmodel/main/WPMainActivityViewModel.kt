package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.PUBLISH_POST
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.mysite.QuickStartRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.SiteUtils.hasFullAccessToContent
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.MySiteImprovementsFeatureConfig
import org.wordpress.android.util.config.WPStoriesFeatureConfig
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class WPMainActivityViewModel @Inject constructor(
    private val featureAnnouncementProvider: FeatureAnnouncementProvider,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val wpStoriesFeatureConfig: WPStoriesFeatureConfig,
    private val mySiteImprovementsFeatureConfig: MySiteImprovementsFeatureConfig,
    private val quickStartRepository: QuickStartRepository,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _fabUiState = MutableLiveData<MainFabUiState>()
    val fabUiState: LiveData<MainFabUiState> = _fabUiState

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

    private val _startLoginFlow = MutableLiveData<Event<Boolean>>()
    val startLoginFlow: LiveData<Event<Boolean>> = _startLoginFlow

    private val _onFeatureAnnouncementRequested = SingleLiveEvent<Unit>()
    val onFeatureAnnouncementRequested: LiveData<Unit> = _onFeatureAnnouncementRequested

    private val _completeBottomSheetQuickStartTask = SingleLiveEvent<Unit>()
    val completeBottomSheetQuickStartTask: LiveData<Unit> = _completeBottomSheetQuickStartTask

    fun start(site: SiteModel?) {
        if (isStarted) return
        isStarted = true

        setMainFabUiState(false, site)

        loadMainActions(site)

        updateFeatureAnnouncements()
    }

    private fun loadMainActions(site: SiteModel?) {
        val actionsList = ArrayList<MainActionListItem>()

        actionsList.add(CreateAction(
                actionType = NO_ACTION,
                iconRes = 0,
                labelRes = R.string.my_site_bottom_sheet_title,
                onClickAction = null
        ))
        actionsList.add(CreateAction(
                actionType = CREATE_NEW_POST,
                iconRes = R.drawable.ic_posts_white_24dp,
                labelRes = R.string.my_site_bottom_sheet_add_post,
                onClickAction = ::onCreateActionClicked
        ))
        if (hasFullAccessToContent(site)) {
            actionsList.add(CreateAction(
                    actionType = CREATE_NEW_PAGE,
                    iconRes = R.drawable.ic_pages_white_24dp,
                    labelRes = R.string.my_site_bottom_sheet_add_page,
                    onClickAction = ::onCreateActionClicked
            ))
        }
        if (shouldShowStories(site)) {
            actionsList.add(CreateAction(
                    actionType = CREATE_NEW_STORY,
                    iconRes = R.drawable.ic_story_icon_24dp,
                    labelRes = R.string.my_site_bottom_sheet_add_story,
                    onClickAction = ::onCreateActionClicked
            ))
        }

        _mainActions.postValue(actionsList)
    }

    private fun onCreateActionClicked(actionType: ActionType) {
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)

        _showQuickStarInBottomSheet.value?.let { showQuickStart ->
            if (showQuickStart) {
                if (actionType == CREATE_NEW_POST) {
                    if (mySiteImprovementsFeatureConfig.isEnabled()) {
                        quickStartRepository.completeTask(PUBLISH_POST)
                    } else {
                        _completeBottomSheetQuickStartTask.call()
                    }
                }
                _showQuickStarInBottomSheet.postValue(false)
            }
        }
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

    fun onFabClicked(site: SiteModel?, shouldShowQuickStartFocusPoint: Boolean = false) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)
        setMainFabUiState(true, site)

        val quickStartFromImprovedMySiteFragment = mySiteImprovementsFeatureConfig.isEnabled() &&
                quickStartRepository.shouldShowPublishPostQuickStartTask()
        _showQuickStarInBottomSheet.postValue(shouldShowQuickStartFocusPoint || quickStartFromImprovedMySiteFragment)

        if (shouldShowStories(site)) {
            loadMainActions(site)
            _isBottomSheetShowing.value = Event(true)
        } else {
            // NOTE: this whole piece of code and comment below to be removed when we remove the feature flag.
            // Also note: This comment below and code as is is in `develop` at the time of writing the feature
            // flag, so bringing it all back in. See https://github.com/wordpress-mobile/WordPress-Android/pull/11930
            // ----------------------
            // Currently this bottom sheet has only 2 options.
            // We should evaluate to re-introduce the bottom sheet also for users without full access to content
            // if user has at least 2 options (eventually filtering the content not accessible like pages in this case)
            // See p5T066-1cA-p2/#comment-4463
            if (hasFullAccessToContent(site)) {
                // reload main actions given the first time this is initialized, the SiteModel may not contain the
                // latest info
                loadMainActions(site)
                _isBottomSheetShowing.value = Event(true)
            } else {
                _createAction.postValue(CREATE_NEW_POST)
            }
        }
    }

    fun onPageChanged(showFab: Boolean, site: SiteModel?) {
        setMainFabUiState(showFab, site)
    }

    fun onTooltipTapped(site: SiteModel?) {
        disableTooltip(site)
    }

    fun onFabLongPressed(site: SiteModel?) {
        disableTooltip(site)
    }

    fun onOpenLoginPage() {
        _startLoginFlow.value = Event(true)
    }

    fun onResume(site: SiteModel?, showFab: Boolean) {
        setMainFabUiState(showFab, site)

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

    private fun setMainFabUiState(isFabVisible: Boolean, site: SiteModel?) {
        val newState = MainFabUiState(
                isFabVisible = isFabVisible,
                isFabTooltipVisible = if (appPrefsWrapper.isMainFabTooltipDisabled()) false else isFabVisible,
                CreateContentMessageId = getCreateContentMessageId(site)
        )

        _fabUiState.value = newState
    }

    fun getCreateContentMessageId(site: SiteModel?): Int {
        return if (shouldShowStories(site))
            getCreateContentMessageId_StoriesFlagOn(hasFullAccessToContent(site))
        else
            getCreateContentMessageId_StoriesFlagOff(hasFullAccessToContent(site))
    }

    // create_post_page_fab_tooltip_stories_feature_flag_on
    private fun getCreateContentMessageId_StoriesFlagOn(hasFullAccessToContent: Boolean): Int {
        return if (hasFullAccessToContent)
            R.string.create_post_page_fab_tooltip_stories_enabled
        else
            R.string.create_post_page_fab_tooltip_contributors_stories_enabled
    }

    private fun getCreateContentMessageId_StoriesFlagOff(hasFullAccessToContent: Boolean): Int {
        return if (hasFullAccessToContent)
            R.string.create_post_page_fab_tooltip
        else
            R.string.create_post_page_fab_tooltip_contributors
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

    private fun shouldShowStories(site: SiteModel?): Boolean {
        return wpStoriesFeatureConfig.isEnabled() && SiteUtils.supportsStoriesFeature(site)
    }
}
