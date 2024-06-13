package org.wordpress.android.ui.posts

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.AddCategoryBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class AddCategoryFragment : AppCompatDialogFragment() {
    private var mSite: SiteModel? = null
    private var binding: AddCategoryBinding? = null

    @set:Inject
    var mTaxonomyStore: TaxonomyStore? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        (requireActivity().application as WordPress).component().inject(this)
        initSite(savedInstanceState)
        val builder =
            MaterialAlertDialogBuilder(ContextThemeWrapper(activity, R.style.PostSettingsTheme))
        binding = AddCategoryBinding.inflate(layoutInflater, null, false)
        loadCategories()
        builder.setView(binding!!.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = requireDialog() as AlertDialog
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (addCategory()) {
                dismiss()
            }
        }
    }

    @Suppress("Deprecation")
    private fun initSite(savedInstanceState: Bundle?) {
        mSite = if (savedInstanceState == null) {
            if (arguments != null) {
                requireArguments().getSerializable(WordPress.SITE) as SiteModel?
            } else {
                requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel?
            }
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
        }

        if (mSite == null) {
            ToastUtils.showToast(
                requireActivity(),
                R.string.blog_not_found,
                ToastUtils.Duration.SHORT
            )
            parentFragmentManager.popBackStack()
        }
    }

    private fun addCategory(): Boolean {
        val categoryName = binding!!.categoryName.text.toString()
        val selectedCategory = binding!!.parentCategory.selectedItem as CategoryNode
        val parentId = selectedCategory.categoryId

        if (categoryName.replace(" ".toRegex(), "") == "") {
            binding!!.categoryName.error = getText(R.string.cat_name_required)
            return false
        }

        val newCategory = TermModel(
            TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
            categoryName,
            parentId
        )
        (requireActivity() as SelectCategoriesActivity).categoryAdded(newCategory)
        return true
    }

    private fun loadCategories() {
        val rootCategory = CategoryNode.createCategoryTreeFromList(
            mTaxonomyStore!!.getCategoriesForSite(
                mSite!!
            )
        )
        val categoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(rootCategory)
        categoryLevels.add(0, CategoryNode(0, 0, getString(R.string.top_level_category_name)))
        if (categoryLevels.size > 0) {
            val categoryAdapter =
                ParentCategorySpinnerAdapter(
                    activity,
                    R.layout.categories_row_parent,
                    categoryLevels
                )
            binding!!.parentCategory.adapter = categoryAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(WordPress.SITE, mSite)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        fun newInstance(site: SiteModel?): AddCategoryFragment {
            val fragment = AddCategoryFragment()
            val bundle = Bundle()
            bundle.putSerializable(WordPress.SITE, site)
            fragment.arguments = bundle
            return fragment
        }
    }
}
