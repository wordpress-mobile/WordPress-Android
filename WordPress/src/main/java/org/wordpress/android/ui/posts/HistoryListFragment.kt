package org.wordpress.android.ui.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.history_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.history.HistoryAdapter
import org.wordpress.android.ui.history.HistoryListItem
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.history.HistoryViewModel
import org.wordpress.android.viewmodel.history.HistoryViewModel.HistoryListStatus
import javax.inject.Inject

class HistoryListFragment : Fragment() {
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var viewModel: HistoryViewModel
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    companion object {
        private const val KEY_POST_LOCAL_ID = "key_post_local_id"
        private const val KEY_SITE = "key_site"

        fun newInstance(@NonNull post: PostModel, @NonNull site: SiteModel): HistoryListFragment {
            val fragment = HistoryListFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_POST_LOCAL_ID, post.id)
            bundle.putSerializable(KEY_SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }

    interface HistoryItemClickInterface {
        fun onHistoryItemClicked(revision: Revision, revisions: ArrayList<Revision>)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.history_list_fragment, container, false)
    }

    private fun onItemClicked(item: HistoryListItem) {
        viewModel.onItemClicked(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (checkNotNull(activity).application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HistoryViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        empty_recycler_view.layoutManager = LinearLayoutManager(nonNullActivity, RecyclerView.VERTICAL, false)
        empty_recycler_view.setEmptyView(actionable_empty_view)
        actionable_empty_view.button.setText(R.string.button_retry)
        actionable_empty_view.button.setOnClickListener {
            viewModel.onPullToRefresh()
        }

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(swipe_refresh_layout) {
            viewModel.onPullToRefresh()
        }

        (nonNullActivity.application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HistoryViewModel::class.java)
        viewModel.create(
                localPostId = arguments?.getInt(KEY_POST_LOCAL_ID) ?: 0,
                site = arguments?.get(KEY_SITE) as SiteModel
        )
        updatePostOrPageEmptyView()
        setObservers()
    }

    private fun updatePostOrPageEmptyView() {
        actionable_empty_view.title.text = getString(R.string.history_empty_title)
        actionable_empty_view.button.visibility = View.GONE
        actionable_empty_view.subtitle.visibility = View.VISIBLE
    }

    private fun reloadList(data: List<HistoryListItem>) {
        setList(data)
    }

    private fun setList(list: List<HistoryListItem>) {
        val adapter: HistoryAdapter

        if (empty_recycler_view.adapter == null) {
            adapter = HistoryAdapter(checkNotNull(activity), this::onItemClicked)
            empty_recycler_view.adapter = adapter
        } else {
            adapter = empty_recycler_view.adapter as HistoryAdapter
        }

        adapter.updateList(list)
    }

    private fun setObservers() {
        viewModel.revisions.observe(this, Observer {
            reloadList(it ?: emptyList())
        })

        viewModel.listStatus.observe(this, Observer { listStatus ->
            if (isAdded && view != null) {
                swipeToRefreshHelper.isRefreshing = listStatus == HistoryListStatus.FETCHING
            }
            when (listStatus) {
                HistoryListStatus.DONE -> {
                    updatePostOrPageEmptyView()
                }
                HistoryListStatus.FETCHING -> {
                    actionable_empty_view.title.setText(R.string.history_fetching_revisions)
                    actionable_empty_view.subtitle.visibility = View.GONE
                    actionable_empty_view.button.visibility = View.GONE
                }
                HistoryListStatus.NO_NETWORK -> {
                    actionable_empty_view.title.setText(R.string.no_network_title)
                    actionable_empty_view.subtitle.setText(R.string.no_network_message)
                    actionable_empty_view.subtitle.visibility = View.VISIBLE
                    actionable_empty_view.button.visibility = View.VISIBLE
                }
                HistoryListStatus.ERROR -> {
                    actionable_empty_view.title.setText(R.string.no_network_title)
                    actionable_empty_view.subtitle.setText(R.string.error_generic_network)
                    actionable_empty_view.subtitle.visibility = View.VISIBLE
                    actionable_empty_view.button.visibility = View.VISIBLE
                }
            }
        })

        viewModel.showDialog.observe(this, Observer {
            if (it is Revision && activity is HistoryItemClickInterface) {
                (activity as HistoryItemClickInterface).onHistoryItemClicked(it, viewModel.revisionsList)
            }
        })

        viewModel.post.observe(this, Observer { post ->
            actionable_empty_view.subtitle.text = if (post?.isPage == true) {
                getString(R.string.history_empty_subtitle_page)
            } else {
                getString(R.string.history_empty_subtitle_post)
            }
        })
    }
}
