package org.wordpress.android.ui.posts

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PostListViewLayoutType.COMPACT
import org.wordpress.android.ui.posts.PostListViewLayoutType.STANDARD
import org.wordpress.android.ui.posts.adapters.PostListAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.NetworkUtils
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

private const val EXTRA_POST_LIST_TYPE = "post_list_type"
private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

class PostListFragment : Fragment() {
    @Inject internal lateinit var imageManager: ImageManager
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: PostListViewModel
    private lateinit var mainViewModel: PostListMainViewModel

    private var swipeToRefreshHelper: SwipeToRefreshHelper? = null

    private var swipeRefreshLayout: CustomSwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var actionableEmptyView: ActionableEmptyView? = null
    private var progressLoadMore: ProgressBar? = null

    private lateinit var itemDecorationCompactLayout: RecyclerItemDecoration
    private lateinit var itemDecorationStandardLayout: RecyclerItemDecoration

    private lateinit var postListType: PostListType

    private lateinit var nonNullActivity: FragmentActivity

    private val postListAdapter: PostListAdapter by lazy {
        PostListAdapter(
                context = nonNullActivity,
                imageManager = imageManager,
                uiHelpers = uiHelpers,
                postListFragment = this
        )
    }

    public var canStartEditPostActivity= true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component().inject(this)

        val nonNullIntent = checkNotNull(nonNullActivity.intent)
        val site: SiteModel? = nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel?

        if (site == null) {
            ToastUtils.showToast(nonNullActivity, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            nonNullActivity.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postListType = requireNotNull(arguments).getSerializable(EXTRA_POST_LIST_TYPE) as PostListType

        mainViewModel = ViewModelProviders.of(nonNullActivity, viewModelFactory)
                .get(PostListMainViewModel::class.java)

        mainViewModel.viewLayoutType.observe(viewLifecycleOwner, Observer { optionaLayoutType ->
            optionaLayoutType?.let { layoutType ->
                recyclerView?.removeItemDecoration(itemDecorationCompactLayout)
                recyclerView?.removeItemDecoration(itemDecorationStandardLayout)

                when (layoutType) {
                    STANDARD -> {
                        recyclerView?.addItemDecoration(itemDecorationStandardLayout)
                    }
                    COMPACT -> {
                        recyclerView?.addItemDecoration(itemDecorationCompactLayout)
                    }
                }

                if (postListAdapter.updateItemLayoutType(layoutType)) {
                    recyclerView?.scrollToPosition(0)
                }
            }
        })

        mainViewModel.authorSelectionUpdated.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                if (viewModel.updateAuthorFilterIfNotSearch(it)) {
                    recyclerView?.scrollToPosition(0)
                }
            }
        })

        actionableEmptyView?.updateLayoutForSearch(postListType == SEARCH, 0)

        val postListViewModelConnector = mainViewModel.getPostListViewModelConnector(postListType)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(PostListViewModel::class.java)

        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val contentSpacing = nonNullActivity.resources.getDimensionPixelSize(R.dimen.content_margin)

        // since the MainViewModel has been already started, we need to manually update the authorFilterSelection value
        viewModel.start(
                postListViewModelConnector,
                mainViewModel.authorSelectionUpdated.value!!,
                photonWidth = displayWidth - contentSpacing * 2,
                photonHeight = nonNullActivity.resources.getDimensionPixelSize(R.dimen.reader_featured_image_height)
        )

        initObservers()
    }

    override fun onResume() {
        super.onResume()

        canStartEditPostActivity = true
    }

    private fun initObservers() {
        if (postListType == SEARCH) {
            mainViewModel.searchQuery.observe(viewLifecycleOwner, Observer {
                if (TextUtils.isEmpty(it)) {
                    postListAdapter.submitList(null)
                }
                viewModel.search(it)
            })
        }

        viewModel.emptyViewState.observe(viewLifecycleOwner, Observer {
            it?.let { emptyViewState -> updateEmptyViewForState(emptyViewState) }
        })

        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner, Observer {
            swipeRefreshLayout?.isRefreshing = it == true
        })

        viewModel.pagedListData.observe(viewLifecycleOwner, Observer {
            it?.let { pagedListData -> updatePagedListData(pagedListData) }
        })

        viewModel.isLoadingMore.observe(viewLifecycleOwner, Observer {
            progressLoadMore?.visibility = if (it == true) View.VISIBLE else View.GONE
        })
        viewModel.scrollToPosition.observe(viewLifecycleOwner, Observer {
            it?.let { index ->
                recyclerView?.scrollToPosition(index)
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.post_list_fragment, container, false)

        swipeRefreshLayout = view.findViewById(R.id.ptr_layout)
        recyclerView = view.findViewById(R.id.recycler_view)
        progressLoadMore = view.findViewById(R.id.progress)
        actionableEmptyView = view.findViewById(R.id.actionable_empty_view)

        val context = nonNullActivity
        itemDecorationStandardLayout = RecyclerItemDecoration(
                0,
                context.resources.getDimensionPixelSize(R.dimen.margin_medium)
        )
        itemDecorationCompactLayout = RecyclerItemDecoration(
                0,
                context.resources.getDimensionPixelSize(R.dimen.list_divider_height)
        )
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.adapter = postListAdapter

        swipeToRefreshHelper = buildSwipeToRefreshHelper(swipeRefreshLayout) {
            if (!NetworkUtils.isNetworkAvailable(nonNullActivity)) {
                swipeRefreshLayout?.isRefreshing = false
            } else {
                viewModel.swipeToRefresh()
            }
        }
        return view
    }

    /**
     * Updates the data for the adapter while retaining visible item in certain cases.
     *
     * PagedList tries to keep the visible item by adding new items outside of the screen while modifying the scroll
     * position. In most cases, this works out great because it doesn't interrupt to the user. However, after a new
     * item is inserted at the top while the list is showing the very first items, it feels very weird to not have the
     * inserted item shown. For example, if a new draft is added there is no indication of it except for the flash
     * of the scroll bar which is not noticeable unless user is paying attention to it.
     *
     * In these cases, we try to keep the scroll position the same instead of keeping the visible item the same by
     * first saving the state and re-applying it after the data updates are completed. Since `PagedListAdapter` uses
     * bg thread to calculate the changes, we need to post the change in the bg thread as well so that it'll be applied
     * after changes are reflected.
     */
    private fun updatePagedListData(pagedListData: PagedPostList) {
        val recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
        postListAdapter.submitList(pagedListData)
        recyclerView?.post {
            (recyclerView?.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                if (layoutManager.findFirstVisibleItemPosition() < MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION) {
                    layoutManager.onRestoreInstanceState(recyclerViewState)
                }
            }
        }
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
            postListType: PostListType
        ): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            bundle.putSerializable(EXTRA_POST_LIST_TYPE, postListType)
            fragment.arguments = bundle
            return fragment
        }
    }
}
