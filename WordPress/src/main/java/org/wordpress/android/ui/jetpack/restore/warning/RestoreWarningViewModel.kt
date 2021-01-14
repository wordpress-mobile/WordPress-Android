package org.wordpress.android.ui.jetpack.restore.warning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Success
import org.wordpress.android.ui.jetpack.restore.RestoreState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.ToolbarState.WarningToolbarState
import org.wordpress.android.ui.jetpack.restore.usecases.PostRestoreUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class RestoreWarningViewModel @Inject constructor(
    private val postRestoreUseCase: PostRestoreUseCase,
    private val stateListItemBuilder: RestoreWarningStateListItemBuilder,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private lateinit var site: SiteModel
    private lateinit var parentViewModel: RestoreViewModel
    private lateinit var restoreState: RestoreState
    private var isStarted: Boolean = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

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
        this.parentViewModel = parentViewModel
        this.restoreState = restoreState

        parentViewModel.setToolbarState(WarningToolbarState())

        initSources()
        initView()
    }

    private fun initSources() {
        parentViewModel.addSnackbarMessageSource(snackbarEvents)
        parentViewModel.addErrorMessageSource(errorEvents)
    }

    private fun initView() {
        _uiState.value = UiState(
            items = stateListItemBuilder.buildWarningListStateItems(
                published = restoreState.published as Date,
                onConfirmRestoreClick = this@RestoreWarningViewModel::onConfirmRestoreClick
            )
        )
    }

    private fun onConfirmRestoreClick() {
        val (rewindId, types) = getParams()
        if (rewindId == null) {
            _errorEvents.value = Event(RestoreErrorTypes.GenericFailure)
        } else {
            launch {
                val result = postRestoreUseCase.postRestoreRequest(rewindId, site, types)
                handleRestoreRequestResult(result)
            }
        }
    }

    private fun getParams(): Pair<String?, RewindRequestTypes> {
        val rewindId = restoreState.rewindId
        val types = buildRewindRequestTypes(restoreState.optionsSelected)
        return rewindId to types
    }

    private fun buildRewindRequestTypes(optionsSelected: List<Pair<Int, Boolean>>?) =
        RewindRequestTypes(
            themes = optionsSelected?.firstOrNull { it.first == THEMES.id }?.second ?: true,
            plugins = optionsSelected?.firstOrNull { it.first == PLUGINS.id }?.second ?: true,
            uploads = optionsSelected?.firstOrNull { it.first == MEDIA_UPLOADS.id }?.second ?: true,
            sqls = optionsSelected?.firstOrNull { it.first == SQLS.id }?.second ?: true,
            roots = optionsSelected?.firstOrNull { it.first == ROOTS.id }?.second ?: true,
            contents = optionsSelected?.firstOrNull { it.first == CONTENTS.id }?.second ?: true
    )

    private fun handleRestoreRequestResult(result: RestoreRequestState) {
        when (result) {
            is NetworkUnavailable -> {
                _errorEvents.postValue(Event(RestoreErrorTypes.NetworkUnavailable))
            }
            is RemoteRequestFailure -> {
                _errorEvents.postValue(Event(RestoreErrorTypes.RemoteRequestFailure))
            }
            is Success -> {
                parentViewModel.onRestoreWarningFinished(
                        result.rewindId,
                        result.restoreId
                )
            }
            is OtherRequestRunning -> {
                // todo: annmarie where should this go? I would think back to details, so implement
                _snackbarEvents.postValue(Event(OtherRequestRunningMsg))
            }
            else -> {
            } // no op
        }
    }

    companion object {
        private val OtherRequestRunningMsg = SnackbarMessageHolder(
                UiStringRes(R.string.restore_another_restore_running))
    }

    data class UiState(val items: List<JetpackListItemState>)
}
