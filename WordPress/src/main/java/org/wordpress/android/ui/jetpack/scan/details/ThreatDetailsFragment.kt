package org.wordpress.android.ui.jetpack.scan.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.threat_details_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.scan.ScanFragment.Companion.ARG_THREAT_ID
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.adapters.ThreatDetailsAdapter
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ThreatDetailsFragment : Fragment(R.layout.threat_details_fragment) {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ThreatDetailsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initAdapter()
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initAdapter() {
        recycler_view.adapter = ThreatDetailsAdapter(imageManager, uiHelpers)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ThreatDetailsViewModel::class.java)
        setupObservers()
        val threatId = requireActivity().intent.getLongExtra(ARG_THREAT_ID, 0)
        viewModel.start(threatId)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(
            viewLifecycleOwner,
            { uiState ->
                if (uiState is Content) {
                    refreshContentScreen(uiState)
                }
            }
        )
    }

    private fun refreshContentScreen(content: Content) {
        ((recycler_view.adapter) as ThreatDetailsAdapter).update(content.items)
    }
}
