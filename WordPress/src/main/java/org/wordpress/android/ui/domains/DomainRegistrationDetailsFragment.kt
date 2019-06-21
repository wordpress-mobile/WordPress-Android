package org.wordpress.android.ui.domains

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.android.support.AndroidSupportInjection
import org.apache.commons.lang3.StringEscapeUtils
import kotlinx.android.synthetic.main.domain_registration_details_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.ADDRESS_1
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.ADDRESS_2
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.CITY
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.COUNTRY_CODE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.EMAIL
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.FIRST_NAME
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.LAST_NAME
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.ORGANIZATION
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.PHONE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.POSTAL_CODE
import org.wordpress.android.fluxc.store.TransactionsStore.TransactionErrorType.STATE
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.viewmodel.domains.DomainRegistrationDetailsViewModel
import javax.inject.Inject

class DomainRegistrationDetailsFragment : Fragment() {
    companion object {
        private const val PHONE_NUMBER_PREFIX = "+"
        private const val PHONE_NUMBER_CONNECTING_CHARACTER = "."

        private const val EXTRA_DOMAIN_PRODUCT_DETAILS = "EXTRA_DOMAIN_PRODUCT_DETAILS"
        const val TAG = "DOMAIN_SUGGESTION_FRAGMENT_TAG"

        fun newInstance(domainProductDetails: DomainProductDetails): DomainRegistrationDetailsFragment {
            val fragment = DomainRegistrationDetailsFragment()
            val bundle = Bundle()
            bundle.putParcelable(EXTRA_DOMAIN_PRODUCT_DETAILS, domainProductDetails)
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainRegistrationDetailsViewModel

    private var loadingProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as WordPress).component()?.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.domain_registration_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                .get(DomainRegistrationDetailsViewModel::class.java)
        setupObservers()

        val domainProductDetails = arguments?.getParcelable(EXTRA_DOMAIN_PRODUCT_DETAILS) as DomainProductDetails
        val site = activity!!.intent?.getSerializableExtra(WordPress.SITE) as SiteModel

        viewModel.start(site, domainProductDetails)

        domain_privacy_on_radio_button.setOnClickListener {
            viewModel.togglePrivacyProtection(true)
        }

        domain_privacy_off_radio_button.setOnClickListener {
            viewModel.togglePrivacyProtection(false)
        }

        // Country and State input could only be populated from the dialog
        country_input.inputType = 0
        country_input.setOnClickListener {
            viewModel.onCountrySelectorClicked()
        }

        state_input.inputType = 0
        state_input.setOnClickListener {
            viewModel.onStateSelectorClicked()
        }

        register_domain_button.setOnClickListener {
            if (validateForm()) {
                viewModel.onRegisterDomainButtonClicked()
            }
        }

        setupTosLink()
        setupInputFieldTextWatchers()
    }

    private fun setupInputFieldTextWatchers() {
        arrayOf(
                first_name_input,
                last_name_input,
                organization_input,
                email_input,
                country_code_input,
                phone_number_input,
                address_first_line_input,
                address_second_line_input,
                city_input,
                postal_code_input
        ).forEach {
            it.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    viewModel.onDomainContactDetailsChanged(contactFormToDomainContactModel())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }
    }

