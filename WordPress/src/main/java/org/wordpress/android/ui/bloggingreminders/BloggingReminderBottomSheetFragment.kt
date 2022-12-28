package org.wordpress.android.ui.bloggingreminders

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.RecyclerViewPrimaryButtonBottomSheetBinding
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.disableAnimation
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class BloggingReminderBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var adapter: BloggingRemindersAdapter
    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: BloggingRemindersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.recycler_view_primary_button_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(RecyclerViewPrimaryButtonBottomSheetBinding.bind(view)) {
            contentRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
            contentRecyclerView.adapter = adapter
            contentRecyclerView.disableAnimation()
            contentRecyclerView.addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (recyclerView.canScrollVertically(1)) {
                        // Show shadow when not at the bottom
                        bottomShadow.animate().alpha(1.0f).start()
                    } else {
                        // Hide shadow at the bottom
                        bottomShadow.animate().alpha(0.0f).start()
                    }
                }
            })
            viewModel =
                ViewModelProvider(requireActivity(), viewModelFactory).get(BloggingRemindersViewModel::class.java)
            viewModel.uiState.observe(this@BloggingReminderBottomSheetFragment) { uiState ->
                (contentRecyclerView.adapter as? BloggingRemindersAdapter)?.submitList(uiState?.uiItems ?: listOf())
                if (uiState?.primaryButton != null) {
                    primaryButton.visibility = View.VISIBLE
                    uiHelpers.setTextOrHide(primaryButton, uiState.primaryButton.text)
                    primaryButton.setOnClickListener { uiState.primaryButton.onClick.click() }
                    primaryButton.isEnabled = uiState.primaryButton.enabled
                } else {
                    primaryButton.visibility = View.GONE
                }
            }

            viewModel.showBloggingPromptHelpDialogVisible.observeEvent(viewLifecycleOwner) { isShowing: Boolean ->
                if (isShowing) {
                    BloggingPromptsOnboardingDialogFragment.newInstance(INFORMATION).show(
                        parentFragmentManager, BloggingPromptsOnboardingDialogFragment.TAG
                    )
                }
            }

            BloggingReminderUtils.observeTimePicker(
                viewModel.isTimePickerShowing,
                viewLifecycleOwner,
                BloggingReminderTimePicker.TAG,
                {
                    requireActivity().supportFragmentManager
                }
            )

            savedInstanceState?.let { viewModel.restoreState(it) }

            (dialog as? BottomSheetDialog)?.apply {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveState(outState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onBottomSheetDismissed()
    }

    companion object {
        const val TAG = "BLOGGING_REMINDER_BOTTOM_SHEET_FRAGMENT"
    }
}
