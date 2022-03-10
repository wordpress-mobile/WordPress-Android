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
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.CategoryDetailFragmentBinding
import org.wordpress.android.models.CategoryNode
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

    var spinnerTouched: Boolean = false
    private var mProgressDialog: ProgressDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()

        with(CategoryDetailFragmentBinding.bind(view)) {
            initSubmitButton()
            initAdapter()
            initSpinner()
            initInputText()
            initViewModel()
        }
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun CategoryDetailFragmentBinding.initViewModel() {
        viewModel = ViewModelProvider(this@CategoryDetailFragment, viewModelFactory)
                .get(CategoryDetailViewModel::class.java)
        startObserving()
        viewModel.start()
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
        }
        with(uiHelpers) {
            updateVisibility(submitButton, submitButtonUiState.visibility)
        }
    }

    private fun CategoryDetailFragmentBinding.loadCategories(categoryLevels: ArrayList<CategoryNode>) {
        (parentCategory.adapter as? ParentCategorySpinnerAdapter)?.replaceItems(categoryLevels)
    }

    private fun showProgressDialog(@StringRes messageId: Int) {
        mProgressDialog = ProgressDialog(requireContext())
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setIndeterminate(true)
        mProgressDialog!!.setMessage(getString(messageId))
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
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
