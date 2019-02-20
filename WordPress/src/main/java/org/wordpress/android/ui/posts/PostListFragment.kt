package org.wordpress.android.ui.posts

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.adapters.PostListAdapter
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import org.wordpress.android.viewmodel.posts.PagedPostList
import org.wordpress.android.viewmodel.posts.PostListViewModel
import org.wordpress.android.viewmodel.posts.PostListViewModel.PostListEmptyUiState
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

private const val EXTRA_POST_LIST_TYPE = "post_list_type"

class PostListFragment : Fragment() {
    @Inject internal lateinit var imageManager: ImageManager
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: PostListViewModel

    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var actionableEmptyView: ActionableEmptyView? = null
    private var progressLoadMore: ProgressBar? = null

    private lateinit var nonNullActivity: Activity
    private lateinit var site: SiteModel

    private val postViewHolderConfig: PostViewHolderConfig by lazy {
        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val contentSpacing = nonNullActivity.resources.getDimensionPixelSize(R.dimen.content_margin)
        PostViewHolderConfig(
                // endlist indicator height is hard-coded here so that its horizontal line is in the middle of the fab
                endlistIndicatorHeight = DisplayUtils.dpToPx(context, 74),
                photonWidth = displayWidth - contentSpacing * 2,
                photonHeight = nonNullActivity.resources.getDimensionPixelSize(R.dimen.reader_featured_image_height),
                isPhotonCapable = SiteUtils.isPhotonCapable(site),
                showAllButtons = displayWidth >= 1080, // on larger displays we can always show all buttons
                imageManager = imageManager,
                isAztecEditorEnabled = AppPrefs.isAztecEditorEnabled(),
                hasCapabilityPublishPosts = site.hasCapabilityPublishPosts
        )
    }

