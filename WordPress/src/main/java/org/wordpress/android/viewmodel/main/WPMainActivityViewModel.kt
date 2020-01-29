package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.ui.main.MainActionListItem
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.NO_ACTION
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.main.MainFabUiState
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class WPMainActivityViewModel @Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) : ViewModel() {
    private var isStarted = false

    private val _fabUiState = MutableLiveData<MainFabUiState>()
    val fabUiState: LiveData<MainFabUiState> = _fabUiState

    private val _mainActions = MutableLiveData<List<MainActionListItem>>()
    val mainActions: LiveData<List<MainActionListItem>> = _mainActions

    private val _createAction = SingleLiveEvent<ActionType>()
    val createAction: LiveData<ActionType> = _createAction

    private val _isBottomSheetShowing = MutableLiveData<Event<Boolean>>()
    val isBottomSheetShowing: LiveData<Event<Boolean>> = _isBottomSheetShowing

    fun start(isFabVisible: Boolean) {
        if (isStarted) return
        isStarted = true

        setMainFabUiState(isFabVisible)

        loadMainActions()
    }

    private fun loadMainActions() {
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

    private fun disableTooltip() {
        appPrefsWrapper.setMainFabTooltipDisabled(true)

        val oldState = _fabUiState.value
        oldState?.let {
            _fabUiState.value = MainFabUiState(
                    isFabVisible = it.isFabVisible,
                    isFabTooltipVisible = false
            )
        }
    }

    fun setIsBottomSheetShowing(showing: Boolean) {
        appPrefsWrapper.setMainFabTooltipDisabled(true)
        setMainFabUiState(true)

        _isBottomSheetShowing.value = Event(showing)
    }

    fun onPageChanged(showFab: Boolean) {
        setMainFabUiState(showFab)
    }

    fun onTooltipTapped() {
        disableTooltip()
    }

    fun onFabLongPressed() {
        disableTooltip()
    }

    private fun setMainFabUiState(isFabVisible: Boolean) {
        val newState = MainFabUiState(
                        isFabVisible = isFabVisible,
                        isFabTooltipVisible = if (appPrefsWrapper.isMainFabTooltipDisabled()) false else isFabVisible
        )

        _fabUiState.value = newState
    }
}
