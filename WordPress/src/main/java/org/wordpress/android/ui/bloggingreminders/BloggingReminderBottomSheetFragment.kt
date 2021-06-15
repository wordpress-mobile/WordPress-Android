package org.wordpress.android.ui.bloggingreminders

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.RecyclerViewBottomSheetBinding
import javax.inject.Inject

class BloggingReminderBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var adapter: BloggingRemindersAdapter
    private lateinit var viewModel: BloggingRemindersViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.recycler_view_bottom_sheet, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(RecyclerViewBottomSheetBinding.bind(view)) {
            contentRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
            contentRecyclerView.adapter = adapter

            viewModel =
                ViewModelProvider(requireActivity(), viewModelFactory).get(BloggingRemindersViewModel::class.java)
            viewModel.uiState.observe(this@BloggingReminderBottomSheetFragment) {
                (contentRecyclerView.adapter as? BloggingRemindersAdapter)?.update(it ?: listOf())
            }

            savedInstanceState?.let { viewModel.restoreState(it) }

            (dialog as? BottomSheetDialog)?.apply {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
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
