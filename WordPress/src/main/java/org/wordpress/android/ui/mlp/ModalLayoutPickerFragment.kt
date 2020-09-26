package org.wordpress.android.ui.mlp

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.modal_layout_picker_bottom_toolbar.*
import kotlinx.android.synthetic.main.modal_layout_picker_categories_skeleton.*
import kotlinx.android.synthetic.main.modal_layout_picker_fragment.*
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_skeleton.*
import kotlinx.android.synthetic.main.modal_layout_picker_title_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_titlebar.*
import kotlinx.android.synthetic.main.modal_layout_picker_titlebar.title
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ContentUiState
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ErrorUiState
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.LoadingUiState
import javax.inject.Inject

/**
 * Implements the Modal Layout Picker UI based on the [BottomSheetDialogFragment] to inherit the container behavior
 */
class ModalLayoutPickerFragment : BottomSheetDialogFragment() {
    @Inject internal lateinit var uiHelper: UiHelpers
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

        categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.HORIZONTAL,
                    false
            )
            setRecycledViewPool(RecyclerView.RecycledViewPool())
            adapter = CategoriesAdapter()
            ViewCompat.setNestedScrollingEnabled(this, false)
        }

        layoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = LayoutCategoryAdapter()
        }

        backButton.setOnClickListener {
            closeModal()
        }

        createBlankPageButton.setOnClickListener {
            viewModel.onCreatePageClicked()
        }
        createPageButton.setOnClickListener {
            viewModel.onCreatePageClicked()
        }
        previewButton.setOnClickListener { /* TODO */ }

        setScrollListener()

        setupViewModel()
    }

    private fun setScrollListener() {
        if (DisplayUtils.isLandscape(requireContext())) return // Always visible
        val scrollThreshold = resources.getDimension(R.dimen.mlp_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
    }

    /**
     * Sets the header and title visibility
     * @param visible if true the title is shown and the header is hidden
     */
    private fun setTitleVisibility(visible: Boolean) {
        if (visible == (title.visibility == View.VISIBLE)) return // No change
        title?.let { uiHelper.setInvisible(it, !visible) }
        header?.let { uiHelper.setInvisible(it, visible) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), getTheme()).apply {
        fillTheScreen(this)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        closeModal()
    }

    private fun closeModal() {
        viewModel.dismiss()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(ModalLayoutPickerViewModel::class.java)

        viewModel.start(DisplayUtils.isLandscape(requireContext()))

        viewModel.uiState.observe(this, Observer { uiState ->
            when (uiState) {
                is LoadingUiState -> {
                    setTitleVisibility(uiState.isHeaderVisible)
                    showLoadingSkeleton(uiState.loadingSkeletonVisible)
                }
                is ContentUiState -> {
                    (categoriesRecyclerView.adapter as CategoriesAdapter).setData(uiState.categories)
                    (layoutsRecyclerView?.adapter as? LayoutCategoryAdapter)?.update(uiState.layoutCategories)
                    setButtonsVisibility(uiState.buttonsUiState)
                    setTitleVisibility(uiState.isHeaderVisible)
                    showLoadingSkeleton(uiState.loadingSkeletonVisible)
                }
                is ErrorUiState -> {
                    setTitleVisibility(uiState.isHeaderVisible)
                    showLoadingSkeleton(uiState.loadingSkeletonVisible)
                    ToastUtils.showToast(activity, uiState.message, SHORT)
                }
            }
        })

        viewModel.onCategorySelected.observe(this, Observer {
            it?.applyIfNotHandled {
                layoutsRecyclerView?.smoothScrollToPosition(0)
            }
        })
    }

    private fun showLoadingSkeleton(skeleton: Boolean) {
        categoriesSkeleton.setVisible(skeleton)
        categoriesRecyclerView.setVisible(!skeleton)
        layoutsSkeleton.setVisible(skeleton)
        layoutsRecyclerView.setVisible(!skeleton)
    }

    private fun setButtonsVisibility(uiState: ButtonsUiState) {
        createBlankPageButton.setVisible(uiState.createBlankPageVisible)
        createPageButton.setVisible(uiState.createPageVisible)
        previewButton.setVisible(uiState.previewVisible)
    }

    private fun fillTheScreen(dialog: BottomSheetDialog) {
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.skipCollapsed = true
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }
}
