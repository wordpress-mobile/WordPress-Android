package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.add_category.*
import kotlinx.android.synthetic.main.prepublishing_toolbar.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.UiState.ContentUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import javax.inject.Inject

class PrepublishingAddCategoryFragment : Fragment(R.layout.prepublishing_add_category_fragment) {
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingAddCategoryViewModel
    @Inject lateinit var uiHelpers: UiHelpers

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        closeListener = parentFragment as PrepublishingScreenClosedListener
    }

    override fun onDetach() {
        super.onDetach()
        closeListener = null
    }

    override fun onResume() {
        val needsRequestLayout = requireArguments().getBoolean(NEEDS_REQUEST_LAYOUT)
        if (needsRequestLayout) {
            requireActivity().window.decorView.requestLayout()
        }
        super.onResume()
    }

    companion object {
        const val TAG = "prepublishing_add_category_fragment_tag"
        const val NEEDS_REQUEST_LAYOUT = "prepublishing_add_category_fragment_needs_request_layout"
        @JvmStatic fun newInstance(
            site: SiteModel,
            needsRequestLayout: Boolean
        ): PrepublishingAddCategoryFragment {
            val bundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putBoolean(NEEDS_REQUEST_LAYOUT, needsRequestLayout)
            }
            return PrepublishingAddCategoryFragment().apply { arguments = bundle }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initBackButton()
        initCloseButton()
        initInputText()
        initViewModel()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initBackButton() {
        back_button.setOnClickListener {
            viewModel.addCategory(
                    category_name.text.toString(),
                    (parent_category.selectedItem as CategoryNode)
            )
            viewModel.onBackButtonClicked()
        }
    }

    private fun initCloseButton() {
        close_button.setOnClickListener {
            viewModel.onBackButtonClicked()
        }
    }

    private fun initInputText() {
        category_name.requestFocus()
        ActivityUtils.showKeyboard(category_name)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingAddCategoryViewModel::class.java)
        startObserving()
    }

    private fun startObserving() {
        viewModel.dismissKeyboard.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                ActivityUtils.hideKeyboardForced(category_name)
            }
        })

        viewModel.navigateBack.observe(viewLifecycleOwner, Observer { event ->
            event?.applyIfNotHandled {
                closeListener?.onBackClicked()
            }
        })

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, Observer { uiString ->
            toolbar_title.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.snackbarEvents.observe(viewLifecycleOwner, Observer {
            it?.applyIfNotHandled {
                showToast()
            }
        })

        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
                when (uiState) {
                    is ContentUiState -> {
                        loadCategories(uiState.categories)
                    }
                }
            with(uiHelpers) {
                updateVisibility(close_button, uiState.closeButtonVisible)
            }
        })

        val needsRequestLayout = requireArguments().getBoolean(PrepublishingTagsFragment.NEEDS_REQUEST_LAYOUT)
        val siteModel = requireArguments().getSerializable(WordPress.SITE) as SiteModel
        viewModel.start(siteModel, !needsRequestLayout)
    }

    private fun loadCategories(categoryLevels: ArrayList<CategoryNode>) {
        categoryLevels.add(
                0, CategoryNode(
                0, 0,
                getString(R.string.top_level_category_name)
        ))
        val categoryAdapter = ParentCategorySpinnerAdapter(
                activity,
                R.layout.categories_row_parent,
                categoryLevels
        )
        parent_category.adapter = categoryAdapter
    }

    private fun SnackbarMessageHolder.showToast() {
        val message = uiHelpers.getTextOfUiString(requireContext(), this.message).toString()
        ToastUtils.showToast(
                requireContext(), message,
                LONG
        )
    }
}
