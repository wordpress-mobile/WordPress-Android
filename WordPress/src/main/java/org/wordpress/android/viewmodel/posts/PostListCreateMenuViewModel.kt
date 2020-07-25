package org.wordpress.android.viewmodel.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class PostListCreateMenuViewModel @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _fabUiState = MutableLiveData<MainFabUiState>()
    val fabUiState: LiveData<MainFabUiState> = _fabUiState

    private val _mainActions = MutableLiveData<List<MainActionListItem>>()
    val mainActions: LiveData<List<MainActionListItem>> = _mainActions

    private val _createAction = SingleLiveEvent<ActionType>()
    val createAction: LiveData<ActionType> = _createAction

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    fun start() {
        if (isStarted) return
        isStarted = true

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
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)
    }

    private fun setMainFabUiState() {
        val newState = MainFabUiState(
                isFabVisible = true,
                isFabTooltipVisible = !appPrefsWrapper.isPostListFabTooltipDisabled(),
                CreateContentMessageId = R.string.create_post_page_fab_tooltip_contributors_stories_feature_flag_on
        )

        _fabUiState.value = newState
    }

    fun onFabClicked() {
        appPrefsWrapper.setPostListFabTooltipDisabled(true)
        setMainFabUiState()
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
                    CreateContentMessageId = R.string.create_post_page_fab_tooltip_contributors_stories_feature_flag_on
            )
        }
    }

    fun onResume(hasFullAccessToContent: Boolean) {
        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = it.isFabTooltipVisible,
                    CreateContentMessageId = R.string.create_post_page_fab_tooltip_contributors_stories_feature_flag_on
            )
        }
    }
}
