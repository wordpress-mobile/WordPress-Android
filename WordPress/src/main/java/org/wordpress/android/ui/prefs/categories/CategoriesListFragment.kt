package org.wordpress.android.ui.prefs.categories

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteSettingsCategoriesListFragmentBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Content
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Error
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Loading
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class CategoriesListFragment : Fragment(R.layout.site_settings_categories_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: CategoriesListViewModel
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var uiHelpers: UiHelpers

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()

        with(SiteSettingsCategoriesListFragmentBinding.bind(view)) {
            initRecyclerView()
            initEmptyView()
            initViewModel(getSite(savedInstanceState))
        }
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this@CategoriesListFragment, viewModelFactory)
                .get(CategoriesListViewModel::class.java)
        setupObservers()
        viewModel.start(site)
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    private fun SiteSettingsCategoriesListFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, {
            when (it) {
                is Content -> showList(it.list)
                is Error -> showError(it)
                is Loading -> showLoading()
            }
        })
    }

    private fun SiteSettingsCategoriesListFragmentBinding.showLoading() {
        progressBar.updateVisibility(true)

        fabButton.updateVisibility(false)
        categoriesRecyclerView.updateVisibility(false)
        actionableEmptyView.updateVisibility(false)
    }

    private fun SiteSettingsCategoriesListFragmentBinding.showError(error: Error) {
        updateErrorContent(error)
        actionableEmptyView.updateVisibility(true)

        fabButton.updateVisibility(false)
        progressBar.updateVisibility(false)
        categoriesRecyclerView.updateVisibility(false)
    }

    private fun SiteSettingsCategoriesListFragmentBinding.updateErrorContent(error:Error) {
        uiHelpers.setTextOrHide(actionableEmptyView.title, error.title)
        uiHelpers.setTextOrHide(actionableEmptyView.subtitle, error.subtitle)
        actionableEmptyView.image.setImageResource(error.image)
        error.buttonText?.let { uiHelpers.setTextOrHide(actionableEmptyView.button, error.buttonText) }
        error.action?.let { action ->
            actionableEmptyView.button.setOnClickListener {
                action.invoke()
            }
        }
    }

    @Suppress("unused")
    private fun SiteSettingsCategoriesListFragmentBinding.showList(list: List<CategoryNode>) {
        categoriesRecyclerView.updateVisibility(true)

        fabButton.updateVisibility(false)
        progressBar.updateVisibility(false)
        uiHelpers.updateVisibility(actionableEmptyView,false)
        // Todo: AjeshRPai implement the logic of showing list
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initRecyclerView() {
        categoriesRecyclerView.setHasFixedSize(true)
        categoriesRecyclerView.layoutManager = LinearLayoutManager(activity)
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initEmptyView() {
        categoriesRecyclerView.setEmptyView(actionableEmptyView)
        actionableEmptyView.updateVisibility(false)
    }

    fun View.updateVisibility(visible:Boolean) {
        uiHelpers.updateVisibility(this,visible)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.siteModel)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        dispatcher.unregister(this)
        super.onStop()
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        viewModel.onTaxonomyChanged(event)
    }
}
