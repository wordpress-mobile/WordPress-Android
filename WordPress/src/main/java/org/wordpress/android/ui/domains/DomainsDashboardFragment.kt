package org.wordpress.android.ui.domains

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainsDashboardFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.RequestCodes.DOMAIN_REGISTRATION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.DOMAIN_PURCHASE
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.ClaimDomain
import org.wordpress.android.ui.domains.DomainsDashboardNavigationAction.GetDomain
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class DomainsDashboardFragment : Fragment(R.layout.domains_dashboard_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: DomainsDashboardViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        with(DomainsDashboardFragmentBinding.bind(view)) {
            setupViews()
            setupViewModel()
            setupObservers()
        }
    }

    private fun DomainsDashboardFragmentBinding.setupViews() {
        contentRecyclerView.adapter = DomainsDashboardAdapter(uiHelpers)
    }

    private fun setupViewModel() {
        val intent = requireActivity().intent
        val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(DomainsDashboardViewModel::class.java)
        viewModel.start(site)
    }

    private fun DomainsDashboardFragmentBinding.setupObservers() {
        viewModel.progressBar.observe(viewLifecycleOwner) {
            progress.isVisible = it
        }
        viewModel.uiModel.observe(viewLifecycleOwner) { uiState ->
            (contentRecyclerView.adapter as? DomainsDashboardAdapter)?.submitList(uiState ?: listOf())
        }
        viewModel.onNavigation.observeEvent(viewLifecycleOwner, ::handleNavigationAction)
    }

    private fun handleNavigationAction(action: DomainsDashboardNavigationAction) = when (action) {
        is GetDomain -> ActivityLauncher.viewDomainRegistrationActivityForResult(
            this,
            action.site,
            DOMAIN_PURCHASE
        )
        is ClaimDomain -> ActivityLauncher.viewDomainRegistrationActivityForResult(
            this,
            action.site,
            CTA_DOMAIN_CREDIT_REDEMPTION
        )
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == DOMAIN_REGISTRATION) {
            viewModel.onSuccessfulDomainRegistration()
        }
    }

    companion object {
        const val TAG = "DOMAINS_DASHBOARD_FRAGMENT"

        @JvmStatic
        fun newInstance() = DomainsDashboardFragment()
    }
}
