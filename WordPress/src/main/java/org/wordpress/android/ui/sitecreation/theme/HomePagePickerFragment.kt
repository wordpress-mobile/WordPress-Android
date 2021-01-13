package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.ListPopupWindow
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.elevation.ElevationOverlayProvider
import kotlinx.android.synthetic.main.home_page_picker_bottom_toolbar.*
import kotlinx.android.synthetic.main.home_page_picker_bottom_toolbar.chooseButton
import kotlinx.android.synthetic.main.home_page_picker_fragment.*
import kotlinx.android.synthetic.main.home_page_picker_fragment.errorView
import kotlinx.android.synthetic.main.home_page_picker_loading_skeleton.*
import kotlinx.android.synthetic.main.home_page_picker_titlebar.*
import kotlinx.android.synthetic.main.modal_layout_picker_subtitle_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_title_row.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.theme.DesignPreviewFragment.Companion.DESIGN_PREVIEW_TAG
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.DesignPreviewAction.Dismiss
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.DesignPreviewAction.Show
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.UiState
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration
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
    @Inject lateinit var thumbDimensionProvider: ThumbDimensionProvider
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HomePagePickerViewModel
    private lateinit var previewModeSelector: ListPopupWindow
    private lateinit var elevationOverlayProvider: ElevationOverlayProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.home_page_picker_fragment, container, false)
        val skeletonCardView = view.findViewById<View>(R.id.skeletonCardView)
        skeletonCardView.minimumHeight = thumbDimensionProvider.height
        skeletonCardView.minimumWidth = thumbDimensionProvider.width
        (skeletonCardView.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            it.marginStart = thumbDimensionProvider.calculatedStartMargin
        }
        elevationOverlayProvider = ElevationOverlayProvider(requireActivity())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutsRecyclerView.apply {
            adapter = HomePagePickerAdapter(imageManager, thumbDimensionProvider)
            layoutManager = GridLayoutManager(activity, thumbDimensionProvider.columns)
        }

        setupUi()
        setupViewModel()
        setupActionListeners()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(HomePagePickerViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            setTitleVisibility(uiState.isHeaderVisible)
            description?.visibility = if (uiState.isDescriptionVisible) View.VISIBLE else View.INVISIBLE
            loadingIndicator.setVisible(uiState.loadingIndicatorVisible)
            errorView.setVisible(uiState.errorViewVisible)
            layoutsRecyclerView.setVisible(!uiState.loadingIndicatorVisible && !uiState.errorViewVisible)
            AniUtils.animateBottomBar(bottomToolbar, uiState.isToolbarVisible)
            when (uiState) {
                is UiState.Loading -> { // Nothing more to do here
                }
                is UiState.Content -> {
                    (layoutsRecyclerView.adapter as? HomePagePickerAdapter)?.setData(uiState.layouts)
                }
                is UiState.Error -> {
                    uiState.toast?.let { ToastUtils.showToast(requireContext(), it) }
                }
            }
        })

        viewModel.onPreviewActionPressed.observe(viewLifecycleOwner, Observer { action ->
            activity?.supportFragmentManager?.let { fm ->
                when (action) {
                    is Show -> {
                        val previewFragment = DesignPreviewFragment.newInstance(action.template, action.demoUrl)
                        previewFragment.show(fm, DESIGN_PREVIEW_TAG)
                    }
                    is Dismiss -> {
                        (fm.findFragmentByTag(DESIGN_PREVIEW_TAG) as? DesignPreviewFragment)?.dismiss()
                    }
                }
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
        previewTypeSelectorButton.setOnClickListener { viewModel.thumbnailMode.value?.let { showModeSelector(it) } }
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

    private fun isPhoneLandscape() = displayUtils.isLandscapeBySize() && !displayUtils.isTablet()

    private fun showModeSelector(selected: PreviewMode) {
        previewTypeSelectorButton.post(Runnable {
            val popupWidth = resources.getDimensionPixelSize(dimen.web_preview_mode_popup_width)
            val popupOffset = resources.getDimensionPixelSize(dimen.margin_extra_large)
            previewModeSelector = ListPopupWindow(requireActivity())
            previewModeSelector.width = popupWidth
            previewModeSelector.setAdapter(PreviewModeMenuAdapter(requireActivity(), selected))
            previewModeSelector.setDropDownGravity(Gravity.END)
            previewModeSelector.anchorView = previewTypeSelectorButton
            previewModeSelector.horizontalOffset = -popupOffset
            previewModeSelector.verticalOffset = popupOffset
            previewModeSelector.isModal = true
            val elevatedPopupBackgroundColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(
                    resources.getDimension(dimen.popup_over_toolbar_elevation)
            )
            previewModeSelector.setBackgroundDrawable(ColorDrawable(elevatedPopupBackgroundColor))
            previewModeSelector.setOnItemClickListener { parent, _, position, _ ->
                previewModeSelector.dismiss()
                val adapter = parent.adapter as PreviewModeMenuAdapter
                val selectedMode = adapter.getItem(position)
                if (selectedMode !== viewModel.thumbnailMode.value) {
                    viewModel.onThumbnailModeChanged(selectedMode)
                }
            }
            previewModeSelector.show()
        })
    }
}
