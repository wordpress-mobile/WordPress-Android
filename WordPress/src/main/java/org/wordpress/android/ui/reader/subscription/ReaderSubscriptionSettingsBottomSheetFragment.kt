package org.wordpress.android.ui.reader.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.databinding.ReaderSubscriptionSettingsBottomSheetBinding
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

    private var _binding: ReaderSubscriptionSettingsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.WordPress_BottomSheetDialogTheme_NonTranslucent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ReaderSubscriptionSettingsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetBehavior()
        setupClickListeners()
        observeViewModel()

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

    private fun setupClickListeners() {
        with(binding) {
            switchNotifyPosts.setOnClickListener {
                viewModel.onNotifyPostsToggled((it as SwitchCompat).isChecked)
            }

            switchEmailPosts.setOnClickListener {
                viewModel.onEmailPostsToggled((it as SwitchCompat).isChecked)
            }

            switchEmailComments.setOnClickListener {
                viewModel.onEmailCommentsToggled((it as SwitchCompat).isChecked)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state?.let { updateUi(it) }
                }
            }
        }

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { messageHolder ->
            if (!isAdded) return@observeEvent

            WPSnackbar.make(
                binding.coordinator,
                uiHelpers.getTextOfUiString(contextProvider.getContext(), messageHolder.message),
                Snackbar.LENGTH_LONG
            ).show()
        }

        viewModel.dismiss.observeEvent(viewLifecycleOwner) {
            dismiss()
        }
    }

    private fun updateUi(state: ReaderSubscriptionSettingsUiState) {
        with(binding) {
            subscriptionSettingsBlogName.text = state.blogUrl.ifEmpty { state.blogName }

            // Update switches without triggering click listeners
            switchNotifyPosts.setCheckedSilently(state.notifyPostsEnabled)
            switchEmailPosts.setCheckedSilently(state.emailPostsEnabled)
            switchEmailComments.setCheckedSilently(state.emailCommentsEnabled)

            // Update loading state
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            switchNotifyPosts.isEnabled = !state.isLoading
            switchEmailPosts.isEnabled = !state.isLoading
            switchEmailComments.isEnabled = !state.isLoading
        }
    }

    private fun SwitchCompat.setCheckedSilently(checked: Boolean) {
        if (isChecked != checked) {
            setOnClickListener(null)
            isChecked = checked
            setupClickListeners()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
