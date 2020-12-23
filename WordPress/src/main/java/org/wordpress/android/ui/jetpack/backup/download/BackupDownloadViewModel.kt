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
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleEventObservable
import javax.inject.Inject

const val KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY = "key_backup_download_activity_id_key"
const val KEY_BACKUP_DOWNLOAD_CURRENT_STEP = "key_backup_download_current_step"
const val KEY_BACKUP_DOWNLOAD_COMPLETED = "key_backup_download_completed"
const val KEY_BACKUP_DOWNLOAD_STATE = "key_backup_download_state"

@Parcelize
@SuppressLint("ParcelCreator")
data class BackupDownloadState(
    val siteId: Long? = null,
    val rewindId: Long? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<BackupDownloadStep, BackupDownloadState>

class BackupDownloadViewModel @Inject constructor(
    private val wizardManager: WizardManager<BackupDownloadStep>
) : ViewModel() {
    private var isStarted = false
    private var backupDownloadCompleted = false

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

    private val _exitFlowObservable = MutableLiveData<Event<Unit>>()
    val exitFlowObservable: LiveData<Event<Unit>> = _exitFlowObservable

    private val _onBackPressedObservable = MutableLiveData<Event<Unit>>()
    val onBackPressedObservable: LiveData<Event<Unit>> = _onBackPressedObservable

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true
        if (savedInstanceState == null) {
            backupDownloadState = BackupDownloadState()
            // Show the next step only if it's a fresh activity so we can handle the navigation
            wizardManager.showNextStep()
        } else {
            backupDownloadCompleted = savedInstanceState.getBoolean(KEY_BACKUP_DOWNLOAD_COMPLETED, false)
            backupDownloadState = requireNotNull(savedInstanceState.getParcelable(KEY_BACKUP_DOWNLOAD_STATE)
            )
            val currentStepIndex = savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
    }

    fun addSnackbarMessageSource(snackbarEvents: LiveData<Event<SnackbarMessageHolder>>) {
        _snackbarEvents.addSource(snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }
    fun writeToBundle(outState: Bundle) {
        outState.putBoolean(KEY_BACKUP_DOWNLOAD_COMPLETED, backupDownloadCompleted)
        outState.putInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_BACKUP_DOWNLOAD_STATE, backupDownloadState)
    }

    fun onBackPressed() {
        return exitFlow()
        // todo: annmarie - what should happen on backPress - always exit, please revisit
    }

    private fun clearOldBackupDownloadState(wizardStep: BackupDownloadStep) {
        when (wizardStep) {
            DETAILS -> backupDownloadState.rewindId?.let {
                backupDownloadState = backupDownloadState.copy(rewindId = null)
            }
            PROGRESS -> backupDownloadState.rewindId?.let {
                backupDownloadState = backupDownloadState.copy(rewindId = null)
            }
            COMPLETE -> {
            } // intentionally left empty
        }
    }

    private fun exitFlow() {
        _exitFlowObservable.value = Event(Unit)
    }

    fun setToolbarState(toolbarState: ToolbarState) {
        _toolbarStateObservable.value = toolbarState
    }

    sealed class BackupDownloadWizardState : Parcelable {
        @Parcelize
        object BackupDownloadCanceled : BackupDownloadWizardState()

        @Parcelize
        data class BackupDownloadInProgress(val activityId: String) : BackupDownloadWizardState()

        @Parcelize
        data class BackupDownloadCompleted(val activityId: String) : BackupDownloadWizardState()
    }

    sealed class ToolbarState {
        abstract val title: Int
        abstract val icon: Int

        data class DetailsToolbarState(
            @StringRes override val title: Int = R.string.backup_download_details_page_title,
            @DrawableRes override val icon: Int = R.drawable.ic_arrow_back
        ) : ToolbarState()
    }
}
