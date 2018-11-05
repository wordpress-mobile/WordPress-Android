package org.wordpress.android.ui.sitecreation.segments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.LayoutRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.ui.sitecreation.NewSiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.NewSiteCreationListener
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class NewSiteCreationSegmentsFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private val keyListState = "list_state"
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: NewSiteCreationSegmentsViewModel

    private lateinit var contentLayout: ViewGroup
    private lateinit var progressLayout: ViewGroup
    private lateinit var errorLayout: ViewGroup

    @Inject protected lateinit var imageManager: ImageManager
    @Inject protected lateinit var viewModelFactory: ViewModelProvider.Factory

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_segments_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // important for accessibility - talkback
        activity!!.setTitle(R.string.new_site_creation_segments_title)
        contentLayout = rootView.findViewById(R.id.content_layout)
        progressLayout = rootView.findViewById(R.id.progress_layout)
        errorLayout = rootView.findViewById(R.id.error_layout)
        initRecyclerView(rootView)
        initViewModel()
        initRetryButton(rootView)
    }

    private fun initRecyclerView(rootView: ViewGroup) {
        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager = layoutManager
        recyclerView.layoutManager = linearLayoutManager
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(NewSiteCreationSegmentsViewModel::class.java)

        viewModel.categories.observe(this, Observer { segments -> segments?.let { updateSegments(segments) } })
        viewModel.showError.observe(this, Observer { showError ->
            showError?.let {
                if (showError) {
                    showError()
                }
            }
        })
        viewModel.showProgress.observe(this, Observer { showProgress ->
            showProgress?.let {
                if (showProgress) {
                    showProgress()
                }
            }
        })

        viewModel.start()
    }

    private fun initRetryButton(rootView: ViewGroup) {
        val retryBtn = rootView.findViewById<Button>(R.id.error_retry)
        retryBtn.setOnClickListener { view -> viewModel.onRetryClicked() }
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(keyListState, linearLayoutManager.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.getParcelable<Parcelable>(keyListState)?.let {
            linearLayoutManager.onRestoreInstanceState(it)
        }
    }

    private fun updateSegments(segments: List<VerticalSegmentModel>) {
        val adapter: NewSiteCreationSegmentsAdapter
        if (recyclerView.adapter == null) {
            adapter = NewSiteCreationSegmentsAdapter(
                    onItemTapped = { segment -> viewModel.onSegmentSelected(segment.segmentId) },
                    imageManager = imageManager
            )
            recyclerView.adapter = adapter
        } else {
            adapter = recyclerView.adapter as NewSiteCreationSegmentsAdapter
        }

        showContent()
        adapter.update(segments)
    }

    private fun showContent() {
        contentLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.VISIBLE
        progressLayout.visibility = View.GONE
        errorLayout.visibility = View.GONE
    }

    private fun showProgress() {
        contentLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
    }

    private fun showError() {
        contentLayout.visibility = View.GONE
        progressLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
    }

    companion object {
        val TAG = "site_creation_segment_fragment_tag"
    }
}
