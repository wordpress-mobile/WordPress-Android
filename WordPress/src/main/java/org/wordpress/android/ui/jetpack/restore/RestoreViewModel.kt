package org.wordpress.android.ui.jetpack.restore

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.jetpack.common.ViewType.CHECKBOX
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes.GenericFailure
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Progress
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Success
import org.wordpress.android.ui.jetpack.restore.RestoreStep.COMPLETE
import org.wordpress.android.ui.jetpack.restore.RestoreStep.DETAILS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.ERROR
import org.wordpress.android.ui.jetpack.restore.RestoreStep.PROGRESS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.WARNING
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.CompleteState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.DetailsState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.ProgressState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.WarningState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ErrorState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCompleted
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreInProgress
import org.wordpress.android.ui.jetpack.restore.builders.RestoreStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.jetpack.restore.usecases.PostRestoreUseCase
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

const val KEY_RESTORE_ACTIVITY_ID_KEY = "key_restore_activity_id_key"
const val KEY_RESTORE_CURRENT_STEP = "key_restore_current_step"
const val KEY_RESTORE_STATE = "key_restore_state"

@Parcelize
@SuppressLint("ParcelCreator")
data class RestoreState(
    val siteId: Long? = null,
    val activityId: String? = null,
    val rewindId: String? = null,
    val optionsSelected: List<Pair<Int, Boolean>>? = null,
    val restoreId: Long? = null,
    val published: Date? = null,
    val errorType: Int? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<RestoreStep, RestoreState>

class RestoreViewModel @Inject constructor(
    private val wizardManager: WizardManager<RestoreStep>,
    private val availableItemsProvider: JetpackAvailableItemsProvider,
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val stateListItemBuilder: RestoreStateListItemBuilder,
    private val postRestoreUseCase: PostRestoreUseCase,
    private val getRestoreStatusUseCase: GetRestoreStatusUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var site: SiteModel
    private lateinit var activityId: String
    private lateinit var restoreState: RestoreState
    private val progressStart = 0

    private val _wizardFinishedObservable = MutableLiveData<Event<RestoreWizardState>>()
    val wizardFinishedObservable: LiveData<Event<RestoreWizardState>> = _wizardFinishedObservable

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _navigationEvents = MediatorLiveData<Event<RestoreNavigationEvents>>()
    val navigationEvents: LiveData<Event<RestoreNavigationEvents>> = _navigationEvents

    private val _uiState = MutableLiveData<RestoreUiState>()
    val uiState: LiveData<RestoreUiState> = _uiState

    private val wizardObserver = Observer { data: RestoreStep? ->
        data?.let {
            clearOldRestoreState(it)
            showStep(NavigationTarget(it, restoreState))
        }
    }

    fun start(site: SiteModel, activityId: String, savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.activityId = activityId

        wizardManager.navigatorLiveData.observeForever(wizardObserver)

        if (savedInstanceState == null) {
            restoreState = RestoreState()
            // Show the next step only if it's a fresh activity so we can handle the navigation
            wizardManager.showNextStep()
        } else {
            restoreState = requireNotNull(savedInstanceState.getParcelable(KEY_RESTORE_STATE))
            val currentStepIndex = savedInstanceState.getInt(KEY_RESTORE_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
    }

    fun onBackPressed() {
        when (wizardManager.currentStep) {
            DETAILS.id -> { _wizardFinishedObservable.value = Event(RestoreCanceled) }
            WARNING.id -> { _wizardFinishedObservable.value = Event(RestoreCanceled) }
            PROGRESS.id -> {
                _wizardFinishedObservable.value = if (restoreState.restoreId != null) {
                    Event(RestoreInProgress(restoreState.restoreId as Long))
                } else {
                    Event(RestoreCanceled)
                }
            }
            COMPLETE.id -> { _wizardFinishedObservable.value = Event(RestoreCompleted) }
            ERROR.id -> { _wizardFinishedObservable.value = Event(RestoreCanceled) }
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putInt(KEY_RESTORE_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_RESTORE_STATE, restoreState)
    }

    private fun buildDetails() {
        launch {
            val availableItems = availableItemsProvider.getAvailableItems()
            val activityLogModel = getActivityLogItemUseCase.get(activityId)
            if (activityLogModel != null) {
                _uiState.value = DetailsState(
                        activityLogModel = activityLogModel,
                        items = stateListItemBuilder.buildDetailsListStateItems(
                                availableItems = availableItems,
                                published = activityLogModel.published,
                                onCreateDownloadClick = this@RestoreViewModel::onRestoreSiteClick,
                                onCheckboxItemClicked = this@RestoreViewModel::onCheckboxItemClicked
                        ),
                        type = StateType.DETAILS
                )
            } else {
                transitionToError(GenericFailure)
            }
        }
    }

    private fun buildWarning() {
        _uiState.value = WarningState(
                items = stateListItemBuilder.buildWarningListStateItems(
                        published = restoreState.published as Date,
                        onConfirmRestoreClick = this@RestoreViewModel::onConfirmRestoreClick,
                        onCancelClick = this@RestoreViewModel::onCancelClick
                ),
                type = StateType.WARNING
        )
    }

    private fun buildProgress() {
        _uiState.value = ProgressState(
                items = stateListItemBuilder.buildProgressListStateItems(
                        progress = progressStart,
                        published = restoreState.published as Date,
                        isIndeterminate = true,
                        onNotifyMeClick = this@RestoreViewModel::onNotifyMeClick
                ),
                type = StateType.PROGRESS
        )
        queryStatus()
    }

    private fun buildComplete() {
        _uiState.value = CompleteState(
                items = stateListItemBuilder.buildCompleteListStateItems(
                        published = restoreState.published as Date,
                        onDoneClick = this@RestoreViewModel::onDoneClick,
                        onVisitSiteClick = this@RestoreViewModel::onVisitSiteClick
                ),
                type = StateType.COMPLETE
        )
    }

    private fun buildError(errorType: RestoreErrorTypes) {
        _uiState.value = ErrorState(
                items = stateListItemBuilder.buildCompleteListStateErrorItems(
                        onDoneClick = this@RestoreViewModel::onDoneClick
                ),
                errorType = errorType
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showStep(target: WizardNavigationTarget<RestoreStep, RestoreState>) {
        when (target.wizardStep) {
            DETAILS -> buildDetails()
            WARNING -> buildWarning()
            PROGRESS -> buildProgress()
            COMPLETE -> buildComplete()
            ERROR -> buildError(
                    RestoreErrorTypes.fromInt(
                            target.wizardState.errorType ?: GenericFailure.id
                    )
            )
        }
    }

    private fun transitionToError(errorType: RestoreErrorTypes) {
        restoreState = restoreState.copy(errorType = errorType.id)
        wizardManager.setCurrentStepIndex(RestoreStep.indexForErrorTransition())
        wizardManager.showNextStep()
    }

    private fun getParams(): Pair<String?, List<Pair<Int, Boolean>>> {
        val rewindId = (_uiState.value as? DetailsState)?.activityLogModel?.rewindID
        val items = _uiState.value?.items ?: mutableListOf()
        val options = buildOptionsSelected(items)
        return rewindId to options
    }

    private fun buildOptionsSelected(items: List<JetpackListItemState>): List<Pair<Int, Boolean>> {
        val checkboxes = items.filterIsInstance(CheckboxState::class.java)
        return listOf(Pair(THEMES.id, checkboxes.firstOrNull { it.availableItemType == THEMES }?.checked ?: true),
                Pair(PLUGINS.id, checkboxes.firstOrNull { it.availableItemType == PLUGINS }?.checked ?: true),
                Pair(MEDIA_UPLOADS.id, checkboxes.firstOrNull { it.availableItemType == MEDIA_UPLOADS }?.checked
                        ?: true),
                Pair(SQLS.id, checkboxes.firstOrNull { it.availableItemType == SQLS }?.checked ?: true),
                Pair(ROOTS.id, checkboxes.firstOrNull { it.availableItemType == ROOTS }?.checked ?: true),
                Pair(CONTENTS.id, checkboxes.firstOrNull { it.availableItemType == CONTENTS }?.checked ?: true)
        )
    }

    private fun buildRewindRequestTypes(optionsSelected: List<Pair<Int, Boolean>>?) =
        RewindRequestTypes(
                themes = optionsSelected?.firstOrNull { it.first == THEMES.id }?.second ?: true,
                plugins = optionsSelected?.firstOrNull { it.first == PLUGINS.id }?.second
                        ?: true,
                uploads = optionsSelected?.firstOrNull { it.first == MEDIA_UPLOADS.id }?.second
                        ?: true,
                sqls = optionsSelected?.firstOrNull { it.first == SQLS.id }?.second ?: true,
                roots = optionsSelected?.firstOrNull { it.first == ROOTS.id }?.second ?: true,
                contents = optionsSelected?.firstOrNull { it.first == CONTENTS.id }?.second
                        ?: true
        )

    private fun handleRestoreRequestResult(result: RestoreRequestState) {
        when (result) {
            is NetworkUnavailable -> {
                handleRestoreRequestError(NetworkUnavailableMsg)
            }
            is RemoteRequestFailure -> {
                handleRestoreRequestError(GenericFailureMsg)
            }
            is Success -> {
                restoreState = restoreState.copy(
                        rewindId = result.rewindId,
                        restoreId = result.restoreId
                )
                wizardManager.showNextStep()
            }
            is OtherRequestRunning -> {
                handleRestoreRequestError(OtherRequestRunningMsg)
            }
            else -> {
                throw Throwable("Unexpected restoreRequestResult ${this.javaClass.simpleName}")
            }
        }
    }

    private fun handleRestoreRequestError(snackbarMessageHolder: SnackbarMessageHolder) {
        _snackbarEvents.postValue((Event(snackbarMessageHolder)))
        wizardManager.onBackPressed()
        showStep(NavigationTarget(DETAILS, restoreState))
    }

    private fun extractPublishedDate(): Date {
        return (_uiState.value as? DetailsState)?.activityLogModel?.published as Date
    }

    private fun queryStatus() {
        launch {
            getRestoreStatusUseCase.getRestoreStatus(site, restoreState.restoreId as Long)
                    .flowOn(bgDispatcher).collect { state -> handleQueryStatus(state) }
        }
    }

    private fun handleQueryStatus(restoreStatus: RestoreRequestState) {
        when (restoreStatus) {
            is NetworkUnavailable -> { transitionToError(RestoreErrorTypes.NetworkUnavailable) }
            is RemoteRequestFailure -> { transitionToError(RestoreErrorTypes.RemoteRequestFailure) }
            is Progress -> {
                (_uiState.value as? ProgressState)?.let { content ->
                    val updatedList = content.items.map { contentState ->
                        if (contentState.type == ViewType.PROGRESS) {
                            contentState as JetpackListItemState.ProgressState
                            contentState.copy(
                                    progress = restoreStatus.progress ?: 0,
                                    progressLabel = UiStringResWithParams(
                                            R.string.restore_progress_label,
                                            listOf(UiStringText(restoreStatus.progress?.toString() ?: "0"))
                                    ),
                                    progressInfoLabel = if (restoreStatus.currentEntry != null) {
                                            UiStringText("${restoreStatus.currentEntry}") }
                                        else {
                                            null
                                        },
                                    progressStateLabel = UiStringText("${restoreStatus.message}"),
                                    isIndeterminate = (restoreStatus.progress ?: 0) <= 0
                            )
                        } else {
                            contentState
                        }
                    }
                    _uiState.postValue(content.copy(items = updatedList))
                }
            }
            is Complete -> { wizardManager.showNextStep() }
            else -> {
                throw Throwable("Unexpected queryStatus result ${this.javaClass.simpleName}")
            }
        }
    }

    private fun clearOldRestoreState(wizardStep: RestoreStep) {
        if (wizardStep == DETAILS) {
            restoreState = restoreState.copy(
                    rewindId = null,
                    restoreId = null,
                    errorType = null,
                    optionsSelected = null,
                    published = null
            )
        }
    }

    private fun onCheckboxItemClicked(itemType: JetpackAvailableItemType) {
        (_uiState.value as? DetailsState)?.let { details ->
            val updatedList = details.items.map { contentState ->
                if (contentState.type == CHECKBOX) {
                    contentState as CheckboxState
                    if (contentState.availableItemType == itemType) {
                        contentState.copy(checked = !contentState.checked)
                    } else {
                        contentState
                    }
                } else {
                    contentState
                }
            }
            _uiState.postValue(details.copy(items = updatedList))
        }
    }

    private fun onRestoreSiteClick() {
        val (rewindId, optionsSelected) = getParams()
        if (rewindId == null) {
            transitionToError(GenericFailure)
        } else {
            restoreState = restoreState.copy(
                    rewindId = rewindId,
                    optionsSelected = optionsSelected,
                    published = extractPublishedDate()
            )
            wizardManager.showNextStep()
        }
    }

    private fun onConfirmRestoreClick() {
        if (restoreState.rewindId == null) {
            transitionToError(GenericFailure)
        } else {
            launch {
                val result = postRestoreUseCase.postRestoreRequest(
                        restoreState.rewindId as String,
                        site,
                        buildRewindRequestTypes(restoreState.optionsSelected)
                )
                handleRestoreRequestResult(result)
            }
        }
    }

    private fun onCancelClick() {
        wizardManager.onBackPressed()
        showStep(NavigationTarget(DETAILS, restoreState))
    }

    private fun onNotifyMeClick() {
        _wizardFinishedObservable.value = Event(RestoreInProgress(restoreState.restoreId as Long))
    }

    private fun onVisitSiteClick() {
        site.url?.let { _navigationEvents.postValue(Event(VisitSite(site.url))) }
    }

    private fun onDoneClick() {
        _wizardFinishedObservable.value = Event(RestoreCanceled)
    }

    override fun onCleared() {
        super.onCleared()
        wizardManager.navigatorLiveData.removeObserver(wizardObserver)
    }

    companion object {
        private val NetworkUnavailableMsg = SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        private val GenericFailureMsg = SnackbarMessageHolder(UiStringRes(R.string.rewind_generic_failure))
        private val OtherRequestRunningMsg = SnackbarMessageHolder(UiStringRes(R.string.rewind_another_process_running))
    }

    sealed class RestoreWizardState : Parcelable {
        @Parcelize
        object RestoreCanceled : RestoreWizardState()

        @Parcelize
        data class RestoreInProgress(val restoreId: Long) : RestoreWizardState()

        @Parcelize
        object RestoreCompleted : RestoreWizardState()
    }
}