    // make link to ToS clickable
    private fun setupTosLink() {
        tos_explanation.text = Html.fromHtml(
                String.format(
                        resources.getString(R.string.domain_registration_privacy_protection_tos), "<u>", "</u>"
                )
        )
        tos_explanation.movementMethod = LinkMovementMethod.getInstance()
        tos_explanation.setOnClickListener {
            viewModel.onTosLinkClicked()
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this,
                Observer { uiState ->
                    uiState?.let {
                        toggleFormProgressIndictor(uiState.isFormProgressIndicatorVisible)
                        toggleStateProgressIndicator(uiState.isStateProgressIndicatorVisible)
                        toggleStateInputEnabledState(uiState.isStateInputEnabled)

                        if (uiState.isRegistrationProgressIndicatorVisible) {
                            showDomainRegistrationProgressDialog()
                        } else {
                            hideDomainRegistrationProgressDialog()
                        }

                        if (uiState.isPrivacyProtectionEnabled) {
                            domain_privacy_options_radiogroup.check(R.id.domain_privacy_on_radio_button)
                        } else {
                            domain_privacy_options_radiogroup.check(R.id.domain_privacy_off_radio_button)
                        }

                        register_domain_button.isEnabled = uiState.isDomainRegistrationButtonEnabled

                        // Country and State fields treated as UI state, since we only use them for display purpose
                        country_input.setText(uiState.selectedCountry?.name)
                        state_input.setText(uiState.selectedState?.name)
                    }
                })

        viewModel.domainContactDetails.observe(this, Observer<DomainContactModel> { domainContactModel ->
            val currentModel = contactFormToDomainContactModel()
            if (currentModel != domainContactModel) {
                populateContactForm(domainContactModel!!)
            }
        })

        viewModel.showCountryPickerDialog.observe(this,
                Observer {
                    if (it != null && it.isNotEmpty()) {
                        showCountryPicker(it)
                    }
                })

        viewModel.showStatePickerDialog.observe(this,
                Observer {
                    if (it != null && it.isNotEmpty()) {
                        showStatePicker(it)
                    }
                })

        viewModel.formError.observe(this,
                Observer { error ->
                    var affectedInputFields: Array<TextInputEditText>? = null

                    when (error?.type) {
                        FIRST_NAME -> affectedInputFields = arrayOf(first_name_input)
                        LAST_NAME -> affectedInputFields = arrayOf(last_name_input)
                        ORGANIZATION -> affectedInputFields = arrayOf(organization_input)
                        ADDRESS_1 -> affectedInputFields = arrayOf(address_first_line_input)
                        ADDRESS_2 -> affectedInputFields = arrayOf(address_second_line_input)
                        POSTAL_CODE -> affectedInputFields = arrayOf(postal_code_input)
                        CITY -> affectedInputFields = arrayOf(city_input)
                        STATE -> affectedInputFields = arrayOf(state_input)
                        COUNTRY_CODE -> affectedInputFields = arrayOf(country_input)
                        EMAIL -> affectedInputFields = arrayOf(email_input)
                        PHONE -> affectedInputFields = arrayOf(country_code_input, phone_number_input)
                        else -> {
                        } // Something else, will just show a Toast with an error message
                    }
                    affectedInputFields?.forEach {
                        showFieldError(
                                it,
                                StringEscapeUtils.unescapeHtml4(error?.message)
                        )
                    }
                    affectedInputFields?.firstOrNull { it.requestFocus() }
                })

        viewModel.showErrorMessage.observe(this,
                Observer { errorMessage ->
                    ToastUtils.showToast(context, errorMessage)
                })

        viewModel.handleCompletedDomainRegistration.observe(this,
                Observer { domainName ->
                    (activity as DomainRegistrationActivity).onDomainRegistered(StringUtils.notNullStr(domainName))
                })

        viewModel.showTos.observe(this,
                Observer {
                    ActivityLauncher.openUrlExternal(context, WPUrlUtils.buildTermsOfServiceUrl(context))
                })
    }

    private fun populateContactForm(domainContactInformation: DomainContactModel) {
        first_name_input.setText(domainContactInformation.firstName)
        last_name_input.setText(domainContactInformation.lastName)
        organization_input.setText(domainContactInformation.organization)
        email_input.setText(domainContactInformation.email)

        if (!TextUtils.isEmpty(domainContactInformation.phone)) {
            val phoneParts = domainContactInformation.phone!!.split(PHONE_NUMBER_CONNECTING_CHARACTER)
            if (phoneParts.size == 2) {
                var countryCode = phoneParts[0]
                if (countryCode.startsWith(PHONE_NUMBER_PREFIX)) {
                    countryCode = countryCode.drop(1)
                }

                val phoneNumber = phoneParts[1]

                country_code_input.setText(countryCode)
                phone_number_input.setText(phoneNumber)
            }
        }

        address_first_line_input.setText(domainContactInformation.addressLine1)
        address_second_line_input.setText(domainContactInformation.addressLine2)
        city_input.setText(domainContactInformation.city)
        postal_code_input.setText(domainContactInformation.postalCode)
    }

    // local validation
    private fun validateForm(): Boolean {
        var formIsCompleted = true

        val requiredFields = arrayOf(
                first_name_input, last_name_input, email_input, country_code_input, phone_number_input,
                country_input, address_first_line_input, city_input, postal_code_input
        )

        requiredFields.forEach {
            if (TextUtils.isEmpty(it.text)) {
                showEmptyFieldError(it)
                if (formIsCompleted) {
                    formIsCompleted = false
                }
            }
        }

        return formIsCompleted
    }

    private fun showEmptyFieldError(editText: EditText) {
        val parent = editText.parent.parent
        if (parent is TextInputLayout) {
            showFieldError(editText, getString(R.string.domain_registration_contact_form_input_error, parent.hint))
        }
    }

    private fun showFieldError(editText: EditText, errorMessage: String) {
        editText.error = errorMessage
    }

    private fun contactFormToDomainContactModel(): DomainContactModel {
        val combinedPhoneNumber = getString(
                R.string.domain_registration_phone_number_format,
                country_code_input.text,
                phone_number_input.text
        )

        return DomainContactModel(
                first_name_input.text.toString(),
                last_name_input.text.toString(),
                StringUtils.notNullStr(organization_input.text.toString()),
                address_first_line_input.text.toString(),
                address_second_line_input.text.toString(),
                postal_code_input.text.toString(),
                city_input.text.toString(),
                null, // state code will be added in ViewModel
                null, // country code will be added in ViewModel
                email_input.text.toString(),
                combinedPhoneNumber,
                null
        )
    }