    private val postListAdapter: PostListAdapter by lazy {
        PostListAdapter(
                context = nonNullActivity,
                postViewHolderConfig = postViewHolderConfig
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as WordPress).component().inject(this)

        val site: SiteModel? = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(nonNullActivity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel?
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
        }

        if (site == null) {
            ToastUtils.showToast(nonNullActivity, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            nonNullActivity.finish()
        } else {
            this.site = site
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetLocalPostId = activity?.intent?.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, -1)?.let {
            if (it != -1) it else null
        }
        val postListType = requireNotNull(arguments).getSerializable(EXTRA_POST_LIST_TYPE) as PostListType
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get<PostListViewModel>(PostListViewModel::class.java)
        viewModel.start(site, postListType, targetLocalPostId)
        viewModel.pagedListDataAndScrollPosition.observe(this, Observer {
            it?.let { (pagedListData, scrollPosition) -> updatePagedListData(pagedListData, scrollPosition) }
        })
        viewModel.emptyViewState.observe(this, Observer {
            it?.let { emptyViewState -> updateEmptyViewForState(emptyViewState) }
        })
        viewModel.isFetchingFirstPage.observe(this, Observer {
            swipeRefreshLayout?.isRefreshing = it == true
        })
        viewModel.isLoadingMore.observe(this, Observer {
            progressLoadMore?.visibility = if (it == true) View.VISIBLE else View.GONE
        })
        viewModel.postListAction.observe(this, Observer {
            it?.let { action -> handlePostListAction(requireActivity(), action) }
        })
        viewModel.postUploadAction.observe(this, Observer {
            it?.let { uploadAction -> handleUploadAction(uploadAction) }
        })
        viewModel.toastMessage.observe(this, Observer {
            it?.show(nonNullActivity)
        })
        viewModel.snackbarAction.observe(this, Observer {
            it?.let { snackbarHolder -> showSnackbar(snackbarHolder) }
        })
        viewModel.dialogAction.observe(this, Observer {
            val fragmentManager = requireNotNull(fragmentManager) { "FragmentManager can't be null at this point" }
            it?.show(nonNullActivity, fragmentManager, uiHelpers)
        })
    }

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        nonNullActivity.findViewById<View>(R.id.root_view)?.let { parent ->
            val message = getString(holder.messageRes)
            val duration = AccessibilityUtils.getSnackbarDuration(nonNullActivity)
            val snackbar = Snackbar.make(parent, message, duration)
            if (holder.buttonTitleRes != null) {
                snackbar.setAction(getString(holder.buttonTitleRes)) {
                    holder.buttonAction()
                }
            }
            snackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    holder.onDismissAction()
                    super.onDismissed(transientBottomBar, event)
                }
            })
            snackbar.show()
        }
    }

    private fun handleUploadAction(action: PostUploadAction) {
        when (action) {
            is PostUploadAction.EditPostResult -> {
                UploadUtils.handleEditPostResultSnackbars(
                        nonNullActivity,
                        nonNullActivity.findViewById(R.id.coordinator),
                        action.data,
                        action.post,
                        action.site
                ) {
                    action.publishAction()
                }
            }
            is PostUploadAction.PublishPost -> {
                UploadUtils.publishPost(
                        nonNullActivity,
                        action.post,
                        action.site,
                        action.dispatcher
                )
            }
            is PostUploadAction.PostUploadedSnackbar -> {
                UploadUtils.onPostUploadedSnackbarHandler(
                        nonNullActivity,
                        nonNullActivity.findViewById(R.id.coordinator),
                        action.isError,
                        action.post,
                        action.errorMessage,
                        action.site,
                        action.dispatcher
                )
            }
            is PostUploadAction.MediaUploadedSnackbar -> {
                UploadUtils.onMediaUploadedSnackbarHandler(
                        nonNullActivity,
                        nonNullActivity.findViewById(R.id.coordinator),
                        action.isError,
                        action.mediaList,
                        action.site,
                        action.message
                )
            }
            is PostUploadAction.CancelPostAndMediaUpload -> {
                UploadService.cancelQueuedPostUploadAndRelatedMedia(nonNullActivity, action.post)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, site)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.post_list_fragment, container, false)

        swipeRefreshLayout = view.findViewById(R.id.ptr_layout)
        recyclerView = view.findViewById(R.id.recycler_view)
        progressLoadMore = view.findViewById(R.id.progress)
        actionableEmptyView = view.findViewById(R.id.actionable_empty_view)

        val context = nonNullActivity
        val spacingVertical = context.resources.getDimensionPixelSize(R.dimen.card_gutters)
        val spacingHorizontal = context.resources.getDimensionPixelSize(R.dimen.content_margin)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.addItemDecoration(RecyclerItemDecoration(spacingHorizontal, spacingVertical))
        recyclerView?.adapter = postListAdapter

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (!NetworkUtils.isNetworkAvailable(nonNullActivity)) {
                swipeRefreshLayout?.isRefreshing = false
            } else {
                viewModel.fetchFirstPage()
            }
        }
        return view
    }

    fun handleEditPostResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (data != null && EditPostActivity.checkToRestart(data)) {
                ActivityLauncher.editPostOrPageForResult(data, nonNullActivity, site,
                        data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0))

                // a restart will happen so, no need to continue here
                return
            }

            viewModel.handleEditPostResult(data)
        }
    }

    private fun updatePagedListData(pagedListData: PagedPostList, scrollPosition: Int?) {
        postListAdapter.submitList(pagedListData)
        scrollPosition?.let { recyclerView?.smoothScrollToPosition(it) }
    }

    private fun updateEmptyViewForState(state: PostListEmptyUiState) {
        if (state.listVisible) {
            actionableEmptyView?.visibility = View.GONE
        } else {
            actionableEmptyView?.visibility = View.VISIBLE
            actionableEmptyView?.let { view ->
                setTextOrHide(view.title, state.titleResId)
                setImageOrHide(view.image, state.imgResId)
                setupButtonOrHide(view.button, state.buttonTextResId, state.onButtonClick)
            }
        }
    }

    private fun setupButtonOrHide(
        buttonView: Button,
        textResId: Int?,
        onButtonClick: (() -> Unit)?
    ) {
        buttonView.visibility = if (textResId == null) View.GONE else View.VISIBLE
        textResId?.let {
            buttonView.setText(textResId)
            buttonView.setOnClickListener { onButtonClick?.invoke() }
        }
    }

    private fun setImageOrHide(imageView: ImageView, resId: Int?) {
        imageView.visibility = if (resId == null) View.GONE else View.VISIBLE
        resId?.let {
            imageView.setImageResource(resId)
        }
    }

    private fun setTextOrHide(textView: TextView, resId: Int?) {
        textView.visibility = if (resId == null) View.GONE else View.VISIBLE
        resId?.let {
            textView.text = resources.getString(resId)
        }
    }

    fun onPositiveClickedForBasicDialog(instanceTag: String) {
        viewModel.onPositiveClickedForBasicDialog(instanceTag)
    }

    fun onNegativeClickedForBasicDialog(instanceTag: String) {
        viewModel.onNegativeClickedForBasicDialog(instanceTag)
    }

    fun onDismissByOutsideTouchForBasicDialog(instanceTag: String) {
        viewModel.onDismissByOutsideTouchForBasicDialog(instanceTag)
    }

    companion object {
        const val TAG = "post_list_fragment_tag"

        @JvmStatic
        fun newInstance(site: SiteModel, postListType: PostListType, targetPost: PostModel?): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            bundle.putSerializable(EXTRA_POST_LIST_TYPE, postListType)
            targetPost?.let {
                bundle.putInt(EXTRA_TARGET_POST_LOCAL_ID, it.id)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
