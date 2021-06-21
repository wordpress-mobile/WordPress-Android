package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.CommentListFragmentBinding
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class UnifiedCommentListFragment : Fragment(R.layout.comment_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var snackbarSequencer: SnackbarSequencer

    private lateinit var viewModel: UnifiedCommentListViewModel
    private lateinit var adapter: UnifiedCommentListAdapter
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private var binding: CommentListFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(UnifiedCommentListViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = CommentListFragmentBinding.bind(view).apply {
            setupContentViews()
            setupObservers()
        }
    }

    private fun CommentListFragmentBinding.setupContentViews() {
        val layoutManager = LinearLayoutManager(context)
        commentsRecyclerView.layoutManager = layoutManager
        commentsRecyclerView.addItemDecoration(UnifiedCommentListItemDecoration(commentsRecyclerView.context))

        adapter = UnifiedCommentListAdapter(requireContext())
        commentsRecyclerView.adapter = adapter.withLoadStateFooter(CommentListLoadingStateAdapter { retry() })

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(ptrLayout) {
            adapter.refresh()
        }
    }

    private fun retry() {
        adapter.retry()
    }

    private fun CommentListFragmentBinding.setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.commentListData.collect { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                setupCommentsList(uiState.commentsListUiModel)
            }
        }

        lifecycleScope.launchWhenStarted {
            adapter.loadStateFlow.collectLatest { loadState ->
                viewModel.onLoadStateChanged(loadState.toPagedListLoadingState(adapter.itemCount > 0))
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.onSnackbarMessage.collect { snackbarMessage ->
                snackbarSequencer.enqueue(
                        SnackbarItem(
                                Info(
                                        view = coordinator,
                                        textRes = snackbarMessage.message,
                                        duration = Snackbar.LENGTH_LONG
                                ),
                                snackbarMessage.buttonTitle?.let {
                                    Action(
                                            textRes = snackbarMessage.buttonTitle,
                                            clickListener = View.OnClickListener { snackbarMessage.buttonAction() }
                                    )
                                },
                                dismissCallback = { _, _ -> snackbarMessage.onDismissAction() }
                        )
                )
            }
        }
    }

    private fun CommentListFragmentBinding.setupCommentsList(uiModel: CommentsListUiModel) {
        uiHelpers.updateVisibility(loadingView, uiModel == CommentsListUiModel.Loading)
        uiHelpers.updateVisibility(actionableEmptyView, uiModel is CommentsListUiModel.Empty)
        uiHelpers.updateVisibility(
                commentsRecyclerView,
                uiModel is CommentsListUiModel.WithData || uiModel is CommentsListUiModel.Refreshing
        )
        ptrLayout.isRefreshing = uiModel is CommentsListUiModel.Refreshing

        when (uiModel) {
            is CommentsListUiModel.Empty -> {
                if (uiModel.image != null) {
                    actionableEmptyView.image.visibility = View.VISIBLE
                    actionableEmptyView.image.setImageResource(uiModel.image)
                } else {
                    actionableEmptyView.image.visibility = View.GONE
                }

                actionableEmptyView.title.text = uiHelpers.getTextOfUiString(
                        requireContext(),
                        uiModel.title
                )
            }

            else -> { // noop
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(): UnifiedCommentListFragment {
            val args = Bundle()
            val fragment = UnifiedCommentListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
