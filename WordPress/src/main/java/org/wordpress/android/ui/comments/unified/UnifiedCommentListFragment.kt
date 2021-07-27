package org.wordpress.android.ui.comments.unified

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentListFragmentBinding
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.comments.CommentsDetailActivity
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ActionModeUiModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel.WithData
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ConfirmationDialogUiModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.ConfirmationDialogUiModel.Visible
import org.wordpress.android.ui.mysite.SelectedSiteRepository
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
    @Inject lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var viewModel: UnifiedCommentListViewModel
    private lateinit var activityViewModel: UnifiedCommentActivityViewModel
    private lateinit var adapter: UnifiedCommentListAdapter
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private lateinit var commentListFilter: CommentFilter

    var confirmationDialog: AlertDialog? = null

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
            viewModel.reload()
        }
    }

    private fun UnifiedCommentListFragmentBinding.setupObservers() {
        var isShowingActionMode = false

        viewModel.setup(commentListFilter)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                setupCommentsList(uiState.commentsListUiModel)
                setupConfirmationDialog(uiState.confirmationDialogUiModel)
                if (uiState.commentsListUiModel is WithData || uiState.commentsListUiModel is CommentsListUiModel.Empty) {
                    commentsRecyclerView.post {
                        adapter.submitList(uiState.commentData)
                    }
                }
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

        lifecycleScope.launchWhenStarted {
            viewModel.onCommentDetailsRequested.collect { selectedComment ->
                showCommentDetails(selectedComment.remoteCommentId, selectedComment.status)
            }
        }
    }

    fun showCommentDetails(commentId: Long, commentStatus: CommentStatus) {
        val detailIntent = Intent(requireActivity(), CommentsDetailActivity::class.java)
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_ID_EXTRA, commentId)
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_STATUS_FILTER_EXTRA, commentStatus)
        detailIntent.putExtra(WordPress.SITE, selectedSiteRepository.getSelectedSite())
        startActivityForResult(detailIntent, 1)
    }

    private fun UnifiedCommentListFragmentBinding.setupCommentsList(uiModel: CommentsListUiModel) {
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

    private fun UnifiedCommentListFragmentBinding.setupConfirmationDialog(uiModel: ConfirmationDialogUiModel) {
        if (uiModel is Visible) {
            val dialogBuilder: Builder = MaterialAlertDialogBuilder(requireContext())
            dialogBuilder.setTitle(uiModel.title)
            dialogBuilder.setMessage(uiModel.message)
            dialogBuilder.setPositiveButton(uiModel.positiveButton) { _, _ -> uiModel.confirmAction.invoke() }
            dialogBuilder.setNegativeButton(uiModel.negativeButton) { _, _ -> uiModel.cancelAction.invoke() }
            dialogBuilder.setCancelable(true)
            dialogBuilder.setOnDismissListener { uiModel.cancelAction.invoke() }
            confirmationDialog = dialogBuilder.create()
            confirmationDialog!!.show()
        } else {
            confirmationDialog?.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        confirmationDialog?.dismiss()
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
