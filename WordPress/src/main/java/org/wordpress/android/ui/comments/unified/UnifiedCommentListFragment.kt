package org.wordpress.android.ui.comments.unified

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
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.UnifiedCommentListFragmentBinding
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.comments.unified.CommentDetailsActivityContract.CommentDetailsActivityRequest
import org.wordpress.android.ui.comments.unified.CommentDetailsActivityContract.CommentDetailsActivityResponse
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.ActionModeUiModel
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentList
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentsListUiModel
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentsListUiModel.WithData
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.ConfirmationDialogUiModel
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.ConfirmationDialogUiModel.Visible
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.config.UnifiedCommentsDetailFeatureConfig
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class UnifiedCommentListFragment : Fragment(R.layout.unified_comment_list_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var uiHelpers: UiHelpers
    @Inject
    lateinit var snackbarSequencer: SnackbarSequencer
    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository
    @Inject
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Inject
    lateinit var unifiedCommentsDetailFeatureConfig: UnifiedCommentsDetailFeatureConfig

    private lateinit var viewModel: UnifiedCommentListViewModel
    private lateinit var activityViewModel: UnifiedCommentActivityViewModel
    private lateinit var adapter: UnifiedCommentListAdapter
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

    private lateinit var commentListFilter: CommentFilter

    var confirmationDialog: AlertDialog? = null
    var currentSnackbar: Snackbar? = null

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
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                ptrLayout.isRefreshing = false
            }
        }
    }

    private fun UnifiedCommentListFragmentBinding.setupObservers() {
        var isShowingActionMode = false
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                setupCommentsList(uiState.commentsListUiModel)
                setupConfirmationDialog(uiState.confirmationDialogUiModel)
                setupCommentsAdapter(uiState.commentData, uiState.commentsListUiModel)
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

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
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
                                clickListener = { snackbarMessage.buttonAction() }
                            )
                        },
                        dismissCallback = { _, event ->
                            currentSnackbar = null
                            snackbarMessage.onDismissAction(event)
                        },
                        showCallback = { snackbar -> currentSnackbar = snackbar }
                    )
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.onCommentDetailsRequested.collect { selectedComment ->
                showCommentDetails(selectedComment.remoteCommentId, selectedComment.status)
            }
        }

        viewModel.start(commentListFilter)
    }

    private fun showCommentDetails(commentId: Long, commentStatus: CommentStatus) {
        currentSnackbar?.dismiss()
        if (unifiedCommentsDetailFeatureConfig.isEnabled()) {
            ActivityLauncher.viewUnifiedCommentsDetails(context, selectedSiteRepository.getSelectedSite())
        } else {
            commentDetails.launch(
                CommentDetailsActivityRequest(
                    commentId,
                    commentStatus,
                    selectedSiteRepository.getSelectedSite()!!
                )
            )
        }
    }

    val commentDetails = registerForActivityResult(
        CommentDetailsActivityContract()
    ) { response: CommentDetailsActivityResponse? ->
        if (response != null) {
            viewModel.performSingleCommentModeration(response.commentId, response.commentStatus)
        }
    }

    private fun UnifiedCommentListFragmentBinding.setupCommentsAdapter(
        commentList: CommentList,
        listUiModel: CommentsListUiModel
    ) {
        if (listUiModel is WithData || listUiModel is CommentsListUiModel.Empty) {
            val recyclerViewState = commentsRecyclerView.layoutManager?.onSaveInstanceState()
            commentsRecyclerView.post {
                adapter.submitList(commentList.comments)
                commentsRecyclerView.post {
                    (commentsRecyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                        if (layoutManager.findFirstVisibleItemPosition() <
                            MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION
                        ) {
                            layoutManager.onRestoreInstanceState(recyclerViewState)
                        }
                    }
                }
            }
        }
    }

    private fun UnifiedCommentListFragmentBinding.setupCommentsList(uiModel: CommentsListUiModel) {
        uiHelpers.updateVisibility(loadingView, uiModel == CommentsListUiModel.Loading)
        uiHelpers.updateVisibility(actionableEmptyView, uiModel is CommentsListUiModel.Empty)
        uiHelpers.updateVisibility(
            commentsRecyclerView, uiModel is WithData || uiModel is CommentsListUiModel.Refreshing
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

    private fun setupConfirmationDialog(uiModel: ConfirmationDialogUiModel) {
        if (uiModel is Visible) {
            val dialogBuilder: Builder = MaterialAlertDialogBuilder(requireContext())
            dialogBuilder.setTitle(uiModel.title)
            dialogBuilder.setMessage(uiModel.message)
            dialogBuilder.setPositiveButton(uiModel.positiveButton) { _, _ -> uiModel.confirmAction.invoke() }
            dialogBuilder.setNegativeButton(uiModel.negativeButton) { _, _ -> uiModel.cancelAction.invoke() }
            dialogBuilder.setCancelable(true)
            dialogBuilder.setOnCancelListener { uiModel.cancelAction.invoke() }
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
        private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 4

        fun newInstance(filter: CommentFilter) = UnifiedCommentListFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_COMMENT_LIST_FILTER, filter)
            }
        }
    }
}
