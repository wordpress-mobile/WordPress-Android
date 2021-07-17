package org.wordpress.android.ui.comments.unified

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentListFragmentBinding
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class UnifiedCommentListFragment : Fragment(R.layout.unified_comment_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var snackbarSequencer: SnackbarSequencer

    private lateinit var viewModel: UnifiedCommentListViewModel
    private lateinit var activityViewModel: UnifiedCommentActivityViewModel
    private lateinit var adapter: UnifiedCommentListAdapter
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private lateinit var commentListFilter: CommentFilter
    private lateinit var commentStatusList: List<CommentStatus>

    private var binding: UnifiedCommentListFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(UnifiedCommentListViewModel::class.java)
        activityViewModel = ViewModelProvider(
                requireActivity(),
                viewModelFactory
        ).get(UnifiedCommentActivityViewModel::class.java)
        arguments?.let {
            commentListFilter = it.getSerializable(KEY_COMMENT_LIST_FILTER) as CommentFilter
            commentStatusList = listOf(APPROVED, UNAPPROVED) // TODO: logic must be changed, here for testing purposes
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UnifiedCommentListFragmentBinding.bind(view).apply {
            setupContentViews()
            setupObservers()
        }
    }

    private fun UnifiedCommentListFragmentBinding.setupContentViews() {
        val layoutManager = LinearLayoutManager(context)
        commentsRecyclerView.layoutManager = layoutManager
        commentsRecyclerView.addItemDecoration(UnifiedCommentListItemDecoration(commentsRecyclerView.context))

        adapter = UnifiedCommentListAdapter(requireContext())
        commentsRecyclerView.adapter = adapter

        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(ptrLayout) {
        }
    }

    private fun retry() {
//        adapter.retry()
    }

    private fun UnifiedCommentListFragmentBinding.setupObservers() {
        viewModel.setup(commentListFilter, commentStatusList)
        var isShowingActionMode = false
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                setupCommentsList(uiState.commentsListUiModel)
                adapter.submitList(uiState.commentData)
                if (uiState.actionModeUiModel is ActionModeUiModel.Visible && !isShowingActionMode) {
                    isShowingActionMode = true
                    (activity as AppCompatActivity).startSupportActionMode(
                            CommentListActionModeCallback(
                                    viewModel,
                                    activityViewModel
                            )
                    )
                } else if (uiState.actionModeUiModel is ActionModeUiModel.Hidden && isShowingActionMode) {
                    isShowingActionMode = false
                }
            }
        }

//        lifecycleScope.launchWhenStarted {
//            adapter.loadStateFlow.collectLatest { loadState ->
//                viewModel.onLoadStateChanged(loadState.toPagedListLoadingState(adapter.itemCount > 0))
//            }
//        }

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

    private fun UnifiedCommentListFragmentBinding.setupCommentsList(uiModel: CommentsListUiModel) {
//        uiHelpers.updateVisibility(loadingView, uiModel == CommentsListUiModel.Loading)
//        uiHelpers.updateVisibility(actionableEmptyView, uiModel is CommentsListUiModel.Empty)
//        uiHelpers.updateVisibility(
//                commentsRecyclerView,
//                uiModel is CommentsListUiModel.WithData || uiModel is CommentsListUiModel.Refreshing
//        )
//        ptrLayout.isRefreshing = uiModel is CommentsListUiModel.Refreshing
//
//        when (uiModel) {
//            is CommentsListUiModel.Empty -> {
//                if (uiModel.image != null) {
//                    actionableEmptyView.image.visibility = View.VISIBLE
//                    actionableEmptyView.image.setImageResource(uiModel.image)
//                } else {
//                    actionableEmptyView.image.visibility = View.GONE
//                }
//
//                actionableEmptyView.title.text = uiHelpers.getTextOfUiString(
//                        requireContext(),
//                        uiModel.title
//                )
//            }
//
//            else -> { // noop
//            }
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val KEY_COMMENT_LIST_FILTER = "KEY_COMMENT_LIST_FILTER"

        fun newInstance(filter: CommentFilter) = UnifiedCommentListFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_COMMENT_LIST_FILTER, filter)
            }
        }
    }
}
