package org.wordpress.android.ui.sitecreation.segments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.LayoutRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.NewSiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsContentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsErrorUiState
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

private const val KEY_LIST_STATE = "list_state"

class NewSiteCreationSegmentsFragment : NewSiteCreationBaseFormFragment() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: NewSiteCreationSegmentsViewModel

    private lateinit var errorLayout: ViewGroup
    private lateinit var errorTitle: TextView
    private lateinit var errorSubtitle: TextView

    @Inject internal lateinit var imageManager: ImageManager
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var helpClickedListener: OnHelpClickedListener
    private lateinit var segmentsScreenListener: SegmentsScreenListener

    override fun onAttach(context: Context?) {
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
        return R.layout.new_site_creation_segments_screen
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
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = NewSiteCreationSegmentsAdapter(imageManager = imageManager)
        recyclerView.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(NewSiteCreationSegmentsViewModel::class.java)

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
            helpClickedListener.onHelpClicked(HelpActivity.Origin.NEW_SITE_CREATION_SEGMENTS)
        })
        viewModel.start()
    }

    private fun updateErrorLayout(errorUiStateState: SegmentsErrorUiState) {
        setTextOrHide(errorTitle, errorUiStateState.titleResId)
        setTextOrHide(errorSubtitle, errorUiStateState.subtitleResId)
    }

    private fun setTextOrHide(textView: TextView, resId: Int?) {
        textView.visibility = if (resId == null) View.GONE else View.VISIBLE
        resId?.let {
            textView.text = resources.getString(resId)
        }
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
        (activity!!.application as WordPress).component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_LIST_STATE, linearLayoutManager.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            linearLayoutManager.onRestoreInstanceState(it)
        }
    }

    private fun updateContentLayout(segments: SegmentsContentUiState) {
        (recyclerView.adapter as NewSiteCreationSegmentsAdapter).update(segments.items)
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
    }

    companion object {
        const val TAG = "site_creation_segment_fragment_tag"

        fun newInstance(screenTitle: String): NewSiteCreationSegmentsFragment {
            val fragment = NewSiteCreationSegmentsFragment()
            val bundle = Bundle()
            bundle.putString(NewSiteCreationBaseFormFragment.EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}
