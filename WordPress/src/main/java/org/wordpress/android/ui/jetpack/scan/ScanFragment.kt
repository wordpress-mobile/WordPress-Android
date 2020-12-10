package org.wordpress.android.ui.jetpack.scan

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.scan_fragment.recycler_view
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.adapters.ScanAdapter
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class ScanFragment : Fragment(R.layout.scan_fragment) {
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ScanViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component()?.inject(this)

        val site = if (savedInstanceState == null) {
            val nonNullIntent = checkNotNull(nonNullActivity.intent)
            nonNullIntent.getSerializableExtra(WordPress.SITE) as SiteModel
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        }

        initAdapter()
        initViewModel(site)
    }

    private fun initAdapter() {
        recycler_view.adapter = ScanAdapter(uiHelpers)
        recycler_view.adapter?.setHasStableIds(true)
    }

    private fun initViewModel(site: SiteModel) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ScanViewModel::class.java)
        setupObservers()
        viewModel.start(site)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(
            viewLifecycleOwner,
            Observer { uiState ->
                if (uiState is Content) {
                    refreshContentScreen(uiState)
                }
            }
        )
    }

    private fun refreshContentScreen(content: Content) {
        ((recycler_view.adapter) as ScanAdapter).update(content.items)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, viewModel.site)
        super.onSaveInstanceState(outState)
    }
}
