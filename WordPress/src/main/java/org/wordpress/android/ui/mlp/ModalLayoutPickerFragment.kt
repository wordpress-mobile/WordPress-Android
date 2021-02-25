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
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.FullscreenBottomSheetDialogFragment
import org.wordpress.android.ui.PreviewModeSelectorPopup
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.setVisible
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ContentUiState
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.ErrorUiState
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel.UiState.LoadingUiState
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
        const val FETCHED_LAYOUTS = "FETCHED_LAYOUTS"
        const val SELECTED_CATEGORIES = "SELECTED_CATEGORIES"
        const val SELECTED_LAYOUT = "SELECTED_LAYOUT"
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
            viewModel.onPreviewPageClicked()
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
     * Sets the header and title visibility
     * @param visible if true the title is shown and the header is hidden
     */
    private fun setTitleVisibility(visible: Boolean) {
        if (title == null || header == null || visible == (title.visibility == View.VISIBLE)) return // No change
        if (visible) {
            AniUtils.fadeIn(title, Duration.SHORT)
            AniUtils.fadeOut(header, Duration.SHORT, View.INVISIBLE)
        } else {
            AniUtils.fadeIn(header, Duration.SHORT)
            AniUtils.fadeOut(title, Duration.SHORT, View.INVISIBLE)
        }
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
        (viewModel.uiState.value as? ContentUiState)?.let {
            outState.putSerializable(SELECTED_CATEGORIES, it.selectedCategoriesSlugs)
            outState.putString(SELECTED_LAYOUT, it.selectedLayoutSlug)
        }
        outState.putParcelable(FETCHED_LAYOUTS, viewModel.fetchedLayouts())

        super.onSaveInstanceState(outState)
    }

    private fun loadSavedState(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            val layouts = it.getParcelable<GutenbergPageLayouts>(FETCHED_LAYOUTS)
            val selected = it.getString(SELECTED_LAYOUT)
            val categories = (it.getSerializable(SELECTED_CATEGORIES) as? List<*>)?.filterIsInstance<String>()
            viewModel.loadSavedState(layouts, selected, categories)
        }
    }

    private fun setupViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(ModalLayoutPickerViewModel::class.java)

        loadSavedState(savedInstanceState)

        viewModel.uiState.observe(this, Observer { uiState ->
            setTitleVisibility(uiState.isHeaderVisible)
            setDescriptionVisibility(uiState.isDescriptionVisible)
            setButtonsVisibility(uiState.buttonsUiState)
            setContentVisibility(uiState.loadingSkeletonVisible, uiState.errorViewVisible)
            when (uiState) {
                is LoadingUiState -> {
                }
                is ContentUiState -> {
                    (categoriesRecyclerView.adapter as CategoriesAdapter).setData(uiState.categories)
                    (layoutsRecyclerView?.adapter as? LayoutCategoryAdapter)?.update(uiState.layoutCategories)
                }
                is ErrorUiState -> {
                    actionableEmptyView.title.setText(uiState.title)
                    actionableEmptyView.subtitle.setText(uiState.subtitle)
                }
            }
        })

        viewModel.onThumbnailModeButtonPressed.observe(viewLifecycleOwner, Observer {
            previewModeSelectorPopup.show(viewModel)
        })

        viewModel.onPreviewPageRequested.observe(this, Observer { request ->
            ActivityLauncher.previewPageForResult(this, request.site, request.content, request.template)
        })

        viewModel.onCategorySelected.observe(this, Observer {
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
