package org.wordpress.android.ui.posts

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.AddCategoryBinding
import org.wordpress.android.databinding.PrepublishingAddCategoryFragmentBinding
import org.wordpress.android.databinding.PrepublishingToolbarBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.SubmitButtonUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class PrepublishingAddCategoryFragment : Fragment(R.layout.prepublishing_add_category_fragment) {
    private var closeListener: PrepublishingScreenClosedListener? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingAddCategoryViewModel
    private lateinit var parentViewModel: PrepublishingViewModel

    @Inject
    lateinit var uiHelpers: UiHelpers

    var spinnerTouched: Boolean = false

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
        // Note: This supports the re-calculation and visibility of views when coming from stories.
        val needsRequestLayout = requireArguments().getBoolean(NEEDS_REQUEST_LAYOUT)
        if (needsRequestLayout) {
            requireActivity().window.decorView.requestLayout()
        }
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(PrepublishingAddCategoryFragmentBinding.bind(view)) {
            prepublishingToolbar.initBackButton()
            addCategory.initSubmitButton()
            addCategory.initAdapter()
            addCategory.initSpinner()
            addCategory.initInputText()
            initViewModel()
        }
    }

    private fun PrepublishingToolbarBinding.initBackButton() {
        backButton.setOnClickListener {
            viewModel.onBackButtonClick()
        }
    }

    private fun AddCategoryBinding.initSubmitButton() {
        submitButton.setOnClickListener {
            viewModel.onSubmitButtonClick()
        }
    }

    private fun AddCategoryBinding.initInputText() {
        categoryName.requestFocus()
        categoryName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.categoryNameUpdated(s.toString())
            }
        })

        ActivityUtils.showKeyboard(categoryName)
    }

    private fun AddCategoryBinding.initAdapter() {
        val categoryAdapter = ParentCategorySpinnerAdapter(
            activity,
            R.layout.categories_row_parent,
            arrayListOf<CategoryNode>()
        )
        parentCategory.adapter = categoryAdapter
    }

    private fun AddCategoryBinding.initSpinner() {
        parentCategory.setOnTouchListener { v, _ ->
            spinnerTouched = true
            v.performClick()
            false
        }

        parentCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (spinnerTouched) {
                    viewModel.parentCategorySelected(position)
                    spinnerTouched = false
                }
            }
        }
    }

    private fun PrepublishingAddCategoryFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(
            this@PrepublishingAddCategoryFragment,
            viewModelFactory
        )[PrepublishingAddCategoryViewModel::class.java]
        parentViewModel =
            ViewModelProvider(requireParentFragment(), viewModelFactory)[PrepublishingViewModel::class.java]

        startObserving()

        val needsRequestLayout = requireArguments().getBoolean(PrepublishingTagsFragment.NEEDS_REQUEST_LAYOUT)
        requireArguments().getSerializableCompat<SiteModel>(WordPress.SITE)?.let {
            viewModel.start(it, !needsRequestLayout)
        }
    }

    private fun PrepublishingAddCategoryFragmentBinding.startObserving() {
        viewModel.dismissKeyboard.observeEvent(viewLifecycleOwner, {
            ActivityUtils.hideKeyboardForced(addCategory.categoryName)
        })

        viewModel.navigateBack.observe(viewLifecycleOwner, { bundle ->
            val newBundle = Bundle()
            newBundle.putAll(arguments)
            bundle?.let {
                newBundle.putAll(bundle)
            }
            closeListener?.onBackClicked(newBundle)
        })

        viewModel.toolbarTitleUiState.observe(viewLifecycleOwner, { uiString ->
            prepublishingToolbar.toolbarTitle.text = uiHelpers.getTextOfUiString(requireContext(), uiString)
        })

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner, {
            it.showToast()
        })

        viewModel.uiState.observe(viewLifecycleOwner, { uiState ->
            addCategory.loadCategories(uiState.categories)
            if (uiState.selectedParentCategoryPosition != addCategory.parentCategory.selectedItemPosition) {
                addCategory.parentCategory.setSelection(uiState.selectedParentCategoryPosition)
            }
            addCategory.updateSubmitButton(uiState.submitButtonUiState)
        })

        parentViewModel.triggerOnDeviceBackPressed.observeEvent(viewLifecycleOwner, {
            closeListener?.onBackClicked(arguments)
        })
    }

    private fun AddCategoryBinding.updateSubmitButton(submitButtonUiState: SubmitButtonUiState) {
        with(submitButton) {
            isEnabled = submitButtonUiState.enabled
        }
        with(uiHelpers) {
            updateVisibility(submitButton, submitButtonUiState.visibility)
        }
    }

    private fun AddCategoryBinding.loadCategories(categoryLevels: ArrayList<CategoryNode>) {
        (parentCategory.adapter as? ParentCategorySpinnerAdapter)?.replaceItems(categoryLevels)
    }

    private fun SnackbarMessageHolder.showToast() {
        val message = uiHelpers.getTextOfUiString(requireContext(), this.message).toString()
        ToastUtils.showToast(
            requireContext(), message,
            SHORT
        )
    }

    companion object {
        const val TAG = "prepublishing_add_category_fragment_tag"
        const val NEEDS_REQUEST_LAYOUT = "prepublishing_add_category_fragment_needs_request_layout"

        @JvmStatic
        fun newInstance(
            site: SiteModel,
            needsRequestLayout: Boolean,
            bundle: Bundle? = null
        ): PrepublishingAddCategoryFragment {
            val newBundle = Bundle().apply {
                putSerializable(WordPress.SITE, site)
                putBoolean(NEEDS_REQUEST_LAYOUT, needsRequestLayout)
            }
            bundle?.let {
                newBundle.putAll(bundle)
            }
            return PrepublishingAddCategoryFragment().apply { arguments = newBundle }
        }
    }
}
