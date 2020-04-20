package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction.AskForSiteSelection
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction.ContinueReblogTo
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogAction.UpdateMenuState
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogActionType.ASK_FOR_SITE_SELECTION
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogActionType.CONTINUE_REBLOG_FLOW
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ReblogActionType.UPDATE_MENU_STATE
import javax.inject.Inject

class SitePickerViewModel @Inject constructor() : ViewModel() {
    private val _onReblogActionTriggered = MutableLiveData<Event<ReblogAction>>()
    val onReblogActionTriggered: LiveData<Event<ReblogAction>> = _onReblogActionTriggered

    private var siteForReblog: SiteRecord? = null

    enum class ReblogActionType {
        UPDATE_MENU_STATE,
        CONTINUE_REBLOG_FLOW,
        ASK_FOR_SITE_SELECTION
    }

    sealed class ReblogAction(val actionType: ReblogActionType) {
        object UpdateMenuState : ReblogAction(UPDATE_MENU_STATE)

        data class ContinueReblogTo(val siteForReblog: SiteRecord?) : ReblogAction(CONTINUE_REBLOG_FLOW)

        object AskForSiteSelection : ReblogAction(ASK_FOR_SITE_SELECTION)
    }

    fun onSiteForReblogSelected(siteRecord: SiteRecord?) {
        siteForReblog = siteRecord
        _onReblogActionTriggered.value = Event(UpdateMenuState)
    }

    fun onContinueFlowSelected() {
        _onReblogActionTriggered.value = Event(
                if (siteForReblog != null)
                    ContinueReblogTo(siteForReblog)
                else
                    AskForSiteSelection
        )
    }

    fun isReblogSiteSelected() = siteForReblog != null
}
