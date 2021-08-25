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
import javax.inject.Inject

class DomainsDashboardFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainsDashboardViewModel
    private var binding: FragmentDomainsDashboardBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component()?.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_domains_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, viewModelFactory).get(DomainsDashboardViewModel::class.java)
        with(FragmentDomainsDashboardBinding.bind(view)) {
            binding = this
        }
    }

    companion object {
        const val TAG = "DOMAINS_DASHBOARD_FRAGMENT"
        @JvmStatic fun newInstance() = DomainsDashboardFragment()
    }
}
