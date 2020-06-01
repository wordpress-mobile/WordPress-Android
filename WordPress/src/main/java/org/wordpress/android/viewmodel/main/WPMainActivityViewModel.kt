package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class WPMainActivityViewModel @Inject constructor(
    private val featureAnnouncementProvider: FeatureAnnouncementProvider,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {
    private var isStarted = false

    private val _fabUiState = MutableLiveData<MainFabUiState>()
    val fabUiState: LiveData<MainFabUiState> = _fabUiState

    private val _mainActions = MutableLiveData<List<MainActionListItem>>()
    val mainActions: LiveData<List<MainActionListItem>> = _mainActions

    private val _createAction = SingleLiveEvent<ActionType>()
    val createAction: LiveData<ActionType> = _createAction

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    private val _startLoginFlow = MutableLiveData<Event<Boolean>>()
    val startLoginFlow: LiveData<Event<Boolean>> = _startLoginFlow

    private val _onFeatureAnnouncementRequested = SingleLiveEvent<Unit>()
    val onFeatureAnnouncementRequested: LiveData<Unit> = _onFeatureAnnouncementRequested

    fun start(isFabVisible: Boolean, hasFullAccessToContent: Boolean) {
        if (isStarted) return
        isStarted = true

        setMainFabUiState(isFabVisible, hasFullAccessToContent)

        loadMainActions()

        checkForFeatureAnnouncements()
    }

    private fun loadMainActions() {
        val actionsList = ArrayList<MainActionListItem>()

        actionsList.add(
                CreateAction(
                        actionType = NO_ACTION,
                        iconRes = 0,
                        labelRes = R.string.my_site_bottom_sheet_title,
                        onClickAction = null
                )
        )
        actionsList.add(
                CreateAction(
                        actionType = CREATE_NEW_POST,
                        iconRes = R.drawable.ic_posts_white_24dp,
                        labelRes = R.string.my_site_bottom_sheet_add_post,
                        onClickAction = ::onCreateActionClicked
                )
        )
        actionsList.add(
                CreateAction(
                        actionType = CREATE_NEW_PAGE,
                        iconRes = R.drawable.ic_pages_white_24dp,
                        labelRes = R.string.my_site_bottom_sheet_add_page,
                        onClickAction = ::onCreateActionClicked
                )
        )

        _mainActions.postValue(actionsList)
    }

    private fun onCreateActionClicked(actionType: ActionType) {
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)
    }

    private fun disableTooltip(hasFullAccessToContent: Boolean) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)

        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = false,
                    CreateContentMessageId = getCreateContentMessageId(hasFullAccessToContent)
            )
        }
    }

    fun onFabClicked(hasFullAccessToContent: Boolean) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)
        setMainFabUiState(true, hasFullAccessToContent)

        // Currently this bottom sheet has only 2 options.
        // We should evaluate to re-introduce the bottom sheet also for users without full access to content
        // if user has at least 2 options (eventually filtering the content not accessible like pages in this case)
        // See p5T066-1cA-p2/#comment-4463
        if (hasFullAccessToContent) {
            _isBottomSheetShowing.value = Event(true)
        } else {
            _createAction.postValue(CREATE_NEW_POST)
        }
    }

    fun onPageChanged(showFab: Boolean, hasFullAccessToContent: Boolean) {
        setMainFabUiState(showFab, hasFullAccessToContent)
    }

    fun onTooltipTapped(hasFullAccessToContent: Boolean) {
        disableTooltip(hasFullAccessToContent)
    }

    fun onFabLongPressed(hasFullAccessToContent: Boolean) {
        disableTooltip(hasFullAccessToContent)
    }

    fun onOpenLoginPage() {
        _startLoginFlow.value = Event(true)
    }

    fun onResume(hasFullAccessToContent: Boolean) {
        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = it.isFabTooltipVisible,
                    CreateContentMessageId = getCreateContentMessageId(hasFullAccessToContent)
            )
        }
    }

    private fun setMainFabUiState(isFabVisible: Boolean, hasFullAccessToContent: Boolean) {
        val newState = MainFabUiState(
                isFabVisible = isFabVisible,
                isFabTooltipVisible = if (appPrefsWrapper.isMainFabTooltipDisabled()) false else isFabVisible,
                CreateContentMessageId = getCreateContentMessageId(hasFullAccessToContent)
        )

        _fabUiState.value = newState
    }

    private fun getCreateContentMessageId(hasFullAccessToContent: Boolean): Int {
        return if (hasFullAccessToContent)
            R.string.create_post_page_fab_tooltip
        else
            R.string.create_post_page_fab_tooltip_contributors
    }

    private fun checkForFeatureAnnouncements() {
        val currentVersionCode = buildConfigWrapper.getAppVersionCode()
        val previousVersionCode = appPrefsWrapper.lastFeatureAnnouncementAppVersionCode

        // only proceed to feature announcement logic if we are upgrading the app
        if (previousVersionCode != 0 && previousVersionCode < currentVersionCode) {
            if (canShowFeatureAnnouncement()) {
                analyticsTracker.track(Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE)
                _onFeatureAnnouncementRequested.call()
            }
//          else {
//              // request feature announcement from endpoint to be used on next app start
//          }
        } else {
            appPrefsWrapper.lastFeatureAnnouncementAppVersionCode = currentVersionCode
        }
    }

    private fun canShowFeatureAnnouncement(): Boolean {
        return featureAnnouncementProvider.isAnnouncementOnUpgradeAvailable() &&
                appPrefsWrapper.featureAnnouncementShownVersion <
                featureAnnouncementProvider.getLatestFeatureAnnouncement()?.announcementVersion!!
    }
}
