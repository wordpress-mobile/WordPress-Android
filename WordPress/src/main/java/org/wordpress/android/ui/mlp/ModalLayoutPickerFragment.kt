package org.wordpress.android.ui.mlp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import kotlinx.android.synthetic.main.modal_layout_picker_bottom_toolbar.*
import kotlinx.android.synthetic.main.modal_layout_picker_categories_skeleton.*
import kotlinx.android.synthetic.main.modal_layout_picker_error.*
import kotlinx.android.synthetic.main.modal_layout_picker_fragment.*
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_skeleton.*
import kotlinx.android.synthetic.main.modal_layout_picker_subtitle_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_title_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_titlebar.backButton
import kotlinx.android.synthetic.main.modal_layout_picker_titlebar.previewTypeSelectorButton
import kotlinx.android.synthetic.main.modal_layout_picker_titlebar.title
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.FullscreenBottomSheetDialogFragment
import org.wordpress.android.ui.PreviewModeSelectorPopup
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.layoutpicker.ButtonsUiState
import org.wordpress.android.ui.layoutpicker.CategoriesAdapter
import org.wordpress.android.ui.layoutpicker.LayoutCategoryAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Content
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Error
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState.Loading
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Dismiss
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Show
import org.wordpress.android.ui.mlp.BlockLayoutPreviewFragment.Companion.BLOCK_LAYOUT_PREVIEW_TAG
import javax.inject.Inject

/**
 * Implements the Modal Layout Picker UI
 */
class ModalLayoutPickerFragment : FullscreenBottomSheetDialogFragment() {
    @Inject internal lateinit var uiHelper: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ModalLayoutPickerViewModel
    private lateinit var previewModeSelectorPopup: PreviewModeSelectorPopup

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
        previewButton.setOnClickListener {
            viewModel.onPreviewTapped()
        }
        retryButton.setOnClickListener {
            viewModel.onRetryClicked()
        }
        previewTypeSelectorButton.setOnClickListener {
            viewModel.onThumbnailModePressed()
        }

        setScrollListener()

        setupViewModel(savedInstanceState)

        previewModeSelectorPopup = PreviewModeSelectorPopup(requireActivity(), previewTypeSelectorButton)
    }

    private fun setScrollListener() {
        if (DisplayUtils.isLandscape(requireContext())) return // Always visible
        val scrollThreshold = resources.getDimension(R.dimen.picker_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
    }

    /**
     * Sets the header description visibility
     * @param visible if true the description is visible else invisible
     */
    private fun setDescriptionVisibility(visible: Boolean) {
        description?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun closeModal() {
        viewModel.dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    private fun setupViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(ModalLayoutPickerViewModel::class.java)

        viewModel.loadSavedState(savedInstanceState)

        viewModel.uiState.observe(this, Observer { uiState ->
            uiHelper.fadeInfadeOutViews(title, header, uiState.isHeaderVisible)
            setDescriptionVisibility(uiState.isDescriptionVisible)
            setButtonsVisibility(uiState.buttonsUiState)
            setContentVisibility(uiState.loadingSkeletonVisible, uiState.errorViewVisible)
            when (uiState) {
                is Loading -> {
                }
                is Content -> {
                    (categoriesRecyclerView.adapter as CategoriesAdapter).setData(uiState.categories)
                    (layoutsRecyclerView?.adapter as? LayoutCategoryAdapter)?.update(uiState.layoutCategories)
                }
                is Error -> {
                    uiState.title?.let { actionableEmptyView.title.setText(it) }
                    uiState.subtitle?.let { actionableEmptyView.subtitle.setText(it) }
                }
            }
        })

        viewModel.onThumbnailModeButtonPressed.observe(viewLifecycleOwner, Observer {
            previewModeSelectorPopup.show(viewModel)
        })

        viewModel.onPreviewActionPressed.observe(viewLifecycleOwner, Observer { action ->
            activity?.supportFragmentManager?.let { fm ->
                when (action) {
                    is Show -> {
                        val previewFragment = BlockLayoutPreviewFragment.newInstance()
                        previewFragment.show(fm, BLOCK_LAYOUT_PREVIEW_TAG)
                    }
                    is Dismiss -> {
                        (fm.findFragmentByTag(BLOCK_LAYOUT_PREVIEW_TAG) as? BlockLayoutPreviewFragment)?.dismiss()
                    }
                }
            }
        })

        viewModel.onCategorySelectionChanged.observe(this, Observer {
            it?.applyIfNotHandled {
                layoutsRecyclerView?.smoothScrollToPosition(0)
            }
        })
    }

    private fun setContentVisibility(skeleton: Boolean, error: Boolean) {
        categoriesSkeleton.setVisible(skeleton)
        categoriesRecyclerView.setVisible(!skeleton && !error)
        layoutsSkeleton.setVisible(skeleton)
        layoutsRecyclerView.setVisible(!skeleton && !error)
        errorLayout.setVisible(error)
    }

    private fun setButtonsVisibility(uiState: ButtonsUiState) {
        createBlankPageButton.setVisible(uiState.createBlankPageVisible)
        createPageButton.setVisible(uiState.createPageVisible)
        previewButton.setVisible(uiState.previewVisible)
        retryButton.setVisible(uiState.retryVisible)
        createOrRetryContainer.setVisible(uiState.createBlankPageVisible || uiState.retryVisible)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.PREVIEW_POST) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.onCreatePageClicked()
            }
        }
    }
}
