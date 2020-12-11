package org.wordpress.android.ui.jetpack.backup

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.ui.jetpack.backup.BackupDownloadStep.COMPLETE
import org.wordpress.android.ui.jetpack.backup.BackupDownloadStep.DETAILS
import org.wordpress.android.ui.jetpack.backup.BackupDownloadStep.PROGRESS
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.SingleEventObservable
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

const val KEY_BACKUP_DOWNLOAD_ACTIVITY_ID_KEY: String = "key_backup_download_activity_id_key"
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

    private val _wizardFinishedObservable = SingleLiveEvent<BackupDownloadWizardState>()
    val wizardFinishedObservable: LiveData<BackupDownloadWizardState> = _wizardFinishedObservable

    private val _screenTitleObservable = SingleLiveEvent<@StringRes Int>()
    val screenTitleObservable: LiveData<Int> = _screenTitleObservable

    private val _exitFlowObservable = SingleLiveEvent<Unit>()
    val exitFlowObservable: LiveData<Unit> = _exitFlowObservable

    private val _onBackPressedObservable = SingleLiveEvent<Unit>()
    val onBackPressedObservable: LiveData<Unit> = _onBackPressedObservable

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        if (savedInstanceState == null) {
            backupDownloadState = BackupDownloadState()
        } else {
            backupDownloadCompleted = savedInstanceState.getBoolean(
                    KEY_BACKUP_DOWNLOAD_COMPLETED,
                    false
            )
            backupDownloadState = requireNotNull(
                    savedInstanceState.getParcelable(
                            KEY_BACKUP_DOWNLOAD_STATE
                    )
            )
            val currentStepIndex = savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
        isStarted = true
        if (savedInstanceState == null) {
            // Show the next step only if it's a fresh activity so we can handle the navigation
            wizardManager.showNextStep()
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
        _exitFlowObservable.call()
    }

    fun setTitle(@StringRes title: Int) {
        _screenTitleObservable.value = title
    }

    sealed class BackupDownloadWizardState : Parcelable {
        @Parcelize
        object BackupDownloadCanceled : BackupDownloadWizardState()

        @Parcelize
        data class BackupDownloadInProgress(val activityId: String) : BackupDownloadWizardState()

        @Parcelize
        data class BackupDownloadCompleted(val activityId: String) : BackupDownloadWizardState()
    }
}
