package org.wordpress.android.ui.voicetocontent

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.util.audio.IAudioRecorder.Companion.REQUIRED_RECORDING_PERMISSIONS

@AndroidEntryPoint
class VoiceToContentDialogFragment : BottomSheetDialogFragment() {
    private val viewModel: VoiceToContentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                VoiceToContentScreen(
                    viewModel = viewModel,
                    onRequestPermission = { requestAllPermissionsForRecording() },
                    hasPermission = { hasAllPermissionsForRecording() }
                )
            }
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val areAllPermissionsGranted = permissions.entries.all { it.value }
        if (areAllPermissionsGranted) {
            viewModel.startRecording()
        } else {
            // todo pantelis handle permissions denied case
            Toast.makeText(context, "Permissions needed for recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasAllPermissionsForRecording(): Boolean {
        return REQUIRED_RECORDING_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestAllPermissionsForRecording() {
        requestMultiplePermissionsLauncher.launch(REQUIRED_RECORDING_PERMISSIONS)
    }

    companion object {
        const val TAG = "voice_to_content_fragment_tag"

        @JvmStatic
        fun newInstance() = VoiceToContentDialogFragment()
    }
}

@Composable
fun VoiceToContentScreen(
    viewModel: VoiceToContentViewModel,
    onRequestPermission: () -> Unit,
    hasPermission: () -> Boolean
) {
    val result by viewModel.uiState.observeAsState()
    val assistantFeature by viewModel.aiAssistantFeatureState.observeAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when {
            result?.isError == true -> {
                Text(text = "Error happened", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            result?.content != null -> {
                Text(text = result?.content!!, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            assistantFeature != null -> {
                Text(text = "Assistant Feature Returned Successfully", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }

            else -> {
                Text(text = "Ready to fake record - tap microphone", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    painterResource(id = R.drawable.ic_mic_white_24dp),
                    contentDescription = "Microphone",
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            if (hasPermission()) {
                                viewModel.startRecording()
                            } else {
                                onRequestPermission()
                            }
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    painterResource(id = com.google.android.exoplayer2.ui.R.drawable.exo_icon_stop),
                    contentDescription = "Stop",
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            viewModel.stopRecording()
                        }
                )
            }
        }
    }
}
