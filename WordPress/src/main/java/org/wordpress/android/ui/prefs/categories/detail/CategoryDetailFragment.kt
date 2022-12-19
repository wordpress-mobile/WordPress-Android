@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.prefs.categories.detail

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
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

@AndroidEntryPoint
class CategoryDetailFragment : Fragment(R.layout.category_detail_fragment) {
    private val viewModel: CategoryDetailViewModel by viewModels()
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var categoryAdapter: ParentCategorySpinnerAdapter

    private var spinnerTouched: Boolean = false
    @Suppress("DEPRECATION") private var progressDialog: ProgressDialog? = null

    private var isInEditMode: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val categoryId = getCategoryId(savedInstanceState)

        with(CategoryDetailFragmentBinding.bind(view)) {
            if (isInEditMode) updateToolbarTitle()
            initAdapter()
            initSubmitButton()
            initSpinner()
            initViewModel(categoryId)
            if (!isInEditMode) initInputText()
        }
    }

    private fun updateToolbarTitle() {
        (requireActivity() as CategoryDetailActivity).title = getString(R.string.update_category)
    }

    private fun getCategoryId(savedInstanceState: Bundle?): Long? {
        val categoryId = savedInstanceState?.getLong(ActivityLauncher.CATEGORY_DETAIL_ID)
                ?: requireActivity().intent.getLongExtra(ActivityLauncher.CATEGORY_DETAIL_ID, 0L)
        if (categoryId == 0L)
            return null
        isInEditMode = true
        return categoryId
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
                is InProgress -> showProgressDialog(it.message)
                is Success -> showPostSuccess(it.message)
                is Failure -> showPostError(it.errorMessage)
            }
        }
    }

    private fun CategoryDetailFragmentBinding.updateSubmitButton(submitButtonUiState: SubmitButtonUiState) {
        with(submitButton) {
            isEnabled = submitButtonUiState.enabled
            uiHelpers.setTextOrHide(this, submitButtonUiState.buttonText)
        }
    }

    private fun loadCategories(categoryLevels: ArrayList<CategoryNode>) {
        categoryAdapter.replaceItems(categoryLevels)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.tag_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_trash).isVisible = isInEditMode
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_trash) {
            confirmDeleteCategory()
            return true
        }
        return super.onOptionsItemSelected(item)
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

    private fun confirmDeleteCategory() {
        val message = String.format(getString(R.string.dlg_confirm_delete_category),
                viewModel.existingCategory!!.name)
        val dialogBuilder: Builder = MaterialAlertDialogBuilder(requireActivity())
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton(
                resources.getText(R.string.delete_yes)
        ) { _: DialogInterface?, _: Int ->
            viewModel.deleteCategory()
        }
        dialogBuilder.setNegativeButton(R.string.cancel, null)
        dialogBuilder.setCancelable(true)
        dialogBuilder.create().show()
    }

    private fun showToast(uiString: UiString) {
        val message = uiHelpers.getTextOfUiString(requireContext(), uiString).toString()
        ToastUtils.showToast(
                requireContext(), message,
                SHORT
        )
    }
}
