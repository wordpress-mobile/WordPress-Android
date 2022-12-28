package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.HistoryListFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.history.HistoryAdapter
import org.wordpress.android.ui.history.HistoryListItem
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.viewmodel.history.HistoryViewModel
import org.wordpress.android.viewmodel.history.HistoryViewModel.HistoryListStatus
import javax.inject.Inject

class HistoryListFragment : Fragment(R.layout.history_list_fragment) {
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private lateinit var viewModel: HistoryViewModel
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    companion object {
        private const val KEY_POST_LOCAL_ID = "key_post_local_id"
        private const val KEY_SITE = "key_site"

        fun newInstance(postId: Int, @NonNull site: SiteModel): HistoryListFragment {
            val fragment = HistoryListFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_POST_LOCAL_ID, postId)
            bundle.putSerializable(KEY_SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }

    interface HistoryItemClickInterface {
        fun onHistoryItemClicked(revision: Revision, revisions: List<Revision>)
    }

    private fun onItemClicked(item: HistoryListItem) {
        viewModel.onItemClicked(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity().application as WordPress).component().inject(this)

        viewModel = ViewModelProvider(this, viewModelFactory).get(HistoryViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(activity is HistoryItemClickInterface) {
            "Parent activity has to implement HistoryItemClickInterface"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        with(HistoryListFragmentBinding.bind(view)) {
            emptyRecyclerView.layoutManager = LinearLayoutManager(nonNullActivity, RecyclerView.VERTICAL, false)
            emptyRecyclerView.setEmptyView(actionableEmptyView)
            actionableEmptyView.button.setText(R.string.button_retry)
            actionableEmptyView.button.setOnClickListener {
                viewModel.onPullToRefresh()
            }

            swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(swipeRefreshLayout) {
                viewModel.onPullToRefresh()
            }

            (nonNullActivity.application as WordPress).component().inject(this@HistoryListFragment)

            viewModel = ViewModelProvider(this@HistoryListFragment, viewModelFactory).get(HistoryViewModel::class.java)
            viewModel.create(
                localPostId = arguments?.getInt(KEY_POST_LOCAL_ID) ?: 0,
                site = arguments?.get(KEY_SITE) as SiteModel
            )
            updatePostOrPageEmptyView()
            setObservers()
        }
    }

    private fun HistoryListFragmentBinding.updatePostOrPageEmptyView() {
        actionableEmptyView.title.text = getString(R.string.history_empty_title)
        actionableEmptyView.button.visibility = View.GONE
        actionableEmptyView.subtitle.visibility = View.VISIBLE
    }

    private fun HistoryListFragmentBinding.reloadList(data: List<HistoryListItem>) {
        setList(data)
    }

    private fun HistoryListFragmentBinding.setList(list: List<HistoryListItem>) {
        val adapter: HistoryAdapter

        if (emptyRecyclerView.adapter == null) {
            adapter = HistoryAdapter(requireActivity(), this@HistoryListFragment::onItemClicked)
            emptyRecyclerView.adapter = adapter
        } else {
            adapter = emptyRecyclerView.adapter as HistoryAdapter
        }

        adapter.updateList(list)
    }

    private fun HistoryListFragmentBinding.setObservers() {
        viewModel.revisions.observe(viewLifecycleOwner, Observer {
            reloadList(it ?: emptyList())
        })

        viewModel.listStatus.observe(viewLifecycleOwner, Observer { listStatus ->
            listStatus?.let {
                if (isAdded && view != null) {
                    swipeToRefreshHelper.isRefreshing = listStatus == HistoryListStatus.FETCHING
                }
                when (listStatus) {
                    HistoryListStatus.DONE -> {
                        updatePostOrPageEmptyView()
                    }
                    HistoryListStatus.FETCHING -> {
                        actionableEmptyView.title.setText(R.string.history_fetching_revisions)
                        actionableEmptyView.subtitle.visibility = View.GONE
                        actionableEmptyView.button.visibility = View.GONE
                    }
                    HistoryListStatus.NO_NETWORK -> {
                        actionableEmptyView.title.setText(R.string.no_network_title)
                        actionableEmptyView.subtitle.setText(R.string.no_network_message)
                        actionableEmptyView.subtitle.visibility = View.VISIBLE
                        actionableEmptyView.button.visibility = View.VISIBLE
                    }
                    HistoryListStatus.ERROR -> {
                        actionableEmptyView.title.setText(R.string.no_network_title)
                        actionableEmptyView.subtitle.setText(R.string.error_generic_network)
                        actionableEmptyView.subtitle.visibility = View.VISIBLE
                        actionableEmptyView.button.visibility = View.VISIBLE
                    }
                }
            }
        })

        viewModel.showDialog.observe(viewLifecycleOwner, Observer { showDialogItem ->
            if (showDialogItem != null && showDialogItem.historyListItem is Revision) {
                (activity as HistoryItemClickInterface).onHistoryItemClicked(
                    showDialogItem.historyListItem,
                    showDialogItem.revisionsList
                )
            }
        })

        viewModel.post.observe(viewLifecycleOwner, Observer { post ->
            actionableEmptyView.subtitle.text = if (post?.isPage == true) {
                getString(R.string.history_empty_subtitle_page)
            } else {
                getString(R.string.history_empty_subtitle_post)
            }
        })
    }
}
