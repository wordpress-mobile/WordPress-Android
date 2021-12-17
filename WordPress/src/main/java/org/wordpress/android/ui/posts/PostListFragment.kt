package org.wordpress.android.ui.posts

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.ViewPagerFragment
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
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.viewmodel.posts.PostListViewModel
import org.wordpress.android.widgets.RecyclerItemDecoration
import javax.inject.Inject

private const val EXTRA_POST_LIST_TYPE = "post_list_type"
private const val MAX_INDEX_FOR_VISIBLE_ITEM_TO_KEEP_SCROLL_POSITION = 2

class PostListFragment : ViewPagerFragment() {
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
                uiHelpers = uiHelpers
        )
    }

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

    override fun getScrollableViewForUniqueIdProvision(): View? {
        if (postListType == SEARCH) {
            return null
        }
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postListType = requireNotNull(arguments).getSerializable(EXTRA_POST_LIST_TYPE) as PostListType

        val targetPostId = requireNotNull(arguments).getSerializable(EXTRA_TARGET_POST_REMOTE_ID) as Int?
        val targetPostListType = requireNotNull(arguments).getSerializable(EXTRA_TARGET_POST_LIST_TYPE) as PostListType?

        if (postListType == SEARCH) {
            recyclerView?.id = R.id.posts_search_recycler_view_id
        }

        mainViewModel = ViewModelProvider(nonNullActivity, viewModelFactory)
                .get(PostListMainViewModel::class.java)

        mainViewModel.viewLayoutType.observe(viewLifecycleOwner, { optionalLayoutType ->
            optionalLayoutType?.let { layoutType ->
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

        mainViewModel.authorSelectionUpdated.observe(viewLifecycleOwner, {
            if (it != null) {
                if (viewModel.updateAuthorFilterIfNotSearch(it)) {
                    recyclerView?.scrollToPosition(0)
                }
            }
        })

        actionableEmptyView?.updateLayoutForSearch(postListType == SEARCH, 0)

        val postListViewModelConnector = mainViewModel.getPostListViewModelConnector(postListType)

        viewModel = ViewModelProvider(this, viewModelFactory).get(PostListViewModel::class.java)

        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val contentSpacing = nonNullActivity.resources.getDimensionPixelSize(R.dimen.content_margin)

        // since the MainViewModel has been already started, we need to manually update the authorFilterSelection value
        viewModel.start(
                postListViewModelConnector = postListViewModelConnector,
                value = mainViewModel.authorSelectionUpdated.value!!,
                photonWidth = displayWidth - contentSpacing * 2,
                photonHeight = nonNullActivity.resources.getDimensionPixelSize(R.dimen.reader_featured_image_height),
                navigateToRemotePostId = Pair(
                        targetPostId?.let { RemotePostId(RemoteId(it.toLong())) },
                        if (postListType == targetPostListType) targetPostListType else null
                )
        )

        initObservers()
    }

    private fun initObservers() {
        if (postListType == SEARCH) {
            mainViewModel.searchQuery.observe(viewLifecycleOwner, {
                if (TextUtils.isEmpty(it)) {
                    postListAdapter.submitList(null)
                }
                viewModel.search(it)
            })
        }

        viewModel.emptyViewState.observe(viewLifecycleOwner, {
            it?.let { emptyViewState -> updateEmptyViewForState(emptyViewState) }
        })

        viewModel.isFetchingFirstPage.observe(viewLifecycleOwner, {
            swipeRefreshLayout?.isRefreshing = it == true
        })

        viewModel.pagedListData.observe(viewLifecycleOwner, {
            it?.let { pagedListData -> updatePagedListData(pagedListData) }
        })

        viewModel.isLoadingMore.observe(viewLifecycleOwner, {
            progressLoadMore?.visibility = if (it == true) View.VISIBLE else View.GONE
        })
        viewModel.scrollToPosition.observe(viewLifecycleOwner, {
            it?.let { index ->
                recyclerView?.scrollToPosition(index)
            }
        })
        viewModel.navigateToPost.observe(viewLifecycleOwner, {
            it?.let { index ->
                navigateToPost(index)
            }
        })
    }

    /**
     * In addition to the [PostListViewModel.navigateToPost] and its logic, which includes:
     * - The [PostListViewModel.navigateToEditPost] to skip the first result, and
     * - The [PostListViewModel.navigateToRemotePostId] to reset the navigation trigger.
     * An extra delay of [NAVIGATE_TO_EDIT_POST_DELAY_MS] milliseconds it added to make sure that the list is submitted
     * to the [PostListAdapter] paging related adapter (see [PagedListAdapter]). This will make sure that the newly
     * submitted list updated the existing and potentially stale list.
     *
     * Otherwise, and due to the fact that the list submission can take some additional time, selecting the item might
     * launch the [EditPostActivity] with a stale item and thus this might overwrite any remote changes.
     *
     * PS: A delay of half a second would have been sufficient enough, based on the testing that was done, but doubling
     * this number to 1 second seemed safer to to make sure that no there will be less changes for the stale item to get
     * selected.
     *
     * NOTE: This whole solution is hacky and done this way to help us achieve a quick result, without needing to
     * update the paging library or the underneath list store related implementation.
     */
    private fun navigateToPost(index: Int) {
        recyclerView?.postDelayed({
            recyclerView?.layoutManager?.findViewByPosition(index)?.performClick()
        }, NAVIGATE_TO_EDIT_POST_DELAY_MS)
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
        private const val NAVIGATE_TO_EDIT_POST_DELAY_MS = 1000L

        @JvmStatic
        fun newInstance(
            site: SiteModel,
            postListType: PostListType,
            targetPostId: Int? = null,
            targetPostListType: PostListType? = null
        ): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            bundle.putSerializable(EXTRA_POST_LIST_TYPE, postListType)
            bundle.putSerializable(EXTRA_TARGET_POST_REMOTE_ID, targetPostId)
            bundle.putSerializable(EXTRA_TARGET_POST_LIST_TYPE, targetPostListType)
            fragment.arguments = bundle
            return fragment
        }
    }
}
