package org.wordpress.android.ui.bloggingreminders

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.RecyclerViewPrimaryButtonBottomSheetBinding
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class BloggingReminderBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var adapter: BloggingRemindersAdapter
    @Inject lateinit var uiHelpers: UiHelpers
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

            viewModel =
                    ViewModelProvider(requireActivity(), viewModelFactory).get(BloggingRemindersViewModel::class.java)
            viewModel.uiState.observe(this@BloggingReminderBottomSheetFragment) { uiState ->
                (contentRecyclerView.adapter as? BloggingRemindersAdapter)?.update(uiState?.uiItems ?: listOf())
                if (uiState?.primaryButton != null) {
                    primaryButton.visibility = View.VISIBLE
                    uiHelpers.setTextOrHide(primaryButton, uiState.primaryButton.text)
                    primaryButton.setOnClickListener { uiState.primaryButton.onClick.click() }
                    primaryButton.isEnabled = uiState.primaryButton.enabled
                } else {
                    primaryButton.visibility = View.GONE
                }
            }

            savedInstanceState?.let { viewModel.restoreState(it) }

            (dialog as? BottomSheetDialog)?.apply {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.addBottomSheetCallback(object : BottomSheetCallback() {
                    override fun onStateChanged(view: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                            behavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }

                    override fun onSlide(view: View, v: Float) {
                        // Not needed
                    }
                })
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

    companion object {
        @JvmStatic
        fun newInstance() = BloggingReminderBottomSheetFragment()
    }
}
