package org.wordpress.android.ui.domains

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.domain_registration_result_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress

class DomainRegistrationResultFragment : Fragment() {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.domain_registration_result_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkNotNull((activity?.application as WordPress).component())

        continue_button.setOnClickListener {
            val intent = Intent()
            intent.putExtra(RESULT_REGISTERED_DOMAIN_EMAIL, email)
            val nonNullActivity = requireActivity()
            nonNullActivity.setResult(RESULT_OK, intent)
            nonNullActivity.finish()
        }

        domain_registration_result_message.text = HtmlCompat.fromHtml(
                getString(
                        R.string.domain_registration_result_description,
                        domainName
                ), FROM_HTML_MODE_COMPACT
        )
    }
}
