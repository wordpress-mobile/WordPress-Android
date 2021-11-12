package org.wordpress.android.ui.prefs.categories

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteSettingsCategoriesListFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class CategoriesListFragment : Fragment(R.layout.site_settings_categories_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: CategoriesListViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()

        with(SiteSettingsCategoriesListFragmentBinding.bind(view)) {
            initViewModel(getSite(savedInstanceState))
            setupObservers()
        }
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
            // todo handle the ui states
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.siteModel)
        super.onSaveInstanceState(outState)
    }
}
