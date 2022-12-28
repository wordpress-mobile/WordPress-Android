package org.wordpress.android.ui.prefs.categories.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteSettingsCategoriesListFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.prefs.categories.list.CategoryDetailNavigation.CreateCategory
import org.wordpress.android.ui.prefs.categories.list.CategoryDetailNavigation.EditCategory
import org.wordpress.android.ui.prefs.categories.list.UiState.Content
import org.wordpress.android.ui.prefs.categories.list.UiState.Loading
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

@AndroidEntryPoint
class CategoriesListFragment : Fragment(R.layout.site_settings_categories_list_fragment) {
    private val viewModel: CategoriesListViewModel by viewModels()
    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var adapter: SiteSettingsCategoriesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(SiteSettingsCategoriesListFragmentBinding.bind(view)) {
            initRecyclerView()
            initFabButton()
            initEmptyView()
            initViewModel(getSite(savedInstanceState))
        }
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initViewModel(site: SiteModel) {
        setupObservers()
        viewModel.start(site)
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initRecyclerView() {
        categoriesRecyclerView.setHasFixedSize(true)
        adapter = SiteSettingsCategoriesAdapter(uiHelpers, ::onCategoryRowClicked)
        categoriesRecyclerView.adapter = adapter

        categoriesRecyclerView.addItemDecoration(
            DividerItemDecoration(
                categoriesRecyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initFabButton() {
        fabButton.setOnClickListener {
            viewModel.createCategory()
        }
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initEmptyView() {
        categoriesRecyclerView.setEmptyView(actionableEmptyView)
        actionableEmptyView.updateVisibility(false)
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    private fun SiteSettingsCategoriesListFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) {
            progressBar.updateVisibility(it.loadingVisible)
            categoriesRecyclerView.updateVisibility(it.contentVisible)
            fabButton.updateVisibility(it.contentVisible)
            actionableEmptyView.updateVisibility(it.errorVisible)
            when (it) {
                is Content -> updateContentLayout(it.list)
                is UiState.Error -> updateErrorContent(it)
                is Loading -> {
                }
            }
        }

        viewModel.navigation.observe(viewLifecycleOwner) {
            when (it) {
                is CreateCategory -> ActivityLauncher.showCategoryDetail(requireContext(), null)
                is EditCategory -> ActivityLauncher.showCategoryDetail(requireContext(), it.categoryId)
            }
        }
    }

    private fun SiteSettingsCategoriesListFragmentBinding.updateErrorContent(error: UiState.Error) {
        uiHelpers.setTextOrHide(actionableEmptyView.title, error.title)
        uiHelpers.setTextOrHide(actionableEmptyView.subtitle, error.subtitle)
        uiHelpers.setImageOrHide(actionableEmptyView.image, error.image)
        uiHelpers.setTextOrHide(actionableEmptyView.button, error.buttonText)
        error.action?.let { action ->
            actionableEmptyView.button.setOnClickListener {
                action.invoke()
            }
        }
    }

    private fun updateContentLayout(list: List<CategoryNode>) {
        adapter.submitList(list)
    }

    private fun onCategoryRowClicked(categoryNode: CategoryNode) {
        viewModel.onCategoryClicked(categoryNode)
    }

    fun View.updateVisibility(visible: Boolean) {
        uiHelpers.updateVisibility(this, visible)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.siteModel)
        super.onSaveInstanceState(outState)
    }
}
