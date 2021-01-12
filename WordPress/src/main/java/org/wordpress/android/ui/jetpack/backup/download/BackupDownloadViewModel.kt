package org.wordpress.android.ui.jetpack.backup.download

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.COMPLETE
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.DETAILS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep.PROGRESS
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleEventObservable
import java.util.Date
import javax.inject.Inject

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
    private val wizardManager: WizardManager<BackupDownloadStep>
) : ViewModel() {
    private var isStarted = false

    private lateinit var backupDownloadState: BackupDownloadState

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> by lazy {
        SingleEventObservable(
                Transformations.map(wizardManager.navigatorLiveData) {
                    clearOldBackupDownloadState(it)
                    WizardNavigationTarget(it, backupDownloadState)
                }
        )
    }

    private val _wizardFinishedObservable = MutableLiveData<Event<BackupDownloadWizardState>>()
    val wizardFinishedObservable: LiveData<Event<BackupDownloadWizardState>> = _wizardFinishedObservable

    private val _toolbarStateObservable = MutableLiveData<ToolbarState>()
    val toolbarStateObservable: LiveData<ToolbarState> = _toolbarStateObservable

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _errorEvents = MediatorLiveData<Event<BackupDownloadErrorTypes>>()
    val errorEvents: LiveData<Event<BackupDownloadErrorTypes>> = _errorEvents

    private val _navigationEvents = MediatorLiveData<Event<BackupDownloadNavigationEvents>>()
    val navigationEvents: LiveData<Event<BackupDownloadNavigationEvents>> = _navigationEvents

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true
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

    fun addSnackbarMessageSource(snackbarEvents: LiveData<Event<SnackbarMessageHolder>>) {
        _snackbarEvents.addSource(snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    fun addErrorMessageSource(errorEvents: LiveData<Event<BackupDownloadErrorTypes>>) {
        _errorEvents.addSource(errorEvents) { event ->
            _errorEvents.value = event
        }
    }

    fun addNavigationEventSource(navigationEvent: LiveData<Event<BackupDownloadNavigationEvents>>) {
        _navigationEvents.addSource(navigationEvent) { event ->
            _navigationEvents.value = event
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_BACKUP_DOWNLOAD_STATE, backupDownloadState)
    }

    fun onBackPressed() {
        when (wizardManager.currentStep) {
            DETAILS.id -> {
                _wizardFinishedObservable.value = Event(BackupDownloadCanceled)
            }
            PROGRESS.id -> {
                _wizardFinishedObservable.value = if (backupDownloadState.downloadId != null) {
                    Event(BackupDownloadInProgress(backupDownloadState.downloadId as Long))
                } else {
                    Event(BackupDownloadCanceled)
                }
            }
            COMPLETE.id -> {
                _wizardFinishedObservable.value = Event(BackupDownloadCompleted)
            }
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

    fun onBackupDownloadDetailsFinished(rewindId: String?, downloadId: Long?, published: Date) {
        backupDownloadState = backupDownloadState.copy(
                rewindId = rewindId,
                downloadId = downloadId,
                published = published)
        wizardManager.showNextStep()
    }

    fun onBackupDownloadDetailsCanceled() {
        _wizardFinishedObservable.value = Event(BackupDownloadCanceled)
    }

    fun onBackupDownloadProgressFinished(url: String?) {
        backupDownloadState = backupDownloadState.copy(url = url)
        wizardManager.showNextStep()
    }

    fun setToolbarState(toolbarState: ToolbarState) {
        _toolbarStateObservable.value = toolbarState
    }

    fun transitionToError(errorType: BackupDownloadErrorTypes) {
        backupDownloadState = backupDownloadState.copy(errorType = errorType.id)
        wizardManager.setCurrentStepIndex(BackupDownloadStep.indexForErrorTransition())
        wizardManager.showNextStep()
    }

    sealed class BackupDownloadWizardState : Parcelable {
        @Parcelize
        object BackupDownloadCanceled : BackupDownloadWizardState()

        @Parcelize
        data class BackupDownloadInProgress(val downloadId: Long) : BackupDownloadWizardState()

        @Parcelize
        object BackupDownloadCompleted : BackupDownloadWizardState()
    }

    sealed class ToolbarState {
        abstract val title: Int
        abstract val icon: Int

        data class DetailsToolbarState(
            @StringRes override val title: Int = R.string.backup_download_details_page_title,
            @DrawableRes override val icon: Int = R.drawable.ic_arrow_back
        ) : ToolbarState()

        data class ProgressToolbarState(
            @StringRes override val title: Int = R.string.backup_download_progress_page_title,
            @DrawableRes override val icon: Int = R.drawable.ic_close_24px
        ) : ToolbarState()

        data class CompleteToolbarState(
            @StringRes override val title: Int = R.string.backup_download_complete_page_title,
            @DrawableRes override val icon: Int = R.drawable.ic_close_24px
        ) : ToolbarState()

        data class ErrorToolbarState(
            @StringRes override val title: Int = R.string.backup_download_complete_failed_title,
            @DrawableRes override val icon: Int = R.drawable.ic_close_24px
        ) : ToolbarState()
    }
}
