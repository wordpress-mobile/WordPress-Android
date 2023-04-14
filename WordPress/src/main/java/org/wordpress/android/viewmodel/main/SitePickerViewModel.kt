package org.wordpress.android.viewmodel.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.AskForSiteSelection
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.ContinueReblogTo
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.NavigateToState
import org.wordpress.android.viewmodel.main.SitePickerViewModel.Action.ShowJetpackIndividualPluginOverlay
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ActionType.ASK_FOR_SITE_SELECTION
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ActionType.CONTINUE_REBLOG_TO
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ActionType.NAVIGATE_TO_STATE
import org.wordpress.android.viewmodel.main.SitePickerViewModel.ActionType.SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY
import org.wordpress.android.viewmodel.main.SitePickerViewModel.NavigateState.TO_NO_SITE_SELECTED
import org.wordpress.android.viewmodel.main.SitePickerViewModel.NavigateState.TO_SITE_SELECTED
import javax.inject.Inject

class SitePickerViewModel @Inject constructor(
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
) : ViewModel() {
    private val _onActionTriggered = MutableLiveData<Event<Action>>()
    val onActionTriggered: LiveData<Event<Action>> = _onActionTriggered

    private var siteForReblog: SiteRecord? = null

    fun onSiteForReblogSelected(siteRecord: SiteRecord) {
        selectSite(siteRecord)
    }

    fun onContinueFlowSelected() {
        _onActionTriggered.value = Event(
            if (siteForReblog != null)
                ContinueReblogTo(siteForReblog)
            else
                AskForSiteSelection
        )
    }

    fun onReblogActionBackSelected() {
        siteForReblog = null
        _onActionTriggered.value = Event(NavigateToState(TO_NO_SITE_SELECTED))
    }

    fun onRefreshReblogActionMode() {
        siteForReblog?.let {
            selectSite(it)
        }
    }

    private fun selectSite(siteRecord: SiteRecord) {
        siteForReblog = siteRecord
        _onActionTriggered.value = Event(NavigateToState(TO_SITE_SELECTED, siteRecord))
    }

    fun onSiteListLoaded() {
        // don't check if already shown
        if (_onActionTriggered.value?.peekContent() == ShowJetpackIndividualPluginOverlay) return

        viewModelScope.launch {
            val showOverlay = wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()
            if (showOverlay) {
                delay(DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
                _onActionTriggered.postValue(Event(ShowJetpackIndividualPluginOverlay))
            }
        }
    }

    enum class ActionType {
        NAVIGATE_TO_STATE,
        CONTINUE_REBLOG_TO,
        ASK_FOR_SITE_SELECTION,
        SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY
    }

    enum class NavigateState {
        TO_SITE_SELECTED,
        TO_NO_SITE_SELECTED
    }

    sealed class Action(val actionType: ActionType) {
        data class NavigateToState(val navigateState: NavigateState, val siteForReblog: SiteRecord? = null) : Action(
            NAVIGATE_TO_STATE
        )

        data class ContinueReblogTo(val siteForReblog: SiteRecord?) : Action(
            CONTINUE_REBLOG_TO
        )

        object AskForSiteSelection : Action(ASK_FOR_SITE_SELECTION)

        object ShowJetpackIndividualPluginOverlay : Action(SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
    }

    companion object {
        private const val DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY = 500L
    }
}
