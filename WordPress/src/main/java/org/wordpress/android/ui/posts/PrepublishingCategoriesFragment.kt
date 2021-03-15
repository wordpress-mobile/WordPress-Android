package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.ADD_CATEGORY
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingCategoriesFragment : Fragment(R.layout.prepublishing_categories_fragment) {
    private var closeListener: PrepublishingScreenClosedListener? = null
    private var actionListener: PrepublishingActionClickedListener? = null

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingCategoriesViewModel
    private lateinit var parentViewModel: PrepublishingViewModel
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var dispatcher: Dispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
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

    override fun onResume() {
        // Note: This supports the re-calculation and visibility of views when coming from stories.
        val needsRequestLayout = requireArguments().getBoolean(NEEDS_REQUEST_LAYOUT)
        if (needsRequestLayout) {
            requireActivity().window.decorView.requestLayout()
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBackButton()
        initAddCategoryButton()
        initRecyclerView()
        initViewModel()
    }

    private fun initBackButton() {
        back_button.setOnClickListener {
            viewModel.onBackButtonClick()
        }
    }

    private fun initAddCategoryButton() {
        add_action_button.setOnClickListener {
            viewModel.onAddCategoryClick()
        }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
        )
        recycler_view.adapter = PrepublishingCategoriesAdapter(uiHelpers)
        recycler_view.addItemDecoration(
                DividerItemDecoration(
                        recycler_view.context,
                        DividerItemDecoration.VERTICAL
                )
        )
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(PrepublishingCategoriesViewModel::class.java)
        parentViewModel = ViewModelProvider(requireParentFragment(), viewModelFactory)
                .get(PrepublishingViewModel::class.java)
        startObserving()
        val siteModel = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        val addCategoryRequest: PrepublishingAddCategoryRequest? =
                arguments?.getSerializable(ADD_CATEGORY_REQUEST) as? PrepublishingAddCategoryRequest
        val selectedCategoryIds: List<Long> =
                arguments?.getLongArray(SELECTED_CATEGORY_IDS)?.toList() ?: listOf()

        viewModel.start(getEditPostRepository(), siteModel, addCategoryRequest, selectedCategoryIds)
    }

    private fun startObserving() {
        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner, {
                closeListener?.onBackClicked()
        })

        viewModel.navigateToAddCategoryScreen.observe(viewLifecycleOwner, { bundle ->
            actionListener?.onActionClicked(ADD_CATEGORY, bundle)
        }
        )

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, { uiString ->
            toolbar_title.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, {
                it.showToast()
        })

        viewModel.uiState.observe(viewLifecycleOwner, {
            (recycler_view.adapter as PrepublishingCategoriesAdapter).update(
                    it.categoriesListItemUiState
            )
            with(uiHelpers) {
                updateVisibility(add_action_button, it.addCategoryActionButtonVisibility)
                updateVisibility(progress_loading, it.progressVisibility)
                updateVisibility(recycler_view, it.categoryListVisibility)
            }
        })
        parentViewModel.triggerOnDeviceBackPressed.observeEvent(viewLifecycleOwner, {
            viewModel.onBackButtonClick()
        })
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
                SHORT
        )
    }

    companion object {
        const val TAG = "prepublishing_categories_fragment_tag"
        const val NEEDS_REQUEST_LAYOUT = "prepublishing_categories_fragment_needs_request_layout"
        const val ADD_CATEGORY_REQUEST = "prepublishing_add_category_request"
        const val SELECTED_CATEGORY_IDS = "prepublishing_selected_category_ids"
        @JvmStatic fun newInstance(
            site: SiteModel,
            needsRequestLayout: Boolean,
            bundle: Bundle? = null
        ): PrepublishingCategoriesFragment {
            val newBundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putBoolean(NEEDS_REQUEST_LAYOUT, needsRequestLayout)
            }
            bundle?.let {
                newBundle.putAll(bundle)
            }
            return PrepublishingCategoriesFragment().apply { arguments = newBundle }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        viewModel.onTermUploadedComplete(event)
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.isError) {
            AppLog.e(T.POSTS, "An error occurred while updating taxonomy with type: " + event.error.type)
            return
        }
        if (event.causeOfChange == TaxonomyAction.FETCH_CATEGORIES) {
            viewModel.updateCategoriesListItemUiState()
        }
    }
}
