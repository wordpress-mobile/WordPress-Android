package org.wordpress.android.ui.engagement

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.engagement.BottomSheetAction.HideBottomSheet
import org.wordpress.android.ui.engagement.BottomSheetAction.ShowBottomSheet
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewCommentInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewPostInReader
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteById
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.PreviewSiteByUrl
import org.wordpress.android.ui.engagement.EngagedListServiceRequestEvent.RequestBlogPost
import org.wordpress.android.ui.engagement.EngagedListServiceRequestEvent.RequestComment
import org.wordpress.android.ui.engagement.EngagedPeopleListViewModel.EngagedPeopleListUiState
import org.wordpress.android.ui.engagement.UserProfileViewModel.Companion.USER_PROFILE_VM_KEY
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.actions.ReaderPostActions
import org.wordpress.android.ui.reader.services.comment.ReaderCommentService
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Action
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class EngagedPeopleListFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var resourceProvider: ResourceProvider
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var snackbarSequencer: SnackbarSequencer
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var readerTracker: ReaderTracker
    @Inject lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var viewModel: EngagedPeopleListViewModel
    private lateinit var userProfileViewModel: UserProfileViewModel
    private lateinit var recycler: RecyclerView
    private lateinit var loadingView: View
    private lateinit var rootView: View
    private lateinit var emptyView: ActionableEmptyView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(EngagedPeopleListViewModel::class.java)
        userProfileViewModel = ViewModelProvider(this, viewModelFactory)
                .get(USER_PROFILE_VM_KEY, UserProfileViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.engaged_people_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        recycler = view.findViewById(R.id.recycler)
        loadingView = view.findViewById(R.id.loading_view)
        emptyView = view.findViewById(R.id.actionable_empty_view)

        val listScenario = requireArguments().getParcelable<ListScenario>(KEY_LIST_SCENARIO)

        val layoutManager = LinearLayoutManager(activity)

        savedInstanceState?.getParcelable<Parcelable>(KEY_LIST_STATE)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        recycler.layoutManager = layoutManager

        userProfileViewModel.onBottomSheetAction.observeEvent(viewLifecycleOwner, { state ->
            var bottomSheet = childFragmentManager.findFragmentByTag(USER_PROFILE_BOTTOM_SHEET_TAG)
                    as? UserProfileBottomSheetFragment

            when (state) {
                ShowBottomSheet -> {
                    if (bottomSheet == null) {
                        bottomSheet = UserProfileBottomSheetFragment.newInstance(USER_PROFILE_VM_KEY)
                        bottomSheet.show(childFragmentManager, USER_PROFILE_BOTTOM_SHEET_TAG)
                    }
                }
                HideBottomSheet -> {
                    bottomSheet?.apply { this.dismiss() }
                }
            }
        })

        viewModel.uiState.observe(viewLifecycleOwner, { state ->
            if (!isAdded) return@observe

            updateUiState(state)
        })

        viewModel.onNavigationEvent.observeEvent(viewLifecycleOwner, { event ->
            if (!isAdded) return@observeEvent

            manageNavigation(event)
        })

        viewModel.onSnackbarMessage.observeEvent(viewLifecycleOwner, { messageHolder ->
            if (!isAdded || !lifecycle.currentState.isAtLeast(State.RESUMED)) return@observeEvent

            showSnackbar(messageHolder)
        })

        viewModel.onServiceRequestEvent.observeEvent(viewLifecycleOwner, { serviceRequest ->
            if (!isAdded) return@observeEvent

            manageServiceRequest(serviceRequest)
        })

        viewModel.start(listScenario!!)
    }

    @Suppress("ForbiddenComment")
    private fun manageNavigation(event: EngagedListNavigationEvent) {
        with(requireActivity()) {
            if (this.isFinishing) return@with

            when (event) {
                is PreviewSiteById -> {
                    ReaderActivityLauncher.showReaderBlogPreview(
                            this,
                            event.siteId,
                            // TODO: this can be true if we use this fragment for NOTE_FOLLOW_TYPE notifications
                            false,
                            event.source,
                            readerTracker
                    )
                }
                is PreviewSiteByUrl -> {
                    val url = event.siteUrl
                    openUrl(this, url, event.source)
                }
                is PreviewCommentInReader -> {
                    ReaderActivityLauncher.showReaderComments(
                            this,
                            event.siteId,
                            event.commentPostId,
                            event.postOrCommentId,
                            event.source.sourceDescription
                    )
                }
                is PreviewPostInReader -> {
                    ReaderActivityLauncher.showReaderPostDetail(this, event.siteId, event.postId)
                }
                is OpenUserProfileBottomSheet -> {
                    userProfileViewModel.onBottomSheetOpen(event.userProfile, event.onClick, event.source)
                }
            }

            if (event.closeUserProfileIfOpened) {
                userProfileViewModel.onBottomSheetCancelled()
            }
        }
    }

    private fun updateUiState(state: EngagedPeopleListUiState) {
        uiHelpers.updateVisibility(loadingView, state.showLoading)

        setupAdapter(state.engageItemsList)

        if (state.showEmptyState) {
            uiHelpers.setTextOrHide(emptyView.title, state.emptyStateTitle)
            uiHelpers.setTextOrHide(emptyView.button, state.emptyStateButtonText)
            emptyView.button.setOnClickListener { state.emptyStateAction?.invoke() }

            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
        }
    }

    private fun manageServiceRequest(serviceRequest: EngagedListServiceRequestEvent) {
        with(requireActivity()) {
            if (this.isFinishing) return@with

            when (serviceRequest) {
                is RequestBlogPost -> ReaderPostActions.requestBlogPost(
                        serviceRequest.siteId,
                        serviceRequest.postId,
                        null
                )
                is RequestComment -> ReaderCommentService.startServiceForComment(
                        this,
                        serviceRequest.siteId,
                        serviceRequest.postId,
                        serviceRequest.commentId
                )
            }
        }
    }

    private fun openUrl(context: Context, url: String, source: String) {
        analyticsUtilsWrapper.trackBlogPreviewedByUrl(source)
        if (WPUrlUtils.isWordPressCom(url)) {
            WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(context, url)
        } else {
            WPWebViewActivity.openURL(context, url)
        }
    }

    private fun setupAdapter(items: List<EngageItem>) {
        val adapter = recycler.adapter as? EngagedPeopleAdapter ?: EngagedPeopleAdapter(
                imageManager,
                resourceProvider
        ).also {
            recycler.adapter = it
        }

        val recyclerViewState = recycler.layoutManager?.onSaveInstanceState()
        adapter.loadData(items)
        recycler.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        snackbarSequencer.enqueue(
                SnackbarItem(
                        Info(
                                view = rootView,
                                textRes = holder.message,
                                duration = Snackbar.LENGTH_LONG
                        ),
                        holder.buttonTitle?.let {
                            Action(
                                    textRes = holder.buttonTitle,
                                    clickListener = { holder.buttonAction() }
                            )
                        },
                        dismissCallback = { _, event -> holder.onDismissAction(event) }
                )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        recycler.layoutManager?.let {
            outState.putParcelable(KEY_LIST_STATE, it.onSaveInstanceState())
        }
    }

    companion object {
        private const val KEY_LIST_SCENARIO = "list_scenario"
        private const val KEY_LIST_STATE = "list_state"

        private const val USER_PROFILE_BOTTOM_SHEET_TAG = "USER_PROFILE_BOTTOM_SHEET_TAG"

        @JvmStatic
        fun newInstance(listScenario: ListScenario): EngagedPeopleListFragment {
            val args = Bundle()
            args.putParcelable(KEY_LIST_SCENARIO, listScenario)

            val fragment = EngagedPeopleListFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
