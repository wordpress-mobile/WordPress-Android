package org.wordpress.android.ui.jetpack.restore.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.common.ViewType.PROGRESS
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Progress
import org.wordpress.android.ui.jetpack.restore.RestoreState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.restore.builders.RestoreStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class RestoreProgressViewModel @Inject constructor(
    private val getRestoreStatusUseCase: GetRestoreStatusUseCase,
    private val stateListItemBuilder: RestoreStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var restoreState: RestoreState
    private lateinit var parentViewModel: RestoreViewModel
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _errorEvents = MediatorLiveData<Event<RestoreErrorTypes>>()
    val errorEvents: LiveData<Event<RestoreErrorTypes>> = _errorEvents

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

        parentViewModel.setToolbarState(ProgressToolbarState())

        initSources()

        initView()
        queryStatus()
    }

    private fun initSources() {
        parentViewModel.addErrorMessageSource(errorEvents)
    }

    private fun initView() {
        _uiState.value = UiState(
                items = stateListItemBuilder.buildProgressListStateItems(
                        progress = 0,
                        published = restoreState.published as Date,
                        onNotifyMeClick = this@RestoreProgressViewModel::onNotifyMeClick
                )
        )
    }

    private fun queryStatus() {
        launch {
            getRestoreStatusUseCase.getRestoreStatus(site, restoreState.restoreId as Long)
            .flowOn(bgDispatcher).collect { state -> handleState(state) }
        }
    }

    private fun handleState(state: RestoreRequestState) {
        when (state) {
            is NetworkUnavailable -> {
                _errorEvents.postValue(Event(RestoreErrorTypes.NetworkUnavailable))
            }
            is RemoteRequestFailure -> {
                _errorEvents.postValue(Event(RestoreErrorTypes.RemoteRequestFailure))
            }
            is Progress -> {
                _uiState.value?.let { uiState ->
                    val updatedList = uiState.items.map { contentState ->
                        if (contentState.type == PROGRESS) {
                            contentState as ProgressState
                            contentState.copy(
                                    progress = state.progress ?: 0,
                                    label = UiStringResWithParams(
                                            R.string.restore_progress_label,
                                            listOf(UiStringText(state.progress?.toString() ?: "0"))
                                    )
                            )
                        } else {
                            contentState
                        }
                    }
                    _uiState.postValue(uiState.copy(items = updatedList))
                }
            }
            is Complete -> {
                parentViewModel.onRestoreProgressFinished()
            }
            else -> {
            } // no op
        }
    }

    private fun onNotifyMeClick() {
        parentViewModel.onProgressExit(restoreState.restoreId as Long)
    }

    data class UiState(
        val items: List<JetpackListItemState>
    )
}
