package org.wordpress.android.ui.voicetocontent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.R
import org.wordpress.android.util.audio.IAudioRecorder.Companion.REQUIRED_RECORDING_PERMISSIONS
import android.provider.Settings
import androidx.compose.material.ExperimentalMaterialApi

@AndroidEntryPoint
class VoiceToContentDialogFragment : BottomSheetDialogFragment() {
    private val viewModel: VoiceToContentViewModel by viewModels()

    @ExperimentalMaterialApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                VoiceToContentScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        viewModel.start()
    }

    private fun observeViewModel() {
        viewModel.requestPermission.observe(viewLifecycleOwner) {
            requestAllPermissionsForRecording()
        }

        viewModel.dismiss.observe(viewLifecycleOwner) {
            dismiss()
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val areAllPermissionsGranted = permissions.entries.all { it.value }
        if (areAllPermissionsGranted) {
            viewModel.onPermissionGranted()
        } else {
            // Check if any permissions were denied permanently
            if (permissions.entries.any { !it.value }) {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun requestAllPermissionsForRecording() {
        requestMultiplePermissionsLauncher.launch(REQUIRED_RECORDING_PERMISSIONS)
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.voice_to_content_permissions_required_title)
            .setMessage(R.string.voice_to_content_permissions_required_msg)
            .setPositiveButton("Settings") { _, _ ->
                // Open the app's settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val TAG = "voice_to_content_fragment_tag"

        @JvmStatic
        fun newInstance() = VoiceToContentDialogFragment()
    }
}
