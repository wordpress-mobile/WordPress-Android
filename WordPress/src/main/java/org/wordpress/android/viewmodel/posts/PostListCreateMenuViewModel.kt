package org.wordpress.android.viewmodel.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.WPStoriesFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.Locale
import javax.inject.Inject

class PostListCreateMenuViewModel @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val wpStoriesFeatureConfig: WPStoriesFeatureConfig
) : ViewModel() {
    private var isStarted = false
    private lateinit var site: SiteModel

    private val _fabUiState = MutableLiveData<MainFabUiState>()
    val fabUiState: LiveData<MainFabUiState> = _fabUiState

    private val _mainActions = MutableLiveData<List<MainActionListItem>>()
    val mainActions: LiveData<List<MainActionListItem>> = _mainActions

    private val _createAction = SingleLiveEvent<ActionType>()
    val createAction: LiveData<ActionType> = _createAction

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    fun start(site: SiteModel) {
        if (isStarted) return
        isStarted = true

        this.site = site

        setMainFabUiState()

        loadMainActions()
    }

    private fun loadMainActions() {
        val actionsList = ArrayList<MainActionListItem>()

        actionsList.add(
                CreateAction(
                        actionType = NO_ACTION,
                        iconRes = 0,
                        labelRes = string.my_site_bottom_sheet_title,
                        onClickAction = null
                )
        )
        actionsList.add(
                CreateAction(
                        actionType = CREATE_NEW_POST,
                        iconRes = drawable.ic_posts_white_24dp,
                        labelRes = string.my_site_bottom_sheet_add_post,
                        onClickAction = ::onCreateActionClicked
                )
        )

        actionsList.add(
                CreateAction(
                        actionType = CREATE_NEW_STORY,
                        iconRes = drawable.ic_story_icon_24dp,
                        labelRes = string.my_site_bottom_sheet_add_story,
                        onClickAction = ::onCreateActionClicked

                )
        )

        _mainActions.postValue(actionsList)
    }

    private fun onCreateActionClicked(actionType: ActionType) {
        val properties = mapOf("action" to actionType.name.toLowerCase(Locale.ROOT))
        analyticsTracker.track(Stat.POST_LIST_CREATE_SHEET_ACTION_TAPPED, properties)
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)
    }

    private fun setMainFabUiState() {
        val newState = MainFabUiState(
                isFabVisible = true,
                isFabTooltipVisible = !appPrefsWrapper.isPostListFabTooltipDisabled(),
                CreateContentMessageId = getCreateContentMessageId()
        )

        _fabUiState.value = newState
    }

    fun onFabClicked() {
        appPrefsWrapper.setPostListFabTooltipDisabled(true)
        setMainFabUiState()
        analyticsTracker.track(Stat.POST_LIST_CREATE_SHEET_SHOWN)
        _isBottomSheetShowing.value = Event(true)
    }

    fun onTooltipTapped() {
        disableTooltip()
    }

    fun onFabLongPressed() {
        disableTooltip()
    }

    private fun disableTooltip() {
        appPrefsWrapper.setPostListFabTooltipDisabled(true)

        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = false,
                    CreateContentMessageId = getCreateContentMessageId()
            )
        }
    }

    fun onResume() {
        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = it.isFabTooltipVisible,
                    CreateContentMessageId = getCreateContentMessageId()
            )
        }
    }

    private fun getCreateContentMessageId(): Int {
        return if (wpStoriesFeatureConfig.isEnabled() && SiteUtils.supportsStoriesFeature(site)) {
            string.create_post_story_fab_tooltip
        } else {
            string.create_post_fab_tooltip
        }
    }
}
