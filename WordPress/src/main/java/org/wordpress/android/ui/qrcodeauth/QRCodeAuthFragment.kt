package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.QrcodeauthFragmentBinding
import org.wordpress.android.ui.compose.components.VerticalScrollBox
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.FinishActivity
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.Idle
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
@Suppress("TooManyFunctions")
class QRCodeAuthFragment : Fragment() {
    @Inject lateinit var uiHelpers: UiHelpers

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
        //TODO @RenanLukas
        initBackPressHandler()
        initViewModel(savedInstanceState)
//        with(QrcodeauthFragmentBinding.bind(view)) {
        //            initBackPressHandler()
//           TODO @RenanLukas observeViewModel()
        //            initViewModel(savedInstanceState)
//        }
    }

    @Composable
    private fun QRCodeAuthScreen(viewModel: QRCodeAuthViewModel = viewModel()) {
        VerticalScrollBox(
                modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                alignment = Alignment.CenterStart
        ) {
            Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
            ) {
                // TODO fix initial action event
                val actionEvent by viewModel.actionEvents.collectAsState(initial = Idle)
                handleActionEvents(actionEvent)
                val uiState by viewModel.uiState.collectAsState()
                uiState.run {
                    when (this) {
                        is Content -> {
                            ContentState(uiState = this)
                        }
                        is Error -> {
                            // TODO applyErrorState(uiState)
                            ErrorState(
                                    imageRes = image,
                                    contentDescriptionRes = R.string.qrcode_auth_flow_error_content_description,
                                    titleText = uiHelpers.getTextOfUiString(LocalContext.current, title).toString(),
                                    subtitleText = uiHelpers.getTextOfUiString(LocalContext.current, subtitle)
                                            .toString(),
                                    primaryButtonText = uiHelpers.getTextOfUiString(
                                            LocalContext.current,
                                            //TODO @RenanLukas fix non-null assertion before submitting PR
                                            primaryActionButton!!.label
                                    ).toString(),
                                    //TODO @RenanLukas fix non-null assertion before submitting PR
                                    primaryButtonClick = primaryActionButton!!.clickAction,
                                    secondaryButtonText = uiHelpers.getTextOfUiString(
                                            LocalContext.current,
                                            //TODO @RenanLukas fix non-null assertion before submitting PR
                                            secondaryActionButton!!.label
                                    ).toString(),
                                    //TODO @RenanLukas fix non-null assertion before submitting PR
                                    secondaryButtonClick = secondaryActionButton!!.clickAction
                            )
                        }
                        is Loading -> {
                            LoadingState()
                        }
                        is Scanning -> {} // NO OP
                    }
                }
            }
        }
    }

    private fun QrcodeauthFragmentBinding.observeViewModel() {
        qrCodeAuthViewModel.actionEvents.onEach { handleActionEvents(it) }.launchIn(viewLifecycleOwner.lifecycleScope)
        dialogViewModel.onInteraction.observeEvent(viewLifecycleOwner) { qrCodeAuthViewModel.onDialogInteraction(it) }
        qrCodeAuthViewModel.uiState.onEach { renderUi(it) }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun initViewModel(savedInstanceState: Bundle?) {
        val(uri, isDeepLink) = requireActivity().intent?.extras?.let {
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

    private fun QrcodeauthFragmentBinding.renderUi(uiState: QRCodeAuthUiState) {
        uiHelpers.updateVisibility(contentLayout.contentContainer, uiState.contentVisibility)
        uiHelpers.updateVisibility(errorLayout.errorContainer, uiState.errorVisibility)
        uiHelpers.updateVisibility(loadingLayout.loadingContainer, uiState.loadingVisibility)
        when (uiState) {
            is Content -> { applyContentState(uiState) }
            is Error -> { applyErrorState(uiState) }
            is Loading -> { } // NO OP
            is Scanning -> { } // NO OP
        }
    }

    private fun QrcodeauthFragmentBinding.applyContentState(uiState: Content) {
        uiHelpers.updateVisibility(contentLayout.contentContainer, uiState.contentVisibility)
        uiHelpers.updateVisibility(contentLayout.progress, uiState.isProgressShowing)
        uiHelpers.setTextOrHide(contentLayout.contentTitle, uiState.title)
        uiHelpers.setTextOrHide(contentLayout.contentSubtitle, uiState.subtitle)
        uiHelpers.setImageOrHide(contentLayout.contentImage, uiState.image)
        contentLayout.contentContainer.alpha = uiState.alpha
        uiState.primaryActionButton?.let { action ->
            uiHelpers.setTextOrHide(contentLayout.contentPrimaryAction, action.label)
            uiHelpers.updateVisibility(contentLayout.contentPrimaryAction, action.isVisible)
            contentLayout.contentPrimaryAction.setOnClickListener { action.clickAction?.invoke() }
            contentLayout.contentPrimaryAction.isEnabled = action.isEnabled
        }
        uiState.secondaryActionButton?.let { action ->
            uiHelpers.setTextOrHide(contentLayout.contentSecondaryAction, action.label)
            uiHelpers.updateVisibility(contentLayout.contentSecondaryAction, action.isVisible)
            contentLayout.contentSecondaryAction.setOnClickListener { action.clickAction?.invoke() }
            contentLayout.contentSecondaryAction.isEnabled = action.isEnabled
        }
    }

    private fun QrcodeauthFragmentBinding.applyErrorState(uiState: Error) {
        uiHelpers.updateVisibility(errorLayout.errorContainer, uiState.errorVisibility)
        uiHelpers.setImageOrHide(errorLayout.errorImage, uiState.image)
        uiHelpers.setTextOrHide(errorLayout.errorTitle, uiState.title)
        uiHelpers.setTextOrHide(errorLayout.errorSubtitle, uiState.subtitle)
        uiState.primaryActionButton?.let { action ->
            uiHelpers.setTextOrHide(errorLayout.errorPrimaryAction, action.label)
            errorLayout.errorPrimaryAction.setOnClickListener { action.clickAction.invoke() }
        }
        uiState.secondaryActionButton?.let { action ->
            uiHelpers.setTextOrHide(errorLayout.errorSecondaryAction, action.label)
            errorLayout.errorSecondaryAction.setOnClickListener { action.clickAction.invoke() }
        }
    }

    private fun launchDismissDialog(model: QRCodeAuthDialogModel) {
        dialogViewModel.showDialog(requireActivity().supportFragmentManager,
                BasicDialogModel(model.tag,
                        getString(model.title),
                        getString(model.message),
                        getString(model.positiveButtonLabel),
                        model.negativeButtonLabel?.let { label -> getString(label) },
                        model.cancelButtonLabel?.let { label -> getString(label) }
                ))
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
                object : OnBackPressedCallback(
                        true
                ) {
                    override fun handleOnBackPressed() {
                        qrCodeAuthViewModel.onBackPressed()
                    }
                })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        qrCodeAuthViewModel.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }
}
