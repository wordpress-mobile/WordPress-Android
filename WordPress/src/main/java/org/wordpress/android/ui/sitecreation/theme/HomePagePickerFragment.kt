package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.home_page_picker_bottom_toolbar.*
import kotlinx.android.synthetic.main.home_page_picker_fragment.*
import kotlinx.android.synthetic.main.home_page_picker_loading_skeleton.*
import kotlinx.android.synthetic.main.home_page_picker_titlebar.*
import kotlinx.android.synthetic.main.modal_layout_picker_subtitle_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_title_row.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.StarterDesignModel
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

    companion object {
        const val FETCHED_LAYOUTS = "FETCHED_LAYOUTS"
        const val SELECTED_LAYOUT = "SELECTED_LAYOUT"
    }

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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutsRecyclerView.apply {
            adapter = HomePagePickerAdapter(imageManager, thumbDimensionProvider)
            layoutManager = GridLayoutManager(activity, thumbDimensionProvider.columns)
        }

        setupUi()
        setupActionListeners()
        setupViewModel(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        if (context !is SiteDesignsScreenListener) {
            throw IllegalStateException("Parent activity must implement SiteDesignsScreenListener.")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        (viewModel.uiState.value as? UiState.Content)?.let {
            outState.putParcelableArrayList(FETCHED_LAYOUTS, ArrayList(viewModel.layouts))
            outState.putString(SELECTED_LAYOUT, it.selectedLayoutSlug)
        }
        super.onSaveInstanceState(outState)
    }

    private fun setupViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
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

        viewModel.onDesignActionPressed.observe(viewLifecycleOwner, Observer { design ->
            (requireActivity() as SiteDesignsScreenListener).onSiteDesignSelected(design.template, design.segmentId)
        })

        savedInstanceState?.let {
            val layouts = it.getParcelableArrayList<StarterDesignModel>(FETCHED_LAYOUTS)
            val selected = it.getString(SELECTED_LAYOUT)
            viewModel.loadSavedState(layouts, selected)
        } ?: run {
            viewModel.start()
        }
    }

    private fun setupUi() {
        title?.setVisible(isPhoneLandscape())
        header?.setText(R.string.hpp_title)
        description?.setText(R.string.hpp_subtitle)
    }

    private fun setupActionListeners() {
        previewButton.setOnClickListener { viewModel.onPreviewTapped() }
        chooseButton.setOnClickListener { viewModel.onChooseTapped() }
        skipButton.setOnClickListener { viewModel.onSkippedTapped() }
        errorView.button.setOnClickListener { viewModel.onRetryClicked() }
        backButton.setOnClickListener {
            requireActivity().onBackPressed() // FIXME: This is temporary for PR #13192
            viewModel.onBackPressed()
        }
        setScrollListener()
    }

    private fun setScrollListener() {
        if (isPhoneLandscape()) return // Always visible
        val scrollThreshold = resources.getDimension(R.dimen.picker_header_scroll_snap_threshold).toInt()
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            viewModel.onAppBarOffsetChanged(verticalOffset, scrollThreshold)
        })
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

    private fun isPhoneLandscape() = displayUtils.isLandscape() && !displayUtils.isTablet()
}
