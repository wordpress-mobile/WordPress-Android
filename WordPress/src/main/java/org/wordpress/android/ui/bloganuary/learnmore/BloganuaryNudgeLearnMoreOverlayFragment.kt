package org.wordpress.android.ui.bloganuary.learnmore

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.util.extensions.fillScreen
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel
import javax.inject.Inject

@AndroidEntryPoint
class BloganuaryNudgeLearnMoreOverlayFragment : BottomSheetDialogFragment() {
    private val viewModel: BloganuaryNudgeLearnMoreOverlayViewModel by viewModels()

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val wpMainActivityViewModel by lazy {
        ViewModelProvider(requireActivity(), viewModelFactory)[WPMainActivityViewModel::class.java]
    }

    private val isPromptsEnabled: Boolean by lazy {
        arguments?.getBoolean(ARG_IS_PROMPTS_ENABLED) ?: false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            (this as? BottomSheetDialog)?.fillScreen(isDraggable = true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeM2 {
                    BloganuaryNudgeLearnMoreOverlay(
                        model = viewModel.getUiState(isPromptsEnabled),
                        onActionClick = viewModel::onActionClick,
                        onCloseClick = viewModel::onCloseClick,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dismissDialog.observe(viewLifecycleOwner) {
            dismiss()
            if (it.refreshDashboard) wpMainActivityViewModel.requestMySiteDashboardRefresh()
        }
        viewModel.onDialogShown(isPromptsEnabled)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onDialogDismissed()
    }

    companion object {
        const val TAG = "BloganuaryNudgeLearnMoreOverlayFragment"
        private const val ARG_IS_PROMPTS_ENABLED = "isPromptsEnabled"

        fun newInstance(isPromptsEnabled: Boolean) = BloganuaryNudgeLearnMoreOverlayFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_PROMPTS_ENABLED, isPromptsEnabled)
            }
        }
    }
}
