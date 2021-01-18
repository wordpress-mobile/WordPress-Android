package org.wordpress.android.ui.jetpack.scan.history

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.scan_history_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ViewPagerFragment
import javax.inject.Inject

class ScanHistoryListFragment : ViewPagerFragment(R.layout.scan_history_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ScanHistoryListViewModel
    private lateinit var parentViewModel: ScanHistoryViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initRecyclerView()
        initViewModel(getSite(savedInstanceState))
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initRecyclerView() {
        initAdapter()
    }

    private fun initAdapter() {
    }

    private fun initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ScanHistoryListViewModel::class.java)
        parentViewModel = ViewModelProvider(this, viewModelFactory).get(ScanHistoryViewModel::class.java)
        setupObservers()
        viewModel.start(site, parentViewModel)
    }

    private fun setupObservers() {
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    override fun getScrollableViewForUniqueIdProvision(): View? = nested_scroll_view

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }
}
