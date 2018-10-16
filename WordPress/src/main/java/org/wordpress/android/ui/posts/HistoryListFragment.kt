package org.wordpress.android.ui.posts

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.annotation.NonNull
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.history_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.history.HistoryAdapter
import org.wordpress.android.ui.history.HistoryListItem
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.history.HistoryViewModel
import org.wordpress.android.viewmodel.history.HistoryViewModel.HistoryListStatus.FETCHING
import javax.inject.Inject

class HistoryListFragment : Fragment() {
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var viewModel: HistoryViewModel
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    companion object {
        private const val KEY_POST = "key_post"
        private const val KEY_SITE = "key_site"

        fun newInstance(@NonNull post: PostModel, @NonNull site: SiteModel): HistoryListFragment {
            val fragment = HistoryListFragment()
            val bundle = Bundle()
            bundle.putSerializable(KEY_POST, post)
            bundle.putSerializable(KEY_SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }

    interface HistoryItemClickInterface {
        fun onHistoryItemClicked(revision: Revision)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)

        empty_recycler_view.layoutManager = LinearLayoutManager(nonNullActivity, LinearLayoutManager.VERTICAL, false)
        empty_recycler_view.setEmptyView(actionable_empty_view)

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(swipe_refresh_layout) {
            if (NetworkUtils.checkConnection(nonNullActivity)) {
                viewModel.onPullToRefresh()
            } else {
                swipeToRefreshHelper.isRefreshing = false
            }
        }

        (nonNullActivity.application as WordPress).component()?.inject(this)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HistoryViewModel::class.java)
        viewModel.create(arguments?.get(KEY_POST) as PostModel, arguments?.get(KEY_SITE) as SiteModel)

        actionable_empty_view.subtitle.text = if ((arguments?.get(KEY_POST) as PostModel).isPage) {
            resources.getString(R.string.history_empty_subtitle_page)
        } else {
            resources.getString(R.string.history_empty_subtitle_post)
        }

        setObservers()
    }

    private fun reloadList(data: List<HistoryListItem>) {
        setList(data)
    }

    private fun setList(list: List<HistoryListItem>) {
        val adapter: HistoryAdapter

        if (empty_recycler_view.adapter == null) {
            adapter = HistoryAdapter(this::onItemClicked)
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

        viewModel.eventListStatus.observe(this, Observer { listStatus ->
            updateRefreshing(listStatus)
        })

        viewModel.showLoadDialog.observe(this, Observer {
            if (it is HistoryListItem.Revision) {
                showLoadDialog(it)
            }
        })

        viewModel.showSnackbarMessage.observe(this, Observer { message ->
            val parent: View? = activity?.findViewById(android.R.id.content)
            if (message != null && parent != null) {
                val snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
                val snackbarText = snackbar.view.findViewById<TextView>(android.support.design.R.id.snackbar_text)
                snackbarText.maxLines = 2
                snackbar.show()
            }
        })
    }

    private fun showLoadDialog(revision: Revision) {
        if (activity is HistoryItemClickInterface) {
            (activity as HistoryItemClickInterface).onHistoryItemClicked(revision)
        }
    }

    private fun updateRefreshing(listStatus: HistoryViewModel.HistoryListStatus?) {
        if (!isAdded || view == null) {
            return
        }

        swipeToRefreshHelper.isRefreshing = listStatus == FETCHING
    }
}
