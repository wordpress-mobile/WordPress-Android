package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
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
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction.FETCH_CATEGORIES
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel.UiState.ContentUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.ADD_CATEGORY
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.util.WPSwipeToRefreshHelper
import org.wordpress.android.util.helpers.SwipeToRefreshHelper
import javax.inject.Inject

class PrepublishingCategoriesFragment : Fragment(R.layout.prepublishing_categories_fragment) {
    private var closeListener: PrepublishingScreenClosedListener? = null
    private var actionListener: PrepublishingActionClickedListener? = null

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingCategoriesViewModel
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var dispatcher: Dispatcher

    private lateinit var swipeToRefreshHelper: SwipeToRefreshHelper

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
        // Note: This supports the re-calculation and visibility of views when coming from stories.
        val needsRequestLayout = requireArguments().getBoolean(NEEDS_REQUEST_LAYOUT)
        if (needsRequestLayout) {
            requireActivity().window.decorView.requestLayout()
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initBackButton()
        initAddCategoryButton()
        initRecyclerView()
        initSwipeToRefreshHelper()
        initViewModel()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initBackButton() {
        back_button.setOnClickListener {
            viewModel.updateCategories()
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
                    if (checked) {
                        viewModel.addSelectedCategory(id)
                    } else {
                        viewModel.removeSelectedCategory(id)
                    }
                }, context = requireContext()
        )
        recycler_view.addItemDecoration(
                DividerItemDecoration(
                        recycler_view.context,
                        DividerItemDecoration.VERTICAL
                )
        )
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingCategoriesViewModel::class.java)
        startObserving()
    }

    private fun initSwipeToRefreshHelper() {
        swipeToRefreshHelper = WPSwipeToRefreshHelper.buildSwipeToRefreshHelper(ptr_layout,
                SwipeToRefreshHelper.RefreshListener {
                    if (!NetworkUtils.checkConnection(requireContext())) {
                        swipeToRefreshHelper.isRefreshing = false
                        return@RefreshListener
                    }
                    viewModel.refreshSiteCategories()
                })
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

        viewModel.snackbarEvents.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                actionListener?.onActionClicked(ADD_CATEGORY)
            }
        })

        viewModel.refreshingUiState.observe(viewLifecycleOwner, Observer { event ->
            swipeToRefreshHelper.isRefreshing = event.swipeToRefreshVisibility
            swipeToRefreshHelper.setEnabled(event.swipeToRefreshEnabled)
        })

        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is ContentUiState -> {
                    (recycler_view.adapter as PrepublishingCategoriesAdapter).set(
                            it.siteCategories,
                            it.selectedCategoryIds
                    )
                }
            }
        })

        val siteModel = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        viewModel.start(getEditPostRepository(), siteModel)
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

    private fun SnackbarMessageHolder.showToast() {
        val message = uiHelpers.getTextOfUiString(requireContext(), this.message).toString()
        ToastUtils.showToast(
                requireContext(), message,
                LONG
        )
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        when (event.causeOfChange) {
            FETCH_CATEGORIES -> {
                viewModel.onFetchSiteCategoriesComplete(event.isError)
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        viewModel.onNewSiteCategoryAddComplete(event)
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
}
