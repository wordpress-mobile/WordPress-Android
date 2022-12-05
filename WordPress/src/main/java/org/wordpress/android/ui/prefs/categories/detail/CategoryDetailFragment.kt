@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.prefs.categories.detail

import android.app.ProgressDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.CategoryDetailFragmentBinding
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.posts.ParentCategorySpinnerAdapter
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Failure
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.InProgress
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Success
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class CategoryDetailFragment : Fragment(R.layout.category_detail_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: CategoryDetailViewModel
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var categoryAdapter: ParentCategorySpinnerAdapter

    private var spinnerTouched: Boolean = false
    @Suppress("DEPRECATION") private var progressDialog: ProgressDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()

        val categoryId = getCategoryId(savedInstanceState)

        with(CategoryDetailFragmentBinding.bind(view)) {
            initAdapter()
            initSubmitButton()
            initSpinner()
            initViewModel(categoryId)
            if(categoryId==null)initInputText()
        }
    }

    private fun getCategoryId(savedInstanceState: Bundle?): Long? {
        val categoryId = savedInstanceState?.getLong(ActivityLauncher.CATEGORY_DETAIL_ID)
                ?: requireActivity().intent.getLongExtra(ActivityLauncher.CATEGORY_DETAIL_ID, 0L)
        if (categoryId == 0L)
            return null
        return categoryId
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component().inject(this)
    }

    private fun CategoryDetailFragmentBinding.initAdapter() {
        categoryAdapter = ParentCategorySpinnerAdapter(
                activity,
                R.layout.categories_row_parent,
                arrayListOf<CategoryNode>()
        )
        parentCategory.adapter = categoryAdapter
    }

    private fun CategoryDetailFragmentBinding.initSubmitButton() {
        submitButton.setOnClickListener {
            viewModel.onSubmitButtonClick()
            ActivityUtils.hideKeyboardForced(categoryName)
        }
    }

    private fun CategoryDetailFragmentBinding.initSpinner() {
        parentCategory.setOnTouchListener { v, _ ->
            spinnerTouched = true
            v.performClick()
            false
        }

        parentCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @Suppress("EmptyFunctionBlock")
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (spinnerTouched) {
                    viewModel.onParentCategorySelected(position)
                    spinnerTouched = false
                }
            }
        }
    }

    @Suppress("EmptyFunctionBlock")
    private fun CategoryDetailFragmentBinding.initInputText() {
        categoryName.requestFocus()
        categoryName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                viewModel.onCategoryNameUpdated(s.toString())
            }
        })
        ActivityUtils.showKeyboard(categoryName)
    }

    private fun CategoryDetailFragmentBinding.initViewModel(categoryId: Long?) {
        viewModel = ViewModelProvider(this@CategoryDetailFragment, viewModelFactory)
                .get(CategoryDetailViewModel::class.java)
        startObserving()
        viewModel.start(categoryId)
    }

    private fun CategoryDetailFragmentBinding.startObserving() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            loadCategories(uiState.categories)
            if (uiState.selectedParentCategoryPosition != parentCategory.selectedItemPosition) {
                parentCategory.setSelection(uiState.selectedParentCategoryPosition)
                categoryName.setText(uiState.categoryName)
                initInputText()
            }
            updateSubmitButton(uiState.submitButtonUiState)
        }

        viewModel.onCategoryPush.observeEvent(viewLifecycleOwner) {
            when (it) {
                InProgress -> showProgressDialog(R.string.adding_cat)
                is Success -> showPostSuccess(it.message)
                is Failure -> showPostError(it.errorMessage)
            }
        }
    }

    private fun CategoryDetailFragmentBinding.updateSubmitButton(submitButtonUiState: SubmitButtonUiState) {
        with(submitButton) {
            isEnabled = submitButtonUiState.enabled
            uiHelpers.setTextOrHide(this, submitButtonUiState.buttonText)
            uiHelpers.updateVisibility(this, submitButtonUiState.visibility)
        }
    }

    private fun loadCategories(categoryLevels: ArrayList<CategoryNode>) {
        categoryAdapter.replaceItems(categoryLevels)
    }

    @Suppress("DEPRECATION")
    private fun showProgressDialog(@StringRes messageId: Int) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.apply {
            setCancelable(false)
            isIndeterminate = true
            setMessage(getString(messageId))
        }
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }

    private fun showPostError(message: UiString) {
        hideProgressDialog()
        showToast(message)
    }

    private fun showPostSuccess(message: UiString) {
        hideProgressDialog()
        showToast(message)
        requireActivity().finish()
    }

    private fun showToast(uiString: UiString) {
        val message = uiHelpers.getTextOfUiString(requireContext(), uiString).toString()
        ToastUtils.showToast(
                requireContext(), message,
                SHORT
        )
    }
}
