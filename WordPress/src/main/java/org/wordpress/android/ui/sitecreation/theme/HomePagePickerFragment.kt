package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.HomePagePickerFragmentBinding
import org.wordpress.android.ui.layoutpicker.LayoutCategoryAdapter
import org.wordpress.android.ui.layoutpicker.LayoutPickerUiState
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Dismiss
import org.wordpress.android.ui.layoutpicker.LayoutPickerViewModel.DesignPreviewAction.Show
import org.wordpress.android.ui.sitecreation.theme.DesignPreviewFragment.Companion.DESIGN_PREVIEW_TAG
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.config.SiteNameFeatureConfig
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

/**
 * Implements the Home Page Picker UI
 */
@Suppress("TooManyFunctions")
class HomePagePickerFragment : Fragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var displayUtils: DisplayUtilsWrapper
    @Inject internal lateinit var uiHelper: UiHelpers
    @Inject lateinit var siteNameFeatureConfig: SiteNameFeatureConfig
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var thumbDimensionProvider: SiteDesignPickerDimensionProvider
    private lateinit var viewModel: HomePagePickerViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_page_picker_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(HomePagePickerViewModel::class.java)

        with(HomePagePickerFragmentBinding.bind(view)) {
            modalLayoutPickerCategoriesSkeleton.root.isGone = true
            categoriesRecyclerView.isGone = true
            layoutsRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireActivity())
                adapter = LayoutCategoryAdapter(viewModel.nestedScrollStates, thumbDimensionProvider)
            }

            setupUi()
            setupViewModel()
            setupActionListeners()
        }
    }

    private fun HomePagePickerFragmentBinding.setupUi() {
        homePagePickerTitlebar.title.isInvisible = !displayUtils.isPhoneLandscape()
        with(modalLayoutPickerHeaderSection) {
            modalLayoutPickerTitleRow?.header?.apply {
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                setText(R.string.hpp_title)
            }
            modalLayoutPickerSubtitleRow?.root?.visibility = View.GONE
        }
    }

    private fun HomePagePickerFragmentBinding.setupViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            setHeaderVisibility(uiState.isHeaderVisible)
            setContentVisibility(uiState.loadingSkeletonVisible, uiState.errorViewVisible)
            when (uiState) {
                is LayoutPickerUiState.Loading -> { // Nothing more to do here
                }
                is LayoutPickerUiState.Content -> {
                    (layoutsRecyclerView.adapter as? LayoutCategoryAdapter)?.update(uiState.layoutCategories)
                }
                is LayoutPickerUiState.Error -> {
                    uiState.toast?.let { ToastUtils.showToast(requireContext(), it) }
                }
            }
        }

        viewModel.onPreviewActionPressed.observe(viewLifecycleOwner) { action ->
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
        }

        viewModel.start(displayUtils.isTablet())
    }

    private fun HomePagePickerFragmentBinding.setHeaderVisibility(visible: Boolean) {
        uiHelper.fadeInfadeOutViews(
                homePagePickerTitlebar.title,
                modalLayoutPickerHeaderSection.modalLayoutPickerTitleRow?.header,
                visible
        )
    }

    private fun HomePagePickerFragmentBinding.setContentVisibility(skeleton: Boolean, error: Boolean) {
        modalLayoutPickerLayoutsSkeleton.layoutsSkeleton.setVisible(skeleton)
        layoutsRecyclerView.setVisible(!skeleton && !error)
        errorView.setVisible(error)
    }

    private fun HomePagePickerFragmentBinding.setupActionListeners() {
        homePagePickerTitlebar.skipButton.setOnClickListener { viewModel.onSkippedTapped() }
        errorView.button.setOnClickListener { viewModel.onRetryClicked() }
        homePagePickerTitlebar.backButton.setOnClickListener { viewModel.onBackPressed() }
        setScrollListener()
    }

    private fun HomePagePickerFragmentBinding.setScrollListener() {
        if (displayUtils.isPhoneLandscape()) return // Always visible
        val scrollThreshold = resources.getDimension(R.dimen.picker_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
        viewModel.onAppBarOffsetChanged(0, scrollThreshold)
    }
}
