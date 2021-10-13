package org.wordpress.android.ui.domains

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.databinding.DomainRegistrationResultFragmentBinding
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.setLightNavigationBar
import org.wordpress.android.util.setLightStatusBar

class DomainRegistrationResultFragment : Fragment(R.layout.domain_registration_result_fragment) {
    private var domainName: String? = null
    private var email: String? = null

    companion object {
        private const val EXTRA_REGISTERED_DOMAIN_NAME = "extra_registered_domain_name"
        private const val EXTRA_REGISTERED_DOMAIN_EMAIL = "extra_registered_domain_email"
        const val RESULT_REGISTERED_DOMAIN_EMAIL = "RESULT_REGISTERED_DOMAIN_EMAIL"
        const val TAG = "DOMAIN_REGISTRATION_RESULT_FRAGMENT"

        fun newInstance(domainName: String, email: String?): DomainRegistrationResultFragment {
            val fragment = DomainRegistrationResultFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_REGISTERED_DOMAIN_NAME, domainName)
            bundle.putString(EXTRA_REGISTERED_DOMAIN_EMAIL, email)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        domainName = arguments?.getString(EXTRA_REGISTERED_DOMAIN_NAME, "")
        email = arguments?.getString(EXTRA_REGISTERED_DOMAIN_EMAIL, "")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindow()
        setupToolbar()
        with(DomainRegistrationResultFragmentBinding.bind(view)) {
            continueButton.setOnClickListener {
                val intent = Intent()
                intent.putExtra(RESULT_REGISTERED_DOMAIN_EMAIL, email)
                val nonNullActivity = requireActivity()
                nonNullActivity.setResult(RESULT_OK, intent)
                nonNullActivity.finish()
            }

            domainRegistrationResultMessage.text = HtmlCompat.fromHtml(
                    getString(
                            R.string.domain_registration_result_description,
                            domainName
                    ), FROM_HTML_MODE_COMPACT
            )
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

    private fun requireAppCompatActivity() = requireActivity() as AppCompatActivity
}
