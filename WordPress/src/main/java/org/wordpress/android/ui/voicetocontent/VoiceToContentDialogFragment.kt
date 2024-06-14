package org.wordpress.android.ui.voicetocontent

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
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
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.material.ExperimentalMaterialApi
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wordpress.android.ui.ActivityNavigator
import javax.inject.Inject

@AndroidEntryPoint
class VoiceToContentDialogFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet: FrameLayout = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.isDraggable = true
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                @SuppressLint("SwitchIntDef")
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN,
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            onBottomSheetClosed()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Handle the slide offset if needed
                }
            })

            // Disable touch interception by the bottom sheet to allow nested scrolling
            bottomSheet.setOnTouchListener { _, _ -> false }
        }

        // Observe the ViewModel to update the cancelable state of closing on outside touch
        viewModel.isCancelableOutsideTouch.observe(this) { cancelable ->
            Log.i(javaClass.simpleName, "***=> disable outside touch")
            dialog.setCanceledOnTouchOutside(cancelable)
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onBottomSheetClosed()
    }

    private fun onBottomSheetClosed() {
        dismiss()
    }

    private fun observeViewModel() {
        viewModel.requestPermission.observe(viewLifecycleOwner) {
            requestAllPermissionsForRecording()
        }

        viewModel.dismiss.observe(viewLifecycleOwner) {
            dismiss()
        }

        viewModel.onIneligibleForVoiceToContent.observe(viewLifecycleOwner) { url ->
            launchIneligibleForVoiceToContent(url)
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

    private fun launchIneligibleForVoiceToContent(url: String) {
        context?.let {
            activityNavigator.openIneligibleForVoiceToContent(it, url)
        }
    }

    companion object {
        const val TAG = "voice_to_content_fragment_tag"

        @JvmStatic
        fun newInstance() = VoiceToContentDialogFragment()
    }
}
