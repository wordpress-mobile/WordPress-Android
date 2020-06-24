package org.wordpress.android.ui.sitecreation.segments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fullscreen_error_with_retry.*
import kotlinx.android.synthetic.main.site_creation_segments_screen.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsContentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsErrorUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class SiteCreationSegmentsFragment : SiteCreationBaseFormFragment() {
    private lateinit var viewModel: SiteCreationSegmentsViewModel

    @Inject internal lateinit var imageManager: ImageManager
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
        if (context !is SegmentsScreenListener) {
            throw IllegalStateException("Parent activity must implement SegmentsScreenListener.")
        }
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_segments_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        initRecyclerView()
        initViewModel()
        initRetryButton()
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = SiteCreationSegmentsAdapter(imageManager = imageManager)
        recycler_view.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SiteCreationSegmentsViewModel::class.java)

        viewModel.segmentsUiState.observe(this, Observer { state ->
            state?.let { uiState ->
                when (uiState) {
                    is SegmentsContentUiState -> {
                        recycler_view.visibility = View.VISIBLE
                        error_layout.visibility = View.GONE
                        updateContentLayout(uiState)
                    }
                    is SegmentsErrorUiState -> {
                        recycler_view.visibility = View.GONE
                        error_layout.visibility = View.VISIBLE
                        updateErrorLayout(uiState)
                    }
                }
            }
        })
        viewModel.segmentSelected.observe(
                this,
                Observer { segmentId ->
                    segmentId?.let {
                        (requireActivity() as SegmentsScreenListener).onSegmentSelected(segmentId)
                    }
                })
        viewModel.onHelpClicked.observe(this, Observer {
            (requireActivity() as OnHelpClickedListener).onHelpClicked(HelpActivity.Origin.SITE_CREATION_SEGMENTS)
        })
        viewModel.start()
    }

    private fun updateErrorLayout(errorUiStateState: SegmentsErrorUiState) {
        uiHelpers.setTextOrHide(error_title, errorUiStateState.titleResId)
        uiHelpers.setTextOrHide(error_subtitle, errorUiStateState.subtitleResId)
    }

    private fun initRetryButton() {
        error_retry.setOnClickListener { viewModel.onRetryClicked() }
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun updateContentLayout(segments: SegmentsContentUiState) {
        (recycler_view.adapter as SiteCreationSegmentsAdapter).update(segments.items)
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
    }

    companion object {
        const val TAG = "site_creation_segment_fragment_tag"

        fun newInstance(screenTitle: String): SiteCreationSegmentsFragment {
            val fragment = SiteCreationSegmentsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
