package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.LongSparseArray
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.prepublishing_categories_fragment.*
import kotlinx.android.synthetic.main.prepublishing_toolbar.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction.FETCH_CATEGORIES
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PrepublishingCategoriesAdapter.OnCategoryClickedListener
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.ADD_CATEGORY
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper
import org.wordpress.android.util.helpers.ListScrollPositionManager
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class PrepublishingCategoriesFragment : Fragment(R.layout.prepublishing_categories_fragment),
        OnCategoryClickedListener {
    private var closeListener: PrepublishingScreenClosedListener? = null
    private var actionListener: PrepublishingActionClickedListener? = null

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingCategoriesViewModel
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var dispatcher: Dispatcher

    private lateinit var listScrollPositionManager: ListScrollPositionManager
    private val selectedCategories = hashSetOf<Long>()
    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper
    private val categoryRemoteIdsToListPositions = LongSparseArray<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
        actionListener = parentFragment as PrepublishingActionClickedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
        actionListener = null
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
    }

    override fun onResume() {
        val needsRequestLayout = requireArguments().getBoolean(NEEDS_REQUEST_LAYOUT)
        if (needsRequestLayout) {
            requireActivity().window.decorView.requestLayout()
        }
        super.onResume()
    }

    companion object {
        const val TAG = "prepublishing_categories_fragment_tag"
        const val NEEDS_REQUEST_LAYOUT = "prepublishing_categories_fragment_needs_request_layout"
        @JvmStatic fun newInstance(
            site: SiteModel,
            needsRequestLayout: Boolean
        ): PrepublishingCategoriesFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putBoolean(NEEDS_REQUEST_LAYOUT, needsRequestLayout)
            }
            return PrepublishingCategoriesFragment().apply { arguments = bundle }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initBackButton()
        initAddCategoryButton()
        initRecyclerView()
        initSwipeToRefreshHelper()
        initViewModel()
        // todo: annmarie - these can go in the view model uiState
        initSelectedCategories()
        populateCategoryList()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initBackButton() {
        back_button.setOnClickListener {
            // todo: annmarie make this a viewModel thing
// todo: annmarie - this needs to be uncommented            updateSelectedCategoryList()
// todo: annmarie - this needs to be uncommented            viewModel.updateCategories(ArrayList(selectedCategories))
            viewModel.onBackButtonClicked()
        }
    }

    private fun initAddCategoryButton() {
        add_new_category.setOnClickListener {
            viewModel.onAddNewCategoryClicked()
        }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recycler_view.adapter = PrepublishingCategoriesAdapter(
                onCheckChangeListener = { id, checked ->
                    Log.i(javaClass.simpleName, "***=> I made it here $id and $checked")
                }, context = requireContext()
        )
        recycler_view.addItemDecoration(
                DividerItemDecoration(
                        recycler_view.context,
                        DividerItemDecoration.VERTICAL
                )
        )
    }

    private fun initSwipeToRefreshHelper() {
        swipeToRefreshHelper = buildSwipeToRefreshHelper(ptr_layout,
                SwipeToRefreshHelper.RefreshListener {
                    if (!NetworkUtils.checkConnection(requireContext())) {
                        swipeToRefreshHelper.isRefreshing = false
                        return@RefreshListener
                    }
                    refreshCategories()
                })
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingCategoriesViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.navigateToHomeScreen.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onBackClicked()
            }
        })

        viewModel.navigateToAddCategoryScreen.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                actionListener?.onActionClicked(ADD_CATEGORY)
            }
        })

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, Observer { uiString ->
            toolbar_title.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

//        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
//            when (uiState) {
//                is InitialLoadUiState -> {
//                }
//                is ContentUiState -> {
//
//                }
//            }
//        })

        val siteModel = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        viewModel.start(getEditPostRepository(), siteModel)
    }

    private fun initSelectedCategories() {
        // this can go in the UiState
        val post = getEditPostRepository().getPost()
        if (post != null) {
            selectedCategories.addAll(post.categoryIdList)
        }
    }

    private fun refreshCategories() {
        swipeToRefreshHelper.isRefreshing = true
        listScrollPositionManager.saveScrollOffset()
        updateSelectedCategoryList()
        viewModel.fetchNewCategories()
    }

    // todo: annmarie - incomment this below and get it working
    private fun updateSelectedCategoryList() {
//        val selectedItems: SparseBooleanArray = recycler_view.adapter
//        val categoryLevels = viewModel.getCategoryLevels()
//        for (i in 0 until selectedItems.size()) {
//            if (selectedItems.keyAt(i) >= categoryLevels.size) {
//                continue
//            }
//            val categoryRemoteId: Long = categoryLevels[selectedItems.keyAt(i)]
//                    .categoryId
//            if (selectedItems[selectedItems.keyAt(i)]) {
//                selectedCategories.add(categoryRemoteId)
//            } else {
//                selectedCategories.remove(categoryRemoteId)
//            }
//        }
    }

    private fun populateCategoryList() {
        val categoryLevels = viewModel.getCategoryLevels()

        for (i in categoryLevels.indices) {
            categoryRemoteIdsToListPositions.put(categoryLevels[i].categoryId, i)
        }

        // todo: annmarie check for other stuff - like has adapater etc
        (recycler_view.adapter as PrepublishingCategoriesAdapter).categoryNodeList = categoryLevels

        // todo: annmarie make a method in the adapter for setting checked itmems
        for (selectedCategory in selectedCategories) {
            if (categoryRemoteIdsToListPositions.get(selectedCategory) != null) {
                recycler_view.adapter.setItemChecked(
                        categoryRemoteIdsToListPositions.get(selectedCategory),
                        true
                )
            }
        }
//        // todo: annmarie - reset the scroll offset
//        // listScrollPositionManager.restoreScrollOffset()
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "This is possibly null because it's " +
                    "called during config changes."
        }

        return editPostActivityHook.editPostRepository
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null
        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        when (event.causeOfChange) {
            FETCH_CATEGORIES -> {
                swipeToRefreshHelper.isRefreshing = false
                if (event.isError) {
                    if (isAdded) {
                        ToastUtils.showToast(
                                requireContext(), string.category_refresh_error,
                                LONG
                        )
                    }
                } else {
                    populateCategoryList()
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        // todo: annmarie - check that this is the visible fragment using FM
        swipeToRefreshHelper.isRefreshing = false
        if (event.isError) {
            if (isAdded) {
                ToastUtils.showToast(
                        requireContext(),
                        string.adding_cat_failed,
                        LONG
                )
            }
        } else {
            selectedCategories.add(event.term.remoteTermId)
            populateCategoryList()
            if (isAdded) {
                ToastUtils.showToast(
                        requireContext(),
                        string.adding_cat_success,
                        SHORT
                )
            }
        }
    }

    override fun onCategorySelected(categoryId: Long) {
        Log.i(javaClass.simpleName, "***=> categorySelected $categoryId")
    }

    override fun onCategoryUnselected(categoryId: Long) {
        Log.i(javaClass.simpleName, "***=> categoryUnselected $categoryId")
    }
}
