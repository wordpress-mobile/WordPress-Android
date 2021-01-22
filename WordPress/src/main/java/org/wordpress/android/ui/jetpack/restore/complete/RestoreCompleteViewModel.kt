package org.wordpress.android.ui.jetpack.restore.complete

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.restore.builders.RestoreStateListItemBuilder
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class RestoreCompleteViewModel @Inject constructor(
    private val stateListItemBuilder: RestoreStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var restoreState: RestoreState
    private lateinit var parentViewModel: RestoreViewModel
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _navigationEvents = MediatorLiveData<Event<RestoreNavigationEvents>>()
    val navigationEvents: LiveData<Event<RestoreNavigationEvents>> = _navigationEvents

    fun start(
        site: SiteModel,
        restoreState: RestoreState,
        parentViewModel: RestoreViewModel
    ) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.restoreState = restoreState
        this.parentViewModel = parentViewModel

        initSources()
        initView()
    }

    private fun initSources() {
        parentViewModel.addNavigationEventSource(navigationEvents)
    }

    private fun initView() {
        if (restoreState.errorType != null) {
            parentViewModel.setToolbarState(ErrorToolbarState())
            _uiState.value = UiState(
                items = stateListItemBuilder.buildCompleteListStateErrorItems(
                        onDoneClick = this@RestoreCompleteViewModel::onDoneClick
                ))
        } else {
            parentViewModel.setToolbarState(CompleteToolbarState())
            _uiState.value = UiState(
                items = stateListItemBuilder.buildCompleteListStateItems(
                published = restoreState.published as Date,
                onDoneClick = this@RestoreCompleteViewModel::onDoneClick,
                onVisitSiteClick = this@RestoreCompleteViewModel::onVisitSiteClick
            ))
        }
    }

    private fun onVisitSiteClick() {
        site.url?.let { _navigationEvents.postValue(Event(VisitSite(site.url))) }
    }

    private fun onDoneClick() {
        parentViewModel.onRestoreCanceled()
    }

    data class UiState(
        val items: List<JetpackListItemState>
    )
}
