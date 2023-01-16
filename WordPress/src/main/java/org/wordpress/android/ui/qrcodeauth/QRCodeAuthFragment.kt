package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.compose.components.VerticalScrollBox
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.FinishActivity
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchDismissDialog
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchScanner
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActivity.Companion.DEEP_LINK_URI_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActivity.Companion.IS_DEEP_LINK_KEY
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Loading
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Scanning
import org.wordpress.android.ui.qrcodeauth.compose.state.ContentState
import org.wordpress.android.ui.qrcodeauth.compose.state.ErrorState
import org.wordpress.android.ui.qrcodeauth.compose.state.LoadingState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class QRCodeAuthFragment : Fragment() {
    @Inject
    lateinit var uiHelpers: UiHelpers

    private val qrCodeAuthViewModel: QRCodeAuthViewModel by viewModels()
    private val dialogViewModel: BasicDialogViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                QRCodeAuthScreen()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBackPressHandler()
        initViewModel(savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        qrCodeAuthViewModel.actionEvents.onEach(this::handleActionEvents).launchIn(viewLifecycleOwner.lifecycleScope)
        dialogViewModel.onInteraction.observeEvent(viewLifecycleOwner, qrCodeAuthViewModel::onDialogInteraction)
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        val (uri, isDeepLink) = requireActivity().intent?.extras?.let {
            val uri = it.getString(DEEP_LINK_URI_KEY, null)
            val isDeepLink = it.getBoolean(IS_DEEP_LINK_KEY, false)
            uri to isDeepLink
        } ?: (null to false)

        qrCodeAuthViewModel.start(uri, isDeepLink, savedInstanceState)
    }

    private fun handleActionEvents(actionEvent: QRCodeAuthActionEvent) {
        when (actionEvent) {
            is LaunchDismissDialog -> launchDismissDialog(actionEvent.dialogModel)
            is LaunchScanner -> launchScanner()
            is FinishActivity -> requireActivity().finish()
        }
    }

    private fun launchDismissDialog(model: QRCodeAuthDialogModel) {
        dialogViewModel.showDialog(
            requireActivity().supportFragmentManager,
            BasicDialogModel(
                model.tag,
                getString(model.title),
                getString(model.message),
                getString(model.positiveButtonLabel),
                model.negativeButtonLabel?.let { label -> getString(label) },
                model.cancelButtonLabel?.let { label -> getString(label) }
            )
        )
    }

    private fun launchScanner() {
        qrCodeAuthViewModel.track(Stat.QRLOGIN_SCANNER_DISPLAYED)
        val scanner = GmsBarcodeScanning.getClient(requireContext())
        scanner.startScan()
            .addOnSuccessListener { barcode -> qrCodeAuthViewModel.onScanSuccess(barcode.rawValue) }
            .addOnFailureListener { qrCodeAuthViewModel.onScanFailure() }
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    qrCodeAuthViewModel.onBackPressed()
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        qrCodeAuthViewModel.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }
}

@Composable
private fun QRCodeAuthScreen(viewModel: QRCodeAuthViewModel = viewModel()) {
    VerticalScrollBox(
        alignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxSize()
    ) {
        val uiState by viewModel.uiState.collectAsState()

        @Suppress("UnnecessaryVariable") // See: https://stackoverflow.com/a/69558316/4129245
        when (val state = uiState) {
            is Content -> ContentState(state)
            is Error -> ErrorState(state)
            is Loading -> LoadingState()
            is Scanning -> Unit
        }
    }
}
