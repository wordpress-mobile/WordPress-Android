package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.reader.subfilter.ActionType
import org.wordpress.android.ui.reader.subfilter.SubfilterBottomSheetEmptyUiState.VisibleEmptyUiState
import org.wordpress.android.ui.reader.views.compose.ReaderSheetHandle
import org.wordpress.android.ui.reader.views.compose.subfilter.SubfilterEmptyState
import org.wordpress.android.ui.utils.UiString.UiStringRes

/**
 * A bottom sheet that prompts the user to log in to WordPress.com.
 * Used when a logged-out user tries to perform actions that require authentication,
 * such as subscribing to a blog.
 *
 * It shows the same empty state as the logged-out filter sheet, to keep the two consistent.
 */
class ReaderLoginRequiredBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ReaderLoginRequiredBottomSheetFragment"

        fun newInstance(): ReaderLoginRequiredBottomSheetFragment {
            return ReaderLoginRequiredBottomSheetFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val state = VisibleEmptyUiState(
            text = UiStringRes(R.string.reader_filter_self_hosted_empty_blogs_list),
            primaryButton = VisibleEmptyUiState.Button(
                text = UiStringRes(R.string.reader_filter_self_hosted_empty_sites_tags_action),
                action = ActionType.OpenLoginPage
            )
        )
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppThemeM3 {
                    Column {
                        ReaderSheetHandle()
                        SubfilterEmptyState(state) {
                            ActivityLauncher.showMainActivityAndMeScreen(context)
                            dismiss()
                        }
                    }
                }
            }
        }
    }
}