    private fun showStatePicker(states: List<SupportedStateResponse>) {
        val dialogFragment = StatePickerDialogFragment.newInstance(states.toCollection(ArrayList()))
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(fragmentManager, StatePickerDialogFragment.TAG)
    }

    private fun showCountryPicker(countries: List<SupportedDomainCountry>) {
        val dialogFragment = CountryPickerDialogFragment.newInstance(countries.toCollection(ArrayList()))
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(fragmentManager, CountryPickerDialogFragment.TAG)
    }

    private fun toggleFormProgressIndictor(visible: Boolean) {
        if (visible) {
            form_progress_indicator.visibility = View.VISIBLE
        } else {
            form_progress_indicator.visibility = View.GONE
        }
    }

    private fun toggleStateProgressIndicator(visible: Boolean) {
        if (visible) {
            states_loading_progress_indicator.visibility = View.VISIBLE
        } else {
            states_loading_progress_indicator.visibility = View.GONE
        }

        state_input_container.isEnabled = !visible
    }

    private fun toggleStateInputEnabledState(enabled: Boolean) {
        state_input_container.isEnabled = enabled
        if (enabled) {
            state_input_container.hint = getString(R.string.domain_contact_information_state_hint)
        } else {
            state_input_container.hint = getString(R.string.domain_contact_information_state_not_available_hint)
        }
    }

    private fun showDomainRegistrationProgressDialog() {
        if (loadingProgressDialog == null) {
            loadingProgressDialog = ProgressDialog(context)
            loadingProgressDialog!!.isIndeterminate = true
            loadingProgressDialog!!.setCancelable(false)

            loadingProgressDialog!!
                    .setMessage(getString(R.string.domain_registration_registering_domain_name_progress_dialog_message))
        }
        if (!loadingProgressDialog!!.isShowing) {
            loadingProgressDialog!!.show()
        }
    }

    private fun hideDomainRegistrationProgressDialog() {
        if (loadingProgressDialog != null && loadingProgressDialog!!.isShowing) {
            loadingProgressDialog!!.cancel()
        }
    }

    class StatePickerDialogFragment : DialogFragment() {
        private lateinit var states: ArrayList<SupportedStateResponse>
        @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
        private lateinit var viewModel: DomainRegistrationDetailsViewModel

        companion object {
            private const val EXTRA_STATES = "EXTRA_STATES"
            const val TAG = "STATE_PICKER_DIALOG_FRAGMENT"

            fun newInstance(states: ArrayList<SupportedStateResponse>): StatePickerDialogFragment {
                val fragment = StatePickerDialogFragment()
                val bundle = Bundle()
                bundle.putParcelableArrayList(EXTRA_STATES, states)
                fragment.arguments = bundle
                return fragment
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            states = arguments!!.getParcelableArrayList<SupportedStateResponse>(EXTRA_STATES)
                    as ArrayList<SupportedStateResponse>
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                    .get(DomainRegistrationDetailsViewModel::class.java)
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.domain_registration_state_picker_dialog_title)
            builder.setItems(states.map { it.name }.toTypedArray()) { _, which ->
                viewModel.onStateSelected(states[which])
            }

            builder.setPositiveButton(R.string.dialog_button_cancel) { dialog, _ ->
                dialog.dismiss()
            }

            return builder.create()
        }

        override fun onAttach(context: Context?) {
            super.onAttach(context)
            AndroidSupportInjection.inject(this)
        }
    }

    class CountryPickerDialogFragment : DialogFragment() {
        private lateinit var countries: ArrayList<SupportedDomainCountry>
        @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
        private lateinit var viewModel: DomainRegistrationDetailsViewModel

        companion object {
            private const val EXTRA_COUNTRIES = "EXTRA_COUNTRIES"
            const val TAG = "COUNTRY_PICKER_DIALOG_FRAGMENT"

            fun newInstance(countries: ArrayList<SupportedDomainCountry>): CountryPickerDialogFragment {
                val fragment = CountryPickerDialogFragment()
                val bundle = Bundle()
                bundle.putParcelableArrayList(EXTRA_COUNTRIES, countries)
                fragment.arguments = bundle
                return fragment
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            countries = arguments?.getParcelableArrayList<SupportedDomainCountry>(EXTRA_COUNTRIES)
                    as ArrayList<SupportedDomainCountry>
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            viewModel = ViewModelProviders.of(activity!!, viewModelFactory)
                    .get(DomainRegistrationDetailsViewModel::class.java)
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.domain_registration_country_picker_dialog_title)
            builder.setItems(countries.map { it.name }.toTypedArray()) { _, which ->
                viewModel.onCountrySelected(countries[which])
            }

            builder.setPositiveButton(R.string.dialog_button_cancel) { dialog, _ ->
                dialog.dismiss()
            }

            return builder.create()
        }

        override fun onAttach(context: Context?) {
            super.onAttach(context)
            AndroidSupportInjection.inject(this)
        }
    }
}
