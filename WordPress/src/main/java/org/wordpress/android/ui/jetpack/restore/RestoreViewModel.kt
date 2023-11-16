package org.wordpress.android.ui.jetpack.restore

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.JETPACK_RESTORE_CONFIRMED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.JETPACK_RESTORE_ERROR
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindRequestTypes
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.restore.RestoreErrorTypes.GenericFailure
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.AwaitingCredentials
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Empty
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
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.text.PercentFormatter
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.util.wizard.WizardStep
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

const val KEY_RESTORE_ACTIVITY_ID_KEY = "key_restore_activity_id_key"
const val KEY_RESTORE_CURRENT_STEP = "key_restore_current_step"
const val KEY_RESTORE_STATE = "key_restore_state"
private const val TRACKING_ERROR_CAUSE_OFFLINE = "offline"
private const val TRACKING_ERROR_CAUSE_REMOTE = "remote"
private const val TRACKING_ERROR_CAUSE_OTHER = "other"

@Parcelize
@SuppressLint("ParcelCreator")
data class RestoreState(
    val rewindId: String? = null,
    val optionsSelected: List<Pair<Int, Boolean>>? = null,
    val restoreId: Long? = null,
    val published: Date? = null,
    val errorType: Int? = null,
    val shouldInitProgress: Boolean = true,
    val shouldInitDetails: Boolean = true
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
    private val percentFormatter: PercentFormatter
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
            restoreState = requireNotNull(savedInstanceState.getParcelableCompat(KEY_RESTORE_STATE))
            val currentStepIndex = savedInstanceState.getInt(KEY_RESTORE_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
    }

    fun onBackPressed() {
        when (wizardManager.currentStep) {
            DETAILS.id -> _wizardFinishedObservable.value = Event(RestoreCanceled)
            WARNING.id -> _wizardFinishedObservable.value = Event(RestoreCanceled)
            PROGRESS.id -> _wizardFinishedObservable.value = constructProgressEvent()
            COMPLETE.id -> _wizardFinishedObservable.value = Event(RestoreCompleted)
            ERROR.id -> _wizardFinishedObservable.value = Event(RestoreCanceled)
        }
    }

    private fun constructProgressEvent() = if (restoreState.restoreId != null) {
        Event(
            RestoreInProgress(
                restoreState.rewindId as String,
                restoreState.restoreId as Long
            )
        )
    } else {
        Event(RestoreCanceled)
    }

    fun writeToBundle(outState: Bundle) {
        outState.putInt(KEY_RESTORE_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_RESTORE_STATE, restoreState)
    }

    private fun buildDetails(isAwaitingCredentials: Boolean = false) {
        launch {
            val availableItems = availableItemsProvider.getAvailableItems()
            val activityLogModel = getActivityLogItemUseCase.get(activityId)
            if (activityLogModel != null) {
                if (restoreState.shouldInitDetails) {
                    restoreState = restoreState.copy(shouldInitDetails = false)
                    queryRestoreStatus(checkIfAwaitingCredentials = true)
                } else {
                    _uiState.value = DetailsState(
                        activityLogModel = activityLogModel,
                        items = stateListItemBuilder.buildDetailsListStateItems(
                            availableItems = availableItems,
                            published = activityLogModel.published,
                            siteId = site.siteId,
                            isAwaitingCredentials = isAwaitingCredentials,
                            onCreateDownloadClick = this@RestoreViewModel::onRestoreSiteClick,
                            onCheckboxItemClicked = this@RestoreViewModel::onCheckboxItemClicked,
                            onEnterServerCredsIconClicked = this@RestoreViewModel::onEnterServerCredsIconClicked
                        ),
                        type = StateType.DETAILS
                    )
                }
            } else {
                trackError(TRACKING_ERROR_CAUSE_OTHER)
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
                isIndeterminate = true
            ),
            type = StateType.PROGRESS
        )
        if (restoreState.shouldInitProgress) {
            restoreState = restoreState.copy(shouldInitProgress = false)
            launch {
                val result = postRestoreUseCase.postRestoreRequest(
                    restoreState.rewindId as String,
                    site,
                    buildRewindRequestTypes(restoreState.optionsSelected)
                )
                handleRestoreRequestResult(result)
            }
        } else {
            queryRestoreStatus()
        }
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
            items = stateListItemBuilder.buildErrorListStateErrorItems(
                errorType = errorType,
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
        return listOf(
            Pair(
                THEMES.id,
                checkboxes.firstOrNull { it.availableItemType == THEMES }?.checked ?: true
            ),
            Pair(
                PLUGINS.id,
                checkboxes.firstOrNull { it.availableItemType == PLUGINS }?.checked ?: true
            ),
            Pair(
                MEDIA_UPLOADS.id,
                checkboxes.firstOrNull { it.availableItemType == MEDIA_UPLOADS }?.checked
                    ?: true
            ),
            Pair(
                SQLS.id,
                checkboxes.firstOrNull { it.availableItemType == SQLS }?.checked ?: true
            ),
            Pair(
                ROOTS.id,
                checkboxes.firstOrNull { it.availableItemType == ROOTS }?.checked ?: true
            ),
            Pair(
                CONTENTS.id,
                checkboxes.firstOrNull { it.availableItemType == CONTENTS }?.checked ?: true
            )
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
                trackError(TRACKING_ERROR_CAUSE_OFFLINE)
                handleRestoreRequestError(NetworkUnavailableMsg)
            }
            is RemoteRequestFailure -> {
                trackError(TRACKING_ERROR_CAUSE_REMOTE)
                handleRestoreRequestError(GenericFailureMsg)
            }
            is Success -> handleRestoreRequestSuccess(result)
            is OtherRequestRunning -> {
                trackError(TRACKING_ERROR_CAUSE_OTHER)
                handleRestoreRequestError(OtherRequestRunningMsg)
            }
            else -> throw Throwable("Unexpected restoreRequestResult ${this.javaClass.simpleName}")
        }
    }

    private fun handleRestoreRequestSuccess(result: Success) {
        restoreState = restoreState.copy(
            rewindId = result.rewindId,
            restoreId = result.restoreId
        )
        (_uiState.value as? ProgressState)?.let {
            val updatedItems = stateListItemBuilder.updateProgressActionButtonState(it, result.restoreId != null)
            _uiState.postValue(it.copy(items = updatedItems))
        }
        queryRestoreStatus()
    }

    private fun handleRestoreRequestError(snackbarMessageHolder: SnackbarMessageHolder) {
        _snackbarEvents.postValue((Event(snackbarMessageHolder)))
        resetWizardIndex(DETAILS)
        showStep(NavigationTarget(DETAILS, restoreState))
    }

    private fun resetWizardIndex(targetStep: WizardStep) {
        val currentIndex = wizardManager.currentStep
        val targetIndex = wizardManager.stepPosition(targetStep)

        (currentIndex downTo targetIndex).forEach { _ ->
            wizardManager.onBackPressed()
        }
    }

    private fun extractPublishedDate(): Date {
        return (_uiState.value as? DetailsState)?.activityLogModel?.published as Date
    }

    private fun queryRestoreStatus(checkIfAwaitingCredentials: Boolean = false) {
        launch {
            getRestoreStatusUseCase.getRestoreStatus(site, restoreState.restoreId, checkIfAwaitingCredentials)
                .collect { state -> handleQueryStatus(state) }
        }
    }

    private fun handleQueryStatus(restoreStatus: RestoreRequestState) {
        when (restoreStatus) {
            is NetworkUnavailable -> {
                trackError(TRACKING_ERROR_CAUSE_OFFLINE)
                transitionToError(RestoreErrorTypes.RemoteRequestFailure)
            }
            is RemoteRequestFailure -> {
                trackError(TRACKING_ERROR_CAUSE_REMOTE)
                transitionToError(RestoreErrorTypes.RemoteRequestFailure)
            }
            is AwaitingCredentials -> buildDetails(isAwaitingCredentials = restoreStatus.isAwaitingCredentials)
            is Progress -> transitionToProgress(restoreStatus)
            is Complete -> wizardManager.showNextStep()
            is Empty -> transitionToError(RestoreErrorTypes.RemoteRequestFailure)
            else -> Unit // Do nothing
        }
    }

    private fun transitionToProgress(restoreStatus: Progress) {
        (_uiState.value as? ProgressState)?.let { content ->
            val updatedList = content.items.map { contentState ->
                if (contentState.type == ViewType.PROGRESS) {
                    contentState as JetpackListItemState.ProgressState
                    contentState.copy(
                        progress = restoreStatus.progress ?: 0,
                        progressLabel = UiStringText(percentFormatter.format(restoreStatus.progress ?: 0)),
                        progressInfoLabel = if (restoreStatus.currentEntry != null) {
                            UiStringText("${restoreStatus.currentEntry}")
                        } else {
                            null
                        },
                        progressStateLabel = if (restoreStatus.message != null) {
                            UiStringText("${restoreStatus.message}")
                        } else {
                            null
                        },
                        isIndeterminate = (restoreStatus.progress ?: 0) <= 0
                    )
                } else {
                    contentState
                }
            }
            _uiState.postValue(content.copy(items = updatedList))
        }
    }

    private fun clearOldRestoreState(wizardStep: RestoreStep) {
        if (wizardStep == DETAILS) {
            restoreState = restoreState.copy(
                rewindId = null,
                restoreId = null,
                errorType = null,
                optionsSelected = null,
                published = null,
                shouldInitProgress = true,
                shouldInitDetails = true
            )
        }
    }

    private fun onCheckboxItemClicked(itemType: JetpackAvailableItemType) {
        (_uiState.value as? DetailsState)?.let {
            val updatedItems = stateListItemBuilder.updateCheckboxes(it, itemType)
            _uiState.postValue(it.copy(items = updatedItems))
        }
    }

    private fun onEnterServerCredsIconClicked() {
        _navigationEvents.postValue(Event(ShowJetpackSettings("${Constants.URL_JETPACK_SETTINGS}/${site.siteId}")))
    }

    private fun onRestoreSiteClick() {
        val (rewindId, optionsSelected) = getParams()
        if (rewindId == null) {
            trackError(TRACKING_ERROR_CAUSE_OTHER)
            transitionToError(GenericFailure)
        } else {
            restoreState = restoreState.copy(
                rewindId = rewindId,
                optionsSelected = optionsSelected,
                published = extractPublishedDate(),
                shouldInitProgress = true
            )
            wizardManager.showNextStep()
        }
    }

    private fun onConfirmRestoreClick() {
        if (restoreState.rewindId == null) {
            trackError(TRACKING_ERROR_CAUSE_OTHER)
            transitionToError(GenericFailure)
        } else {
            trackRestoreConfirmed()
            wizardManager.showNextStep()
        }
    }

    private fun onCancelClick() {
        wizardManager.onBackPressed()
        showStep(NavigationTarget(DETAILS, restoreState))
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

    private fun trackRestoreConfirmed() {
        val types = buildRewindRequestTypes(restoreState.optionsSelected)
        val propertiesSetup = mapOf(
            "themes" to types.themes,
            "plugins" to types.plugins,
            "uploads" to types.uploads,
            "sqls" to types.sqls,
            "roots" to types.roots,
            "contents" to types.contents
        )
        val map = mapOf("restore_types" to JSONObject(propertiesSetup))
        AnalyticsTracker.track(JETPACK_RESTORE_CONFIRMED, map)
    }

    private fun trackError(cause: String) {
        val properties: MutableMap<String, String?> = HashMap()
        properties["cause"] = cause
        AnalyticsTracker.track(JETPACK_RESTORE_ERROR, properties)
    }

    companion object {
        private val NetworkUnavailableMsg = SnackbarMessageHolder(UiStringRes(R.string.error_network_connection))
        private val GenericFailureMsg = SnackbarMessageHolder(UiStringRes(R.string.restore_generic_failure))
        private val OtherRequestRunningMsg =
            SnackbarMessageHolder(UiStringRes(R.string.restore_another_process_running))
    }

    @SuppressLint("ParcelCreator")
    sealed class RestoreWizardState : Parcelable {
        @Parcelize
        object RestoreCanceled : RestoreWizardState()

        @Parcelize
        data class RestoreInProgress(
            val rewindId: String,
            val restoreId: Long
        ) : RestoreWizardState()

        @Parcelize
        object RestoreCompleted : RestoreWizardState()
    }
}
