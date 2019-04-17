package org.wordpress.android.ui.posts

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.posts.adapters.PostListAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout
import org.wordpress.android.viewmodel.posts.PagedPostList
import org.wordpress.android.viewmodel.posts.PostListEmptyUiState
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

private const val EXTRA_POST_LIST_AUTHOR_FILTER = "post_list_author_filter"
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

    private lateinit var nonNullActivity: FragmentActivity
    // TODO: We can get rid of SiteModel in the Fragment once we remove the `isPhotonCapable`
    private lateinit var site: SiteModel
    private lateinit var postListAdapter: PostListAdapter

    private val postViewHolderConfig: PostViewHolderConfig by lazy {
        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val contentSpacing = nonNullActivity.resources.getDimensionPixelSize(R.dimen.content_margin)
        PostViewHolderConfig(
                // endList indicator height is hard-coded here so that its horizontal line is in the middle of the fab
                endlistIndicatorHeight = DisplayUtils.dpToPx(context, 74),
                photonWidth = displayWidth - contentSpacing * 2,
                photonHeight = nonNullActivity.resources.getDimensionPixelSize(R.dimen.reader_featured_image_height),
                isPhotonCapable = SiteUtils.isPhotonCapable(site),
                imageManager = imageManager
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

        val authorFilter: AuthorFilterSelection = requireNotNull(arguments)
                .getSerializable(EXTRA_POST_LIST_AUTHOR_FILTER) as AuthorFilterSelection
        val postListType = requireNotNull(arguments).getSerializable(EXTRA_POST_LIST_TYPE) as PostListType
        val mainViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(PostListMainViewModel::class.java)


        postListAdapter = PostListAdapter(
                context = nonNullActivity,
                postViewHolderConfig = postViewHolderConfig,
                uiHelpers = uiHelpers,
                viewLayoutType = mainViewModel.viewLayoutType.value ?: ViewLayoutType.defaultValue
        )

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get<PostListViewModel>(PostListViewModel::class.java)
        viewModel.start(mainViewModel.getPostListViewModelConnector(authorFilter, postListType))
        viewModel.pagedListData.observe(this, Observer {
            it?.let { pagedListData -> updatePagedListData(pagedListData) }
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
        viewModel.scrollToPosition.observe(this, Observer {
            it?.let { index ->
                recyclerView?.scrollToPosition(index)
            }
        })
        mainViewModel.viewLayoutType.observe(this, Observer { viewLayoutType ->
            viewLayoutType?.let { nonNullViewLayoutType ->
                postListAdapter.updateViewLayoutType(nonNullViewLayoutType)
            }
        })
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
        val spacingVertical = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.addItemDecoration(RecyclerItemDecoration(0, spacingVertical))
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

    private fun updatePagedListData(pagedListData: PagedPostList) {
        postListAdapter.submitList(pagedListData)
    }

    private fun updateEmptyViewForState(state: PostListEmptyUiState) {
        actionableEmptyView?.let { emptyView ->
            if (state.emptyViewVisible) {
                emptyView.visibility = View.VISIBLE
                uiHelpers.setTextOrHide(emptyView.title, state.title)
                uiHelpers.setImageOrHide(emptyView.image, state.imgResId)
                setupButtonOrHide(emptyView.button, state.buttonText, state.onButtonClick)
            } else {
                emptyView.visibility = View.GONE
            }
        }
    }

    private fun setupButtonOrHide(
        buttonView: Button,
        text: UiString?,
        onButtonClick: (() -> Unit)?
    ) {
        uiHelpers.setTextOrHide(buttonView, text)
        buttonView.setOnClickListener { onButtonClick?.invoke() }
    }

    fun scrollToTargetPost(localPostId: LocalPostId) {
        viewModel.scrollToPost(localPostId)
    }

    companion object {
        const val TAG = "post_list_fragment_tag"

        @JvmStatic
        fun newInstance(
            site: SiteModel,
            authorFilter: AuthorFilterSelection,
            postListType: PostListType
        ): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            bundle.putSerializable(EXTRA_POST_LIST_AUTHOR_FILTER, authorFilter)
            bundle.putSerializable(EXTRA_POST_LIST_TYPE, postListType)
            fragment.arguments = bundle
            return fragment
        }
    }
}
