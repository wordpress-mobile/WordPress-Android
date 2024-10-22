package org.wordpress.android.ui.barcodescanner

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.util.WPPermissionUtils
import javax.inject.Inject

@AndroidEntryPoint
class BarcodeScanningFragment : Fragment() {
    private val viewModel: BarcodeScanningViewModel by viewModels()

    @Inject
    lateinit var codeScanner: GoogleMLKitCodeScanner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view as ComposeView
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        observeCameraPermissionState(view)
        observeViewModelEvents()
        initBackPressHandler()
    }

    private fun observeCameraPermissionState(view: ComposeView) {
        viewModel.permissionState.observe(viewLifecycleOwner) { permissionState ->
            view.setContent {
                AppThemeM2 {
                    BarcodeScannerScreen(
                        codeScanner = codeScanner,
                        permissionState = permissionState,
                        onResult = { granted ->
                            viewModel.updatePermissionState(
                                granted,
                                shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                            )
                        },
                        onScannedResult = object : CodeScannerCallback {
                            override fun run(status: CodeScannerStatus?) {
                                if (status != null) {
                                    setResultAndPopStack(status)
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
                    event.cameraLauncher.launch(KEY_CAMERA_PERMISSION)
                }

                is BarcodeScanningViewModel.ScanningEvents.OpenAppSettings -> {
                    WPPermissionUtils.showAppSettings(requireContext())
                }

                is BarcodeScanningViewModel.ScanningEvents.Exit -> {
                    setResultAndPopStack(CodeScannerStatus.Exit)
                }
            }
        }
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            setResultAndPopStack(CodeScannerStatus.NavigateUp) }
    }

    private fun setResultAndPopStack(status: CodeScannerStatus) {
        if (isAdded) {
            setFragmentResult(KEY_BARCODE_SCANNING_REQUEST, bundleOf(KEY_BARCODE_SCANNING_SCAN_STATUS to status))
            requireActivity().supportFragmentManager.popBackStackImmediate()
        }
    }

    companion object {
        const val KEY_BARCODE_SCANNING_SCAN_STATUS = "barcode_scanning_scan_status"
        const val KEY_BARCODE_SCANNING_REQUEST = "key_barcode_scanning_request"
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
