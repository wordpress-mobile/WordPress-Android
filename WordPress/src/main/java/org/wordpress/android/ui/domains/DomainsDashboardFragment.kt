package org.wordpress.android.ui.domains

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FragmentDomainsDashboardBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainsNavigationEvents.GetDomain
import org.wordpress.android.ui.domains.DomainsNavigationEvents.OpenManageDomains
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class DomainsDashboardFragment : Fragment(R.layout.fragment_domains_dashboard) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: DomainsDashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component()?.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentDomainsDashboardBinding.bind(view)) {
            setupViews()
            setupViewModel()

            viewModel.uiModel.observe(viewLifecycleOwner) { uiState ->
                (contentRecyclerView.adapter as? DomainsDashboardAdapter)?.submitList(uiState ?: listOf())
            }

            viewModel.onNavigation.observeEvent(viewLifecycleOwner, { handleNavigationAction(it) })
        }
    }

    private fun FragmentDomainsDashboardBinding.setupViews() {
        contentRecyclerView.adapter = DomainsDashboardAdapter(uiHelpers)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(DomainsDashboardViewModel::class.java)
        viewModel.start()
    }

    private fun handleNavigationAction(action: DomainsNavigationEvents) = when (action) {
        is GetDomain -> ActivityLauncher.viewDomainRegistrationActivityForResult(
                activity,
                action.site,
                CTA_DOMAIN_CREDIT_REDEMPTION
        )
        is OpenManageDomains -> ActivityLauncher.openUrlExternal(
                requireContext(),
                action.url
        )
    }

    companion object {
        const val TAG = "DOMAINS_DASHBOARD_FRAGMENT"
        @JvmStatic fun newInstance() = DomainsDashboardFragment()
    }
}
