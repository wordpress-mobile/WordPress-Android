package org.wordpress.android.ui.jetpack.restore

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.restore.RestoreStep.DETAILS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.WARNING
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.util.wizard.WizardState
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleEventObservable
import java.util.Date
import javax.inject.Inject

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
    val url: String? = null,
    val errorType: Int? = null
) : WizardState, Parcelable

typealias NavigationTarget = WizardNavigationTarget<RestoreStep, RestoreState>

class RestoreViewModel @Inject constructor(
    private val wizardManager: WizardManager<RestoreStep>
) : ViewModel() {
    private var isStarted = false

    private lateinit var restoreState: RestoreState

    val navigationTargetObservable: SingleEventObservable<NavigationTarget> by lazy {
        SingleEventObservable(
                Transformations.map(wizardManager.navigatorLiveData) {
                    // todo: annmarie when moving back from warning - need state to not clear
                    Log.i(javaClass.simpleName, "***=> in the navigationTargetObserverable")
                    clearOldRestoreState(it)
                    WizardNavigationTarget(it, restoreState)
                }
        )
    }

    private val _wizardFinishedObservable = MutableLiveData<Event<RestoreWizardState>>()
    val wizardFinishedObservable: LiveData<Event<RestoreWizardState>> = _wizardFinishedObservable

    private val _toolbarStateObservable = MutableLiveData<ToolbarState>()
    val toolbarStateObservable: LiveData<ToolbarState> = _toolbarStateObservable

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _errorEvents = MediatorLiveData<Event<RestoreErrorTypes>>()
    val errorEvents: LiveData<Event<RestoreErrorTypes>> = _errorEvents

    private val _navigationEvents = MediatorLiveData<Event<RestoreNavigationEvents>>()
    val navigationEvents: LiveData<Event<RestoreNavigationEvents>> = _navigationEvents

    private val _onBackPressedObservable = MutableLiveData<Unit>()
    val onBackPressedObservable: LiveData<Unit> = _onBackPressedObservable

    fun start(savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true
        if (savedInstanceState == null) {
            restoreState = RestoreState()
            // Show the next step only if it's a fresh activity so we can handle the navigation
            wizardManager.showNextStep()
        } else {
            restoreState = requireNotNull(savedInstanceState.getParcelable(KEY_RESTORE_STATE))
            Log.i(javaClass.simpleName, "***=> instance not null ${restoreState.optionsSelected?.get(0)}")
            val currentStepIndex = savedInstanceState.getInt(KEY_RESTORE_CURRENT_STEP)
            wizardManager.setCurrentStepIndex(currentStepIndex)
        }
    }

    fun addSnackbarMessageSource(snackbarEvents: LiveData<Event<SnackbarMessageHolder>>) {
        _snackbarEvents.addSource(snackbarEvents) { event ->
            _snackbarEvents.value = event
        }
    }

    fun addErrorMessageSource(errorEvents: LiveData<Event<RestoreErrorTypes>>) {
        _errorEvents.addSource(errorEvents) { event ->
            _errorEvents.value = event
        }
    }

    fun writeToBundle(outState: Bundle) {
        outState.putInt(KEY_RESTORE_CURRENT_STEP, wizardManager.currentStep)
        outState.putParcelable(KEY_RESTORE_STATE, restoreState)
    }

    fun onBackPressed() {
        when (wizardManager.currentStep) {
            DETAILS.id -> {
                _wizardFinishedObservable.value = Event(RestoreCanceled)
            }
            WARNING.id -> {
                wizardManager.onBackPressed()
                _onBackPressedObservable.value = null
            }
        }
    }

    private fun clearOldRestoreState(wizardStep: RestoreStep) {
        if (wizardStep == DETAILS) {
            restoreState = restoreState.copy(
                    rewindId = null,
                    restoreId = null,
                    url = null,
                    errorType = null,
                    optionsSelected = null,
                    published = null
            )
        }
    }

    fun onRestoreDetailsFinished(rewindId: String?, optionsSelected: List<Pair<Int, Boolean>>?, published: Date?) {
        restoreState = restoreState.copy(
                rewindId = rewindId,
                optionsSelected = optionsSelected,
                published = published)
        wizardManager.showNextStep()
    }

    fun onRestoreWarningFinished(rewindId: String?, restoreId: Long?) {
        restoreState = restoreState.copy(rewindId = rewindId, restoreId = restoreId)
        _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringText("Warning finished"))))
        // todo: annmarie uncomment the showNextSteps
        // wizardManager.showNextStep()
    }

    fun onRestoreCanceled() {
        _wizardFinishedObservable.value = Event(RestoreCanceled)
    }

    fun onRestoreProgressFinished(url: String?) {
        restoreState = restoreState.copy(url = url)
        wizardManager.showNextStep()
    }

    fun setToolbarState(toolbarState: ToolbarState) {
        _toolbarStateObservable.value = toolbarState
    }

    fun transitionToError(errorType: RestoreErrorTypes) {
        restoreState = restoreState.copy(errorType = errorType.id)
        wizardManager.setCurrentStepIndex(RestoreStep.indexForErrorTransition())
        wizardManager.showNextStep()
    }

    sealed class RestoreWizardState : Parcelable {
        @Parcelize
        object RestoreCanceled : RestoreWizardState()

        @Parcelize
        data class RestoreInProgress(val restoreId: Long) : RestoreWizardState()

        @Parcelize
        object RestoreCompleted : RestoreWizardState()
    }

    sealed class ToolbarState {
        abstract val title: Int
        abstract val icon: Int

        data class DetailsToolbarState(
            @StringRes override val title: Int = R.string.restore_details_page_title,
            @DrawableRes override val icon: Int = R.drawable.ic_arrow_back
        ) : ToolbarState()

        data class WarningToolbarState(
            @StringRes override val title: Int = R.string.restore_warning_page_title,
            @DrawableRes override val icon: Int = R.drawable.ic_arrow_back
        ) : ToolbarState()
    }
}
