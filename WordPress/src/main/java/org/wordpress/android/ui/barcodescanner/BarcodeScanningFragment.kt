package org.wordpress.android.ui.barcodescanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.ui.compose.theme.AppTheme
import javax.inject.Inject

@AndroidEntryPoint
class BarcodeScanningFragment : Fragment() {
    private val viewModel: BarcodeScanningViewModel by viewModels()

    @Inject
    lateinit var codeScanner: GoogleMLKitCodeScanner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(javaClass.simpleName, "***=> onViewCreated")
        view as ComposeView
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        observeCameraPermissionState(view)
        observeViewModelEvents()
    }

    private fun observeCameraPermissionState(view: ComposeView) {
        Log.i(javaClass.simpleName, "***=> observeCameraPermissionState")
        viewModel.permissionState.observe(viewLifecycleOwner) { permissionState ->
            view.setContent {
                AppTheme {
                    BarcodeScannerScreen(
                        codeScanner = codeScanner,
                        permissionState = permissionState,
                        onResult = { granted ->
                            viewModel.updatePermissionState(
                                granted,
                                shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                            )
                        },
                        onScannedResult = { codeScannerStatus ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                                    codeScannerStatus.collect { status ->
                                        Log.i(javaClass.simpleName, "***=> onScannedResult $status")
                                        // todo: annmarie this should finish with result
                                        val resultIntent = Intent().apply {
                                            // Add any data you want to send back to the calling activity
                                            putExtra(KEY_BARCODE_SCANNING_SCAN_STATUS, status)
                                        }

                                        // Set the result code and the Intent containing the result data
                                        requireActivity().setResult(Activity.RESULT_OK, resultIntent)

                                        // Finish the activity
                                        requireActivity().finish()
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
    private fun observeViewModelEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission -> {
                    Log.i(javaClass.simpleName, "***=> LaunchCameraPermission")
                    event.cameraLauncher.launch(KEY_CAMERA_PERMISSION)
                }

                is BarcodeScanningViewModel.ScanningEvents.OpenAppSettings -> {
                   Log.i(javaClass.simpleName, "***=> OpenAppSettings")
                    // todo: annmarie  do we have a utils method to showAppSettings
                }

                is BarcodeScanningViewModel.ScanningEvents.Exit -> {
                    Log.i(javaClass.simpleName, "***=> Exit")
                    // todo: annmarie go home findNavController().navigateUp()
                }
            }
        }
    }

    companion object {
        const val KEY_BARCODE_SCANNING_SCAN_STATUS = "barcode_scanning_scan_status"
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
