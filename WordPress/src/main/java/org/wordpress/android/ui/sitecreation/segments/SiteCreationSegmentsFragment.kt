package org.wordpress.android.ui.sitecreation.segments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: SiteCreationSegmentsViewModel

    private lateinit var errorLayout: ViewGroup
    private lateinit var errorTitle: TextView
    private lateinit var errorSubtitle: TextView

    @Inject internal lateinit var imageManager: ImageManager
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers

    private lateinit var helpClickedListener: OnHelpClickedListener
    private lateinit var segmentsScreenListener: SegmentsScreenListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
        if (context !is SegmentsScreenListener) {
            throw IllegalStateException("Parent activity must implement SegmentsScreenListener.")
        }
        helpClickedListener = context
        segmentsScreenListener = context
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_segments_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        initErrorLayout(rootView)
        initRecyclerView(rootView)
        initViewModel()
        initRetryButton(rootView)
    }

    private fun initErrorLayout(rootView: ViewGroup) {
        errorLayout = rootView.findViewById(R.id.error_layout)
        errorTitle = errorLayout.findViewById(R.id.error_title)
        errorSubtitle = errorLayout.findViewById(R.id.error_subtitle)
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = SiteCreationSegmentsAdapter(imageManager = imageManager)
        recyclerView.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SiteCreationSegmentsViewModel::class.java)

        viewModel.segmentsUiState.observe(this, Observer { state ->
            state?.let { uiState ->
                when (uiState) {
                    is SegmentsContentUiState -> {
                        recyclerView.visibility = View.VISIBLE
                        errorLayout.visibility = View.GONE
                        updateContentLayout(uiState)
                    }
                    is SegmentsErrorUiState -> {
                        recyclerView.visibility = View.GONE
                        errorLayout.visibility = View.VISIBLE
                        updateErrorLayout(uiState)
                    }
                }
            }
        })
        viewModel.segmentSelected.observe(
                this,
                Observer { segmentId -> segmentId?.let { segmentsScreenListener.onSegmentSelected(segmentId) } })
        viewModel.onHelpClicked.observe(this, Observer {
            helpClickedListener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_SEGMENTS)
        })
        viewModel.start()
    }

    private fun updateErrorLayout(errorUiStateState: SegmentsErrorUiState) {
        uiHelpers.setTextOrHide(errorTitle, errorUiStateState.titleResId)
        uiHelpers.setTextOrHide(errorSubtitle, errorUiStateState.subtitleResId)
    }

    private fun initRetryButton(rootView: ViewGroup) {
        val retryBtn = rootView.findViewById<Button>(R.id.error_retry)
        retryBtn.setOnClickListener { viewModel.onRetryClicked() }
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun updateContentLayout(segments: SegmentsContentUiState) {
        (recyclerView.adapter as SiteCreationSegmentsAdapter).update(segments.items)
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
