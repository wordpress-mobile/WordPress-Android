package org.wordpress.android.ui.jetpack.scan.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.jetpack.scan.ScanConstants
import javax.inject.Inject

class ThreatDetailsFragment : Fragment(R.layout.threat_details_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ThreatDetailsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDagger()
        initViewModel()
    }

    private fun initDagger() {
        (requireActivity().application as WordPress).component()?.inject(this)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, viewModelFactory).get(ThreatDetailsViewModel::class.java)
        val threatId = requireActivity().intent.getLongExtra(ScanConstants.ARG_THREAT_ID, 0)
        viewModel.start(threatId)
    }
}
