package org.wordpress.android.ui.domains

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.core.text.parseAsHtml
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainRegistrationResultFragmentBinding
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.setLightNavigationBar
import org.wordpress.android.util.extensions.setLightStatusBar
import javax.inject.Inject

class DomainRegistrationResultFragment : Fragment(R.layout.domain_registration_result_fragment) {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var mainViewModel: DomainRegistrationMainViewModel

    companion object {
        private const val EXTRA_REGISTERED_DOMAIN_NAME = "extra_registered_domain_name"
        private const val EXTRA_REGISTERED_DOMAIN_EMAIL = "extra_registered_domain_email"
        const val TAG = "DOMAIN_REGISTRATION_RESULT_FRAGMENT"

        fun newInstance(domainName: String, email: String?) = DomainRegistrationResultFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_REGISTERED_DOMAIN_NAME, domainName)
                putString(EXTRA_REGISTERED_DOMAIN_EMAIL, email)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)

        mainViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
            .get(DomainRegistrationMainViewModel::class.java)

        val domainName = requireArguments().getString(EXTRA_REGISTERED_DOMAIN_NAME).orEmpty()
        val email = requireArguments().getString(EXTRA_REGISTERED_DOMAIN_EMAIL).orEmpty()

        setupWindow()
        setupToolbar()

        with(DomainRegistrationResultFragmentBinding.bind(view)) {
            setupViews(domainName, email)
            setupObservers(domainName, email)
        }
    }

    private fun setupWindow() = with(requireAppCompatActivity()) {
        val colorPrimarySurface = getColorFromAttribute(R.attr.colorPrimarySurface)
        window.apply {
            statusBarColor = colorPrimarySurface
            navigationBarColor = colorPrimarySurface
            setLightStatusBar(false)
            setLightNavigationBar(false)
        }
    }

    private fun setupToolbar() = with(requireAppCompatActivity()) {
        supportActionBar?.hide()
    }

    private fun DomainRegistrationResultFragmentBinding.setupViews(domainName: String, email: String) {
        continueButton.setOnClickListener {
            finishRegistration(domainName, email)
        }

        domainRegistrationResultMessage.text = getString(
            R.string.domain_registration_result_description,
            domainName
        ).parseAsHtml(FROM_HTML_MODE_COMPACT)
    }

    private fun setupObservers(domainName: String, email: String) = with(requireActivity()) {
        onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishRegistration(domainName, email)
            }
        })
    }

    private fun finishRegistration(domainName: String, email: String) {
        mainViewModel.finishDomainRegistration(DomainRegistrationCompletedEvent(domainName, email))
    }

    private fun requireAppCompatActivity() = requireActivity() as AppCompatActivity
}
