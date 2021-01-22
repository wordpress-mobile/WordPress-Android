package org.wordpress.android.ui.jetpack.scan.history

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.android.synthetic.main.scan_history_list_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.adapters.ScanAdapter
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel.ScanHistoryTabType
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ScanHistoryListFragment : ViewPagerFragment(R.layout.scan_history_list_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers

    private lateinit var viewModel: ScanHistoryListViewModel
    private lateinit var parentViewModel: ScanHistoryViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initRecyclerView()
        initViewModel(getSite(savedInstanceState), getTabType())
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initRecyclerView() {
        initAdapter()
    }

    private fun initAdapter() {
        recycler_view.adapter = ScanAdapter(imageManager, uiHelpers)
        recycler_view.itemAnimator = null
    }

    private fun initViewModel(site: SiteModel, tabType: ScanHistoryTabType) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ScanHistoryListViewModel::class.java)
        parentViewModel = ViewModelProvider(parentFragment as ViewModelStoreOwner, viewModelFactory).get(
                ScanHistoryViewModel::class.java
        )
        viewModel.start(tabType, site, parentViewModel)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner, { listItems -> refreshContentScreen(listItems) })
    }

    private fun refreshContentScreen(items: List<ScanListItemState>) {
        ((recycler_view.adapter) as ScanAdapter).update(items)
    }

    private fun getSite(savedInstanceState: Bundle?): SiteModel {
        return if (savedInstanceState == null) {
            requireActivity().intent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }
    }

    private fun getTabType(): ScanHistoryTabType = requireNotNull(arguments?.getParcelable(ARG_TAB_TYPE))

    override fun getScrollableViewForUniqueIdProvision(): View? = recycler_view

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val ARG_TAB_TYPE = "arg_tab_type"

        fun newInstance(tabType: ScanHistoryTabType): ScanHistoryListFragment {
            val newBundle = Bundle().apply {
                putParcelable(ARG_TAB_TYPE, tabType)
            }
            return ScanHistoryListFragment().apply { arguments = newBundle }
        }
    }
}
