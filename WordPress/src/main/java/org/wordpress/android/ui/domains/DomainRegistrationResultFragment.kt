package org.wordpress.android.ui.domains

import android.app.Activity.RESULT_OK
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.text.HtmlCompat
import android.support.v4.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.domain_registration_result_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.viewmodel.domains.DomainRegistrationResultViewModel

class DomainRegistrationResultFragment : Fragment() {
    private lateinit var viewModel: DomainRegistrationResultViewModel

    companion object {
        private const val EXTRA_REGISTERED_DOMAIN_NAME = "extra_registered_domain_name"
        private const val EXTRA_REGISTERED_DOMAIN_EMAIL = "extra_registered_domain_email"
        const val RESULT_REGISTERED_DOMAIN_EMAIL = "RESULT_REGISTERED_DOMAIN_EMAIL"

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
        viewModel = ViewModelProviders.of(this).get(DomainRegistrationResultViewModel::class.java)
        viewModel.domainName = arguments?.getString(EXTRA_REGISTERED_DOMAIN_NAME, "")
        viewModel.email = arguments?.getString(EXTRA_REGISTERED_DOMAIN_EMAIL, "")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.domain_registration_result_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkNotNull((activity?.application as WordPress).component())

        continue_button.setOnClickListener {
            val intent = Intent()
            intent.putExtra(RESULT_REGISTERED_DOMAIN_EMAIL, viewModel.email)

            activity!!.setResult(RESULT_OK, intent)
            activity!!.finish()
        }

        domain_registration_result_message.text = HtmlCompat.fromHtml(
                getString(
                        R.string.domain_registration_result_description,
                        viewModel.domainName
                ), FROM_HTML_MODE_COMPACT
        )
    }
}
