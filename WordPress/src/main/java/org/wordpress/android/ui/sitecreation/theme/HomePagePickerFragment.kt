package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.home_page_picker_bottom_toolbar.*
import kotlinx.android.synthetic.main.home_page_picker_bottom_toolbar.chooseButton
import kotlinx.android.synthetic.main.home_page_picker_fragment.appBarLayout
import kotlinx.android.synthetic.main.home_page_picker_fragment.categoriesRecyclerView
import kotlinx.android.synthetic.main.home_page_picker_fragment.errorView
import kotlinx.android.synthetic.main.home_page_picker_fragment.layoutsRecyclerView
import kotlinx.android.synthetic.main.home_page_picker_titlebar.*
import kotlinx.android.synthetic.main.home_page_picker_titlebar.backButton
import kotlinx.android.synthetic.main.home_page_picker_titlebar.previewTypeSelectorButton
import kotlinx.android.synthetic.main.home_page_picker_titlebar.title
import kotlinx.android.synthetic.main.modal_layout_picker_categories_skeleton.*
import kotlinx.android.synthetic.main.modal_layout_picker_layouts_skeleton.*
import kotlinx.android.synthetic.main.modal_layout_picker_subtitle_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_title_row.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.PreviewModeSelectorPopup
import org.wordpress.android.ui.layoutpicker.CategoriesAdapter
import org.wordpress.android.ui.layoutpicker.LayoutCategoryAdapter
import org.wordpress.android.ui.sitecreation.theme.DesignPreviewFragment.Companion.DESIGN_PREVIEW_TAG
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Dismiss
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Show
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * Implements the Home Page Picker UI
 */
class HomePagePickerFragment : Fragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var displayUtils: DisplayUtilsWrapper
    @Inject internal lateinit var uiHelper: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HomePagePickerViewModel
    private lateinit var previewModeSelectorPopup: PreviewModeSelectorPopup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_page_picker_fragment, container, false)
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

        setupUi()
        setupViewModel()
        setupActionListeners()
        previewModeSelectorPopup = PreviewModeSelectorPopup(requireActivity(), previewTypeSelectorButton)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    private fun setContentVisibility(skeleton: Boolean, error: Boolean) {
        categoriesSkeleton.setVisible(skeleton)
        categoriesRecyclerView.setVisible(!skeleton && !error)
        layoutsSkeleton.setVisible(skeleton)
        layoutsRecyclerView.setVisible(!skeleton && !error)
        errorView.setVisible(error)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(HomePagePickerViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            uiHelper.fadeInfadeOutViews(title, header, uiState.isHeaderVisible)
            description?.visibility = if (uiState.isDescriptionVisible) View.VISIBLE else View.INVISIBLE
            setContentVisibility(uiState.loadingSkeletonVisible, uiState.errorViewVisible)
            AniUtils.animateBottomBar(bottomToolbar, uiState.isToolbarVisible)
            when (uiState) {
                is LayoutPickerUiState.Loading -> { // Nothing more to do here
                }
                is LayoutPickerUiState.Content -> {
                    (categoriesRecyclerView.adapter as CategoriesAdapter).setData(uiState.categories)
                    (layoutsRecyclerView?.adapter as? LayoutCategoryAdapter)?.update(uiState.layoutCategories)
                }
                is LayoutPickerUiState.Error -> {
                    uiState.toast?.let { ToastUtils.showToast(requireContext(), it) }
                }
            }
        })

        viewModel.onPreviewActionPressed.observe(viewLifecycleOwner, Observer { action ->
            activity?.supportFragmentManager?.let { fm ->
                when (action) {
                    is Show -> {
                        val previewFragment = DesignPreviewFragment.newInstance()
                        previewFragment.show(fm, DESIGN_PREVIEW_TAG)
                    }
                    is Dismiss -> {
                        (fm.findFragmentByTag(DESIGN_PREVIEW_TAG) as? DesignPreviewFragment)?.dismiss()
                    }
                }
            }
        })

        viewModel.onThumbnailModeButtonPressed.observe(viewLifecycleOwner, Observer {
            previewModeSelectorPopup.show(viewModel)
        })

        viewModel.onCategorySelectionChanged.observe(viewLifecycleOwner, Observer {
            it?.applyIfNotHandled {
                layoutsRecyclerView?.smoothScrollToPosition(0)
            }
        })

        viewModel.start(displayUtils.isTablet())
    }

    private fun setupUi() {
        title?.visibility = if (isPhoneLandscape()) View.VISIBLE else View.INVISIBLE
        header?.setText(R.string.hpp_title)
        description?.setText(R.string.hpp_subtitle)
    }

    private fun setupActionListeners() {
        previewButton.setOnClickListener { viewModel.onPreviewTapped() }
        chooseButton.setOnClickListener { viewModel.onChooseTapped() }
        skipButton.setOnClickListener { viewModel.onSkippedTapped() }
        errorView.button.setOnClickListener { viewModel.onRetryClicked() }
        backButton.setOnClickListener { viewModel.onBackPressed() }
        previewTypeSelectorButton.setOnClickListener { viewModel.onThumbnailModePressed() }
        setScrollListener()
    }

    private fun setScrollListener() {
        if (isPhoneLandscape()) return // Always visible
        val scrollThreshold = resources.getDimension(R.dimen.picker_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
        viewModel.onAppBarOffsetChanged(0, scrollThreshold)
    }

    private fun isPhoneLandscape() = displayUtils.isLandscapeBySize() && !displayUtils.isTablet()
}
