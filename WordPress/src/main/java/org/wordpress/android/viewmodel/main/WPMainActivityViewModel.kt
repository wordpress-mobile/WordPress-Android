package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class WPMainActivityViewModel @Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) : ViewModel() {
    private var isStarted = false

    private val _showMainActionFab = MutableLiveData<MainFabUiState>()
    val showMainActionFab: LiveData<MainFabUiState> = _showMainActionFab

    private val _mainActions = MutableLiveData<List<MainActionListItem>>()
    val mainActions: LiveData<List<MainActionListItem>> = _mainActions

    private val _createAction = SingleLiveEvent<ActionType>()
    val createAction: LiveData<ActionType> = _createAction

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    private var mainFabTooltipDisabled = true

    fun start(isFabVisible: Boolean) {
        if (isStarted) return
        isStarted = true

        mainFabTooltipDisabled = !BuildConfig.INFORMATION_ARCHITECTURE_AVAILABLE ||
                appPrefsWrapper.isMainFabTooltipDisabled()
        setMainFabUiState(isFabVisible)

        loadMainActions()
    }

    private fun loadMainActions() {
        val actionsList = ArrayList<MainActionListItem>()

        actionsList.add(CreateAction(
                actionType = CREATE_NEW_POST,
                iconRes = R.drawable.ic_posts_white_24dp,
                labelRes = R.string.my_site_bottom_sheet_add_post,
                onClickAction = ::onCreateActionClicked
        ))
        actionsList.add(CreateAction(
                actionType = CREATE_NEW_PAGE,
                iconRes = R.drawable.ic_pages_white_24dp,
                labelRes = R.string.my_site_bottom_sheet_add_page,
                onClickAction = ::onCreateActionClicked
        ))

        _mainActions.postValue(actionsList)
    }

    private fun onCreateActionClicked(actionType: ActionType) {
        _isBottomSheetShowing.postValue(Event(false))
        _createAction.postValue(actionType)
    }

    fun setIsBottomSheetShowing(showing: Boolean) {
        if (!mainFabTooltipDisabled) {
            mainFabTooltipDisabled = true
            appPrefsWrapper.setMainFabTooltipDisabled(mainFabTooltipDisabled)
            setMainFabUiState(true)
        }
        _isBottomSheetShowing.value = Event(showing)
    }

    fun onPageChanged(showFab: Boolean) {
        setMainFabUiState(showFab)
    }

    private fun setMainFabUiState(isFabVisible: Boolean) {
        val newState = MainFabUiState(
                        isFabVisible = isFabVisible,
                        isFabTooltipVisible = if (mainFabTooltipDisabled) false else isFabVisible
        )

        _showMainActionFab.value = newState
    }
}
