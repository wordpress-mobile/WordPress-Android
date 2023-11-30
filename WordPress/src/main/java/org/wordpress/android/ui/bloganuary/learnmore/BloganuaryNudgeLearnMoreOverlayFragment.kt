package org.wordpress.android.ui.bloganuary.learnmore

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme

@AndroidEntryPoint
class BloganuaryNudgeLearnMoreOverlayFragment : BottomSheetDialogFragment() {
    private val viewModel: BloganuaryNudgeLearnMoreOverlayViewModel by viewModels()

    private val isPromptsEnabled: Boolean by lazy {
        arguments?.getBoolean(ARG_IS_PROMPTS_ENABLED) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
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
        viewModel.dismissDialog.observe(viewLifecycleOwner) { dismiss() }
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
