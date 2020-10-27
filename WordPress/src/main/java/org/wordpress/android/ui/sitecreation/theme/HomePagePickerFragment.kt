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
import kotlinx.android.synthetic.main.home_page_picker_fragment.appBarLayout
import kotlinx.android.synthetic.main.home_page_picker_fragment.layoutsRecyclerView
import kotlinx.android.synthetic.main.home_page_picker_titlebar.*
import kotlinx.android.synthetic.main.modal_layout_picker_subtitle_row.*
import kotlinx.android.synthetic.main.modal_layout_picker_title_row.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.UiState
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AniUtils.Duration
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * Implements the Home Page Picker UI
 */
class HomePagePickerFragment : Fragment() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var thumbDimensionProvider: ThumbDimensionProvider
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HomePagePickerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_page_picker_fragment, container, false)
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
    }

    private fun setupViewModel(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(HomePagePickerViewModel::class.java)

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            setTitleVisibility(uiState.isHeaderVisible)
            AniUtils.animateBottomBar(bottomToolbar, uiState.isToolbarVisible)
            when (uiState) {
                is UiState.Loading -> {
                    // TODO: Show skeleton
                }
                is UiState.Content -> {
                    (layoutsRecyclerView.adapter as? HomePagePickerAdapter)?.setData(uiState.layouts)
                }
                is UiState.Error -> {
                    // TODO: Show error
                }
            }
        })

        viewModel.start()
    }

    private fun setupUi() {
        title?.setVisible(DisplayUtils.isLandscape(requireContext()))
        header?.setText(R.string.hpp_title)
        description?.setText(R.string.hpp_subtitle)
    }

    private fun setupActionListeners() {
        previewButton.setOnClickListener { viewModel.onPreviewTapped() }
        chooseButton.setOnClickListener { viewModel.onChooseTapped() }
        skipButton.setOnClickListener { viewModel.onSkippedTapped() }
        backButton.setOnClickListener {
            requireActivity().onBackPressed() // FIXME: This is temporary for PR #13192
            viewModel.onBackPressed()
        }
        setScrollListener()
    }

    private fun setScrollListener() {
        if (DisplayUtils.isLandscape(requireContext())) return // Always visible
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
}
