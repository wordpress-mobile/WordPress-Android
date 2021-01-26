package org.wordpress.android.ui.jetpack.backup.download

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ActivityLogStore.BackupDownloadRequestTypes
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.DownloadFile
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.ShareLink
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Complete
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.NetworkUnavailable
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.OtherRequestRunning
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Failure.RemoteRequestFailure
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Progress
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState.Success
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.COMPLETE
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.DETAILS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.ERROR
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.PROGRESS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ContentState.CompleteState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ContentState.DetailsState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ContentState.ProgressState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ErrorState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.jetpack.backup.download.builders.BackupDownloadStateListItemBuilder
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostBackupDownloadUseCase
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

const val KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY = "key_backup_download_activity_id_key"
const val KEY_BACKUP_DOWNLOAD_CURRENT_STEP = "key_backup_download_current_step"
const val KEY_BACKUP_DOWNLOAD_STATE = "key_backup_download_state"

@Parcelize
@SuppressLint("ParcelCreator")
data class BackupDownloadState(
    val siteId: Long? = null,
    val activityId: String? = null,
    val rewindId: String? = null,
    val downloadId: Long? = null,
    val published: Date? = null,
    val url: String? = null,
    val errorType: Int? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<BackupDownloadStep, BackupDownloadState>

class BackupDownloadViewModel @Inject constructor(
    private val wizardManager: WizardManager<BackupDownloadStep>,
    private val availableItemsProvider: JetpackAvailableItemsProvider,
    private val getActivityLogItemUseCase: GetActivityLogItemUseCase,
    private val stateListItemBuilder: BackupDownloadStateListItemBuilder,
    private val postBackupDownloadUseCase: PostBackupDownloadUseCase,
    private val getBackupDownloadStatusUseCase: GetBackupDownloadStatusUseCase,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var site: SiteModel
    private lateinit var activityId: String

    private lateinit var backupDownloadState: BackupDownloadState
    private val progressStart = 0

    private val _wizardFinishedObservable = MutableLiveData<Event<BackupDownloadWizardState>>()
    val wizardFinishedObservable: LiveData<Event<BackupDownloadWizardState>> = _wizardFinishedObservable

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _navigationEvents = MediatorLiveData<Event<BackupDownloadNavigationEvents>>()
    val navigationEvents: LiveData<Event<BackupDownloadNavigationEvents>> = _navigationEvents

    private val _uiState = MutableLiveData<BackupDownloadUiState>()
    val uiState: LiveData<BackupDownloadUiState> = _uiState

    private val wizardObserver = Observer { data: BackupDownloadStep? ->
        data?.let {
            clearOldBackupDownloadState(it)
            showStep(NavigationTarget(it, backupDownloadState))
        }
    }

    fun start(site: SiteModel, activityId: String, savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.activityId = activityId

        wizardManager.navigatorLiveData.observeForever(wizardObserver)

        if (savedInstanceState == null) {
            backupDownloadState = BackupDownloadState()
            // Show the next step only if it's a fresh activity so we can handle the navigation
            wizardManager.showNextStep()
        } else {
            backupDownloadState = requireNotNull(savedInstanceState.getParcelable(KEY_BACKUP_DOWNLOAD_STATE))
            val currentStepIndex = savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
    }

    fun onBackPressed() {
        when (wizardManager.currentStep) {
            DETAILS.id -> { _wizardFinishedObservable.value = Event(BackupDownloadCanceled) }
            PROGRESS.id -> {
                _wizardFinishedObservable.value = if (backupDownloadState.downloadId != null) {
                    Event(BackupDownloadInProgress(backupDownloadState.downloadId as Long))
                } else {
                    Event(BackupDownloadCanceled)
                }
            }
            COMPLETE.id -> { _wizardFinishedObservable.value = Event(BackupDownloadCompleted) }
            ERROR.id -> { _wizardFinishedObservable.value = Event(BackupDownloadCanceled) }
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_BACKUP_DOWNLOAD_STATE, backupDownloadState)
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
                                onCreateDownloadClick = this@BackupDownloadViewModel::onCreateDownloadClick,
                                onCheckboxItemClicked = this@BackupDownloadViewModel::onCheckboxItemClicked
                        ),
                        type = StateType.DETAILS
                )
            } else {
                transitionToError(BackupDownloadErrorTypes.GenericFailure)
            }
        }
    }

    private fun buildProgress() {
        _uiState.value = ProgressState(
                items = stateListItemBuilder.buildProgressListStateItems(
                        progress = progressStart,
                        published = backupDownloadState.published as Date,
                        onNotifyMeClick = this@BackupDownloadViewModel::onNotifyMeClick
                ),
                type = StateType.PROGRESS
        )
        queryStatus()
    }

    private fun buildComplete() {
        _uiState.value = CompleteState(
                items = stateListItemBuilder.buildCompleteListStateItems(
                        published = backupDownloadState.published as Date,
                        onDownloadFileClick = this@BackupDownloadViewModel::onDownloadFileClick,
                        onShareLinkClick = this@BackupDownloadViewModel::onShareLinkClick
                ), type = StateType.COMPLETE)
    }

    private fun buildError(errorType: BackupDownloadErrorTypes) {
        _uiState.value = ErrorState(
                errorType = errorType,
                items = stateListItemBuilder.buildCompleteListStateErrorItems(
                        onDoneClick = this@BackupDownloadViewModel::onDoneClick
                ))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showStep(target: WizardNavigationTarget<BackupDownloadStep, BackupDownloadState>) {
        when (target.wizardStep) {
            DETAILS -> buildDetails()
            PROGRESS -> buildProgress()
            COMPLETE -> buildComplete()
            ERROR -> buildError(
                    BackupDownloadErrorTypes.fromInt(
                            target.wizardState.errorType ?: BackupDownloadErrorTypes.GenericFailure.id
                    )
            )
        }
    }

    private fun transitionToError(errorType: BackupDownloadErrorTypes) {
        backupDownloadState = backupDownloadState.copy(errorType = errorType.id)
        wizardManager.setCurrentStepIndex(BackupDownloadStep.indexForErrorTransition())
        wizardManager.showNextStep()
    }

    private fun getParams(): Pair<String?, BackupDownloadRequestTypes> {
        val rewindId = (_uiState.value as? DetailsState)?.activityLogModel?.rewindID
        val items = _uiState.value?.items ?: mutableListOf()
        val types = buildBackupDownloadRequestTypes(items)
        return rewindId to types
    }

    private fun buildBackupDownloadRequestTypes(items: List<JetpackListItemState>): BackupDownloadRequestTypes {
        val checkboxes = items.filterIsInstance(CheckboxState::class.java)

        return BackupDownloadRequestTypes(
                themes = checkboxes.firstOrNull { it.availableItemType == THEMES }?.checked ?: true,
                plugins = checkboxes.firstOrNull { it.availableItemType == PLUGINS }?.checked
                        ?: true,
                uploads = checkboxes.firstOrNull { it.availableItemType == MEDIA_UPLOADS }?.checked
                        ?: true,
                sqls = checkboxes.firstOrNull { it.availableItemType == SQLS }?.checked ?: true,
                roots = checkboxes.firstOrNull { it.availableItemType == ROOTS }?.checked ?: true,
                contents = checkboxes.firstOrNull { it.availableItemType == CONTENTS }?.checked
                        ?: true
        )
    }

    private fun handleBackupDownloadRequestResult(result: BackupDownloadRequestState) {
        when (result) {
            is NetworkUnavailable -> {
                _snackbarEvents.postValue(Event(NetworkUnavailableMsg))
            }
            is RemoteRequestFailure -> {
                _snackbarEvents.postValue(Event(GenericFailureMsg))
            }
            is Success -> {
                backupDownloadState = backupDownloadState.copy(
                        rewindId = result.rewindId,
                        downloadId = result.downloadId,
                        published = extractPublishedDate())
                wizardManager.showNextStep()
            }
            is OtherRequestRunning -> {
                _snackbarEvents.postValue(Event(OtherRequestRunningMsg))
            }
            else -> {
            } // no op
        }
    }

    private fun extractPublishedDate(): Date {
        return (_uiState.value as? DetailsState)?.activityLogModel?.published as Date
    }

    private fun queryStatus() {
        launch {
            getBackupDownloadStatusUseCase.getBackupDownloadStatus(
                    site,
                    backupDownloadState.downloadId as Long
            ).flowOn(bgDispatcher).collect { state -> handleQueryStatus(state) }
        }
    }

    private fun handleQueryStatus(state: BackupDownloadRequestState) {
        when (state) {
            is NetworkUnavailable -> {
                transitionToError(BackupDownloadErrorTypes.NetworkUnavailable)
            }
            is RemoteRequestFailure -> {
                transitionToError(BackupDownloadErrorTypes.RemoteRequestFailure)
            }
            is Progress -> {
                (_uiState.value as? ProgressState)?.let { content ->
                    val updatedList = content.items.map { contentState ->
                        if (contentState.type == ViewType.PROGRESS) {
                            contentState as JetpackListItemState.ProgressState
                            contentState.copy(
                                    progress = state.progress ?: 0,
                                    progressLabel = UiStringResWithParams(
                                            string.backup_download_progress_label,
                                            listOf(UiStringText(state.progress?.toString() ?: "0"))
                                    )
                            )
                        } else {
                            contentState
                        }
                    }
                    _uiState.postValue(content.copy(items = updatedList))
                }
            }
            is Complete -> {
                backupDownloadState = backupDownloadState.copy(url = state.url)
                wizardManager.showNextStep()
            }
            else -> {
            } // no op
        }
    }

    private fun clearOldBackupDownloadState(wizardStep: BackupDownloadStep) {
        if (wizardStep == DETAILS) {
            backupDownloadState = backupDownloadState.copy(
                    rewindId = null,
                    downloadId = null,
                    url = null,
                    errorType = null
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

    private fun onCreateDownloadClick() {
        val (rewindId, types) = getParams()
        if (rewindId == null) {
            transitionToError(BackupDownloadErrorTypes.GenericFailure)
        } else {
            launch {
                val result = postBackupDownloadUseCase.postBackupDownloadRequest(
                        rewindId,
                        site,
                        types
                )
                handleBackupDownloadRequestResult(result)
            }
        }
    }

    private fun onNotifyMeClick() {
        _wizardFinishedObservable.value = Event(BackupDownloadInProgress(backupDownloadState.downloadId as Long))
    }

    private fun onDownloadFileClick() {
        backupDownloadState.url?.let { _navigationEvents.postValue(Event(DownloadFile(it))) }
    }

    private fun onShareLinkClick() {
        backupDownloadState.url?.let { _navigationEvents.postValue(Event(ShareLink(it))) }
    }

    private fun onDoneClick() {
        _wizardFinishedObservable.value = Event(BackupDownloadCanceled)
    }

    override fun onCleared() {
        super.onCleared()
        wizardManager.navigatorLiveData.removeObserver(wizardObserver)
    }

    companion object {
        private val NetworkUnavailableMsg = SnackbarMessageHolder(UiStringRes(string.error_network_connection))
        private val GenericFailureMsg = SnackbarMessageHolder(UiStringRes(string.backup_download_generic_failure))
        private val OtherRequestRunningMsg = SnackbarMessageHolder(
                UiStringRes(string.backup_download_another_download_running)
        )
    }

    sealed class BackupDownloadWizardState : Parcelable {
        @Parcelize
        object BackupDownloadCanceled : BackupDownloadWizardState()

        @Parcelize
        data class BackupDownloadInProgress(val downloadId: Long) : BackupDownloadWizardState()

        @Parcelize
        object BackupDownloadCompleted : BackupDownloadWizardState()
    }
}
