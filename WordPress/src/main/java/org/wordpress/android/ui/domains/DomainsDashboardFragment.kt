package org.wordpress.android.ui.domains

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FragmentDomainsDashboardBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION
import org.wordpress.android.ui.mysite.MySiteAdapter
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.SiteNavigationAction.OpenDomainRegistration
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class DomainsDashboardFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: DomainsDashboardViewModel
    private var binding: FragmentDomainsDashboardBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component()?.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_domains_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(FragmentDomainsDashboardBinding.bind(view)) {
            binding = this
            val adapter = MySiteAdapter(imageManager, uiHelpers)
            contentRecyclerView.adapter = adapter
            viewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                    .get(DomainsDashboardViewModel::class.java)

            viewModel.start()

            viewModel.uiModel.observe(viewLifecycleOwner) { uiState ->
                (contentRecyclerView.adapter as? MySiteAdapter)?.loadData(uiState ?: listOf())
            }

            viewModel.onNavigation.observeEvent(viewLifecycleOwner, { handleNavigationAction(it) })
        }
    }

    private fun handleNavigationAction(action: SiteNavigationAction) = when (action) {
        is OpenDomainRegistration -> ActivityLauncher.viewDomainRegistrationActivityForResult(
                activity,
                action.site,
                CTA_DOMAIN_CREDIT_REDEMPTION
        )
        else -> {} // TODO: next
    }

    companion object {
        const val TAG = "DOMAINS_DASHBOARD_FRAGMENT"
        @JvmStatic fun newInstance() = DomainsDashboardFragment()
    }
}
