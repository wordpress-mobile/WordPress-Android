package org.wordpress.android.ui.domains

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.core.text.parseAsHtml
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.databinding.DomainRegistrationResultFragmentBinding
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.setLightNavigationBar
import org.wordpress.android.util.setLightStatusBar

class DomainRegistrationResultFragment : Fragment(R.layout.domain_registration_result_fragment) {
    companion object {
        private const val EXTRA_REGISTERED_DOMAIN_NAME = "extra_registered_domain_name"
        private const val EXTRA_REGISTERED_DOMAIN_EMAIL = "extra_registered_domain_email"
        const val RESULT_REGISTERED_DOMAIN_EMAIL = "RESULT_REGISTERED_DOMAIN_EMAIL"
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
        val domainName = requireArguments().getString(EXTRA_REGISTERED_DOMAIN_NAME).orEmpty()
        val email = requireArguments().getString(EXTRA_REGISTERED_DOMAIN_EMAIL).orEmpty()

        setupWindow()
        setupToolbar()

        with(DomainRegistrationResultFragmentBinding.bind(view)) {
            setupViews(domainName, email)
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
            finishRegistration(email)
        }

        domainRegistrationResultMessage.text = getString(
                R.string.domain_registration_result_description,
                domainName
        ).parseAsHtml(FROM_HTML_MODE_COMPACT)
    }

    private fun finishRegistration(email: String) = with(requireActivity()) {
        setResult(RESULT_OK, Intent().putExtra(RESULT_REGISTERED_DOMAIN_EMAIL, email))
        finish()
    }

    private fun requireAppCompatActivity() = requireActivity() as AppCompatActivity
}
