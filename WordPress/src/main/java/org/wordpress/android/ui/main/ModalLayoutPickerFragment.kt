package org.wordpress.android.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.modal_layout_picker_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.main.ModalLayoutPickerViewModel
import javax.inject.Inject

/**
 * Implements the Modal Layout Picker UI based on the [BottomSheetDialogFragment] to inherit the container behavior
 */
class ModalLayoutPickerFragment : BottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ModalLayoutPickerViewModel

    companion object {
        const val MODAL_LAYOUT_PICKER_TAG = "MODAL_LAYOUT_PICKER_TAG"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.modal_layout_picker_fragment, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.content_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        recyclerView.adapter = ModalLayoutPickerAdapter(requireActivity())

        // View model binding

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(ModalLayoutPickerViewModel::class.java)

        viewModel.listItems.observe(this, Observer {
            (dialog?.content_recycler_view?.adapter as? ModalLayoutPickerAdapter)?.update(it ?: listOf())
        })

        viewModel.isHeaderVisible.observe(this,
                Observer { event: Event<Boolean> ->
                    event.applyIfNotHandled {
                        title.setVisible(this)
                    }
                }
        )

        // Event handling

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            /**
             * We track the first row visibility to show/hide the header title accordingly
             */
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstItem = layoutManager.findFirstCompletelyVisibleItemPosition()
                viewModel.setHeaderTitleVisibility(firstItem > 0)
            }
        })

        backButton.setOnClickListener { viewModel.dismiss() }

        createBlankPageButton.setOnClickListener { viewModel.createPage() }
    }

    /**
     * Overriding this method allows us to set the dialog to fill the screen
     */
    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), getTheme()).apply {
        setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout =
                    bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
