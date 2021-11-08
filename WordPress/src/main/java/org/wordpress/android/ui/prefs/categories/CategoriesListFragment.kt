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
import org.wordpress.android.ui.layoutpicker.CategoriesAdapter
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Error
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class CategoriesListFragment : Fragment(R.layout.site_settings_categories_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: CategoriesListViewModel
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var dispatcher: Dispatcher


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()

        with(SiteSettingsCategoriesListFragmentBinding.bind(view)) {
            initRecyclerView()
            initViewModel(getSite(savedInstanceState))
            setupObservers()
        }
    }

    private fun SiteSettingsCategoriesListFragmentBinding.initRecyclerView() {
        categoriesRecyclerView.setHasFixedSize(true)
        categoriesRecyclerView.layoutManager =  LinearLayoutManager(activity)
        categoriesRecyclerView.setEmptyView(actionableEmptyView)
        categoriesRecyclerView.visibility = View.VISIBLE
        categoriesRecyclerView.adapter = CategoriesAdapter()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this@CategoriesListFragment, viewModelFactory)
                .get(CategoriesListViewModel::class.java)

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
                is UiState.Content -> showList(it.list)
                is Error -> handleError(it)
                is UiState.Loading -> {}
            }
        })
    }

    private fun SiteSettingsCategoriesListFragmentBinding.handleError(error: Error) {
        uiHelpers.setTextOrHide(actionableEmptyView.title, error.title)
        uiHelpers.setTextOrHide(actionableEmptyView.subtitle, error.subtitle)
        actionableEmptyView.image.setImageResource(error.image)
        error.buttonText?.let { uiHelpers.setTextOrHide(actionableEmptyView.button, error.buttonText) }
        error.action?.let { action -> actionableEmptyView.button.setOnClickListener { action.invoke() } }
    }

    @Suppress("unused")
    private fun SiteSettingsCategoriesListFragmentBinding.showList(list: List<CategoryNode>) {
        // Todo implement the logic of showing list
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
