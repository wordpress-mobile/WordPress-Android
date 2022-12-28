package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.PrepublishingCategoriesFragmentBinding
import org.wordpress.android.databinding.PrepublishingToolbarBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.ADD_CATEGORY
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingCategoriesFragment : Fragment(R.layout.prepublishing_categories_fragment) {
    private var closeListener: PrepublishingScreenClosedListener? = null
    private var actionListener: PrepublishingActionClickedListener? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingCategoriesViewModel
    private lateinit var parentViewModel: PrepublishingViewModel
    @Inject
    lateinit var uiHelpers: UiHelpers
    @Inject
    lateinit var dispatcher: Dispatcher

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
        with(PrepublishingCategoriesFragmentBinding.bind(view)) {
            includePrepublishingToolbar.initBackButton()
            includePrepublishingToolbar.initAddCategoryButton()
            initRecyclerView()
            initViewModel()
        }
    }

    private fun PrepublishingToolbarBinding.initBackButton() {
        backButton.setOnClickListener {
            viewModel.onBackButtonClick()
        }
    }

    private fun PrepublishingToolbarBinding.initAddCategoryButton() {
        addActionButton.setOnClickListener {
            viewModel.onAddCategoryClick()
        }
    }

    private fun PrepublishingCategoriesFragmentBinding.initRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false
        )
        recyclerView.adapter = PrepublishingCategoriesAdapter(uiHelpers)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    private fun PrepublishingCategoriesFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(this@PrepublishingCategoriesFragment, viewModelFactory)
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

    private fun PrepublishingCategoriesFragmentBinding.startObserving() {
        viewModel.navigateToHomeScreen.observeEvent(viewLifecycleOwner, {
            closeListener?.onBackClicked()
        })

        viewModel.navigateToAddCategoryScreen.observe(viewLifecycleOwner, { bundle ->
            actionListener?.onActionClicked(ADD_CATEGORY, bundle)
        }
        )

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, { uiString ->
            includePrepublishingToolbar.toolbarTitle.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, {
            it.showToast()
        })

        viewModel.uiState.observe(viewLifecycleOwner, {
            (recyclerView.adapter as PrepublishingCategoriesAdapter).update(
                it.categoriesListItemUiState
            )
            with(uiHelpers) {
                updateVisibility(includePrepublishingToolbar.addActionButton, it.addCategoryActionButtonVisibility)
                updateVisibility(progressLoading, it.progressVisibility)
                updateVisibility(recyclerView, it.categoryListVisibility)
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
        @JvmStatic
        fun newInstance(
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

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        viewModel.onTermUploadedComplete(event)
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        viewModel.onTaxonomyChanged(event)
    }
}
