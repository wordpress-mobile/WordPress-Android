package org.wordpress.android.ui.qrcodeauth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.databinding.QrcodeauthFragmentBinding
import org.wordpress.android.ui.posts.BasicDialogViewModel
import org.wordpress.android.ui.posts.BasicDialogViewModel.BasicDialogModel
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.FinishActivity
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchDismissDialog
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthActionEvent.LaunchScanner
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Content
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Error
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Loading
import org.wordpress.android.ui.qrcodeauth.QRCodeAuthUiState.Scanning
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class QRCodeAuthFragment : Fragment(R.layout.qrcodeauth_fragment) {
    @Inject lateinit var uiHelpers: UiHelpers

    private val viewModel: QRCodeAuthViewModel by viewModels()
    private val dialogViewModel: BasicDialogViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(QrcodeauthFragmentBinding.bind(view)) {
            initBackPressHandler()
            observeViewModel()
            startViewModel(savedInstanceState)
        }
    }

    private fun QrcodeauthFragmentBinding.observeViewModel() {
        viewModel.actionEvents.onEach { handleActionEvents(it) }.launchIn(viewLifecycleOwner.lifecycleScope)
        dialogViewModel.onInteraction.observeEvent(viewLifecycleOwner) { viewModel.onDialogInteraction(it) }
        viewModel.uiState.onEach { renderUi(it) }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun startViewModel(savedInstanceState: Bundle?) {
        viewModel.start(savedInstanceState)
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
        val scanner = GmsBarcodeScanning.getClient(requireContext())
        scanner.startScan()
                .addOnSuccessListener { barcode -> viewModel.onScanSuccess(barcode.rawValue) }
                .addOnFailureListener { viewModel.onScanFailure() }
    }

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(
                        true
                ) {
                    override fun handleOnBackPressed() {
                        viewModel.onBackPressed()
                    }
                })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.writeToBundle(outState)
        super.onSaveInstanceState(outState)
    }
}
