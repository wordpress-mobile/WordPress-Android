package org.wordpress.android.ui.prefs.categories.detail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.CategoryDetailFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.ParentCategorySpinnerAdapter
import org.wordpress.android.ui.prefs.categories.detail.CategoryDetailViewModel.SubmitButtonUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class CategoryDetailFragment : Fragment(R.layout.category_detail_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: CategoryDetailViewModel
    @Inject lateinit var uiHelpers: UiHelpers

    var spinnerTouched: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()

        with(CategoryDetailFragmentBinding.bind(view)) {
            initSubmitButton()
            initAdapter()
            initSpinner()
            initInputText()
            initViewModel(getSite(savedInstanceState))
        }
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun CategoryDetailFragmentBinding.initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this@CategoryDetailFragment, viewModelFactory)
                .get(CategoryDetailViewModel::class.java)
        startObserving()
        viewModel.start(site)
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.siteModel)
        super.onSaveInstanceState(outState)
    }

    private fun CategoryDetailFragmentBinding.initSubmitButton() {
        submitButton.setOnClickListener {
            viewModel.onSubmitButtonClick()
        }
    }

    private fun CategoryDetailFragmentBinding.initInputText() {
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

    private fun CategoryDetailFragmentBinding.initAdapter() {
        val categoryAdapter = ParentCategorySpinnerAdapter(
                activity,
                layout.categories_row_parent,
                arrayListOf<CategoryNode>()
        )
        parentCategory.adapter = categoryAdapter
    }

    private fun CategoryDetailFragmentBinding.initSpinner() {
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

    private fun CategoryDetailFragmentBinding.startObserving() {
        viewModel.dismissKeyboard.observeEvent(viewLifecycleOwner) {
            ActivityUtils.hideKeyboardForced(categoryName)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            loadCategories(uiState.categories)
            if (uiState.selectedParentCategoryPosition != parentCategory.selectedItemPosition) {
                parentCategory.setSelection(uiState.selectedParentCategoryPosition)
            }
            updateSubmitButton(uiState.submitButtonUiState)
        }
    }

    private fun CategoryDetailFragmentBinding.updateSubmitButton(submitButtonUiState: SubmitButtonUiState) {
        with(submitButton) {
            isEnabled = submitButtonUiState.enabled
        }
        with(uiHelpers) {
            updateVisibility(submitButton, submitButtonUiState.visibility)
        }
    }

    private fun CategoryDetailFragmentBinding.loadCategories(categoryLevels: ArrayList<CategoryNode>) {
        (parentCategory.adapter as? ParentCategorySpinnerAdapter)?.replaceItems(categoryLevels)
    }

    private fun SnackbarMessageHolder.showToast() {
        val message = uiHelpers.getTextOfUiString(requireContext(), this.message).toString()
        ToastUtils.showToast(
                requireContext(), message,
                SHORT
        )
    }
}
