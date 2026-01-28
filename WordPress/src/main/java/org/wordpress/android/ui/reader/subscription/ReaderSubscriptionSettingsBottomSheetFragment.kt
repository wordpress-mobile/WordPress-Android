package org.wordpress.android.ui.reader.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

@AndroidEntryPoint
class ReaderSubscriptionSettingsBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var contextProvider: ContextProvider

    @Inject
    lateinit var uiHelpers: UiHelpers

    private val viewModel: ReaderSubscriptionSettingsViewModel by viewModels()

    private var composeView: ComposeView? = null

    override fun getTheme(): Int = R.style.WordPress_BottomSheetDialogTheme_NonTranslucent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).also { composeView = it }.apply {
            setContent {
                AppThemeM3 {
                    val uiState by viewModel.uiState.collectAsState()
                    uiState?.let { state ->
                        ReaderSubscriptionSettingsScreen(
                            uiState = state,
                            onNotifyPostsToggled = viewModel::onNotifyPostsToggled,
                            onEmailPostsToggled = viewModel::onEmailPostsToggled,
                            onEmailCommentsToggled = viewModel::onEmailCommentsToggled
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetBehavior()
        observeSnackbarEvents()

        val blogId = requireArguments().getLong(ARG_BLOG_ID)
        val blogName = requireArguments().getString(ARG_BLOG_NAME, "")
        val blogUrl = requireArguments().getString(ARG_BLOG_URL, "")

        if (savedInstanceState == null) {
            viewModel.start(blogId, blogName, blogUrl)
        }
    }

    private fun setupBottomSheetBehavior() {
        (dialog as? BottomSheetDialog)?.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    private fun observeSnackbarEvents() {
        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { messageHolder ->
            if (!isAdded) return@observeEvent

            composeView?.let { view ->
                WPSnackbar.make(
                    view,
                    uiHelpers.getTextOfUiString(contextProvider.getContext(), messageHolder.message),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }

    companion object {
        const val TAG = "ReaderSubscriptionSettingsBottomSheet"

        private const val ARG_BLOG_ID = "arg_blog_id"
        private const val ARG_BLOG_NAME = "arg_blog_name"
        private const val ARG_BLOG_URL = "arg_blog_url"

        @JvmStatic
        fun newInstance(
            blogId: Long,
            blogName: String,
            blogUrl: String
        ): ReaderSubscriptionSettingsBottomSheetFragment {
            return ReaderSubscriptionSettingsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_BLOG_ID, blogId)
                    putString(ARG_BLOG_NAME, blogName)
                    putString(ARG_BLOG_URL, blogUrl)
                }
            }
        }
    }
}
