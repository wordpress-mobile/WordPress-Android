package org.wordpress.android.ui.barcodescanner

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
// import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.viewmodel.ScopedViewModel
// todo: annmarie remove these
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class BarcodeScanningViewModel @Inject constructor(
    // savedState: SavedStateHandle,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    // todo: annmarie implement the savedStateHandle - WHAT THE HELL IS THIS?
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    // todo: annmarie figure out a way to navigate events here - need to figure out who listens to this!!
    private val _event: MutableLiveData<ScanningEvents> = MutableLiveData()
    val event: LiveData<ScanningEvents> = _event

    init {
        _permissionState.value = PermissionState.Unknown
    }

    fun updatePermissionState(
        isPermissionGranted: Boolean,
        shouldShowRequestPermissionRationale: Boolean,
    ) {
        when {
            isPermissionGranted -> {
                // display scanning screen
                _permissionState.value = PermissionState.Granted
            }

            // todo: annmarie these triggerEvents ???
            // It will launch events that some place can response to
            // Note: the event may need to be a multiple live event
            shouldShowRequestPermissionRationale -> {
                // Denied once, ask to grant camera permission
                _permissionState.value = PermissionState.ShouldShowRationale(
                    title = R.string.barcode_scanning_alert_dialog_title,
                    message = R.string.barcode_scanning_alert_dialog_rationale_message,
                    ctaLabel = R.string.barcode_scanning_alert_dialog_rationale_cta_label,
                    dismissCtaLabel = R.string.barcode_scanning_alert_dialog_dismiss_label,
                    ctaAction = {  _event.value = ScanningEvents.LaunchCameraPermission(it) },
                    dismissCtaAction = {
                        _event.value = (ScanningEvents.Exit)
                    }
                )
            }

            else -> {
                // Permanently denied, ask to enable permission from the app settings
                _permissionState.value = PermissionState.PermanentlyDenied(
                    title = R.string.barcode_scanning_alert_dialog_title,
                    message = R.string.barcode_scanning_alert_dialog_permanently_denied_message,
                    ctaLabel = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label,
                    dismissCtaLabel = R.string.barcode_scanning_alert_dialog_dismiss_label,
                    ctaAction = {
                        _event.value = ScanningEvents.OpenAppSettings(it)
                    },
                    dismissCtaAction = {
                        _event.value = (ScanningEvents.Exit)
                    }
                )
            }
        }
    }

    sealed class ScanningEvents {
        data class LaunchCameraPermission(
            val cameraLauncher: ManagedActivityResultLauncher<String, Boolean>
        ) : ScanningEvents()

        data class OpenAppSettings(
            val cameraLauncher: ManagedActivityResultLauncher<String, Boolean>
        ) : ScanningEvents()

        object Exit : ScanningEvents()
    }

    sealed class PermissionState {
        object Granted : PermissionState()

        data class ShouldShowRationale(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @StringRes val ctaLabel: Int,
            @StringRes val dismissCtaLabel: Int,
            val ctaAction: (ManagedActivityResultLauncher<String, Boolean>) -> Unit,
            val dismissCtaAction: () -> Unit,
        ) : PermissionState()

        data class PermanentlyDenied(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @StringRes val ctaLabel: Int,
            @StringRes val dismissCtaLabel: Int,
            val ctaAction: (ManagedActivityResultLauncher<String, Boolean>) -> Unit,
            val dismissCtaAction: () -> Unit,
        ) : PermissionState()

        object Unknown : PermissionState()
    }
}
