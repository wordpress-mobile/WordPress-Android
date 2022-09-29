package org.wordpress.android.ui.domains

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.android.support.AndroidSupportInjection
import org.apache.commons.lang3.StringEscapeUtils
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainRegistrationDetailsFragmentBinding
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
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainContactFormModel
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel.DomainRegistrationDetailsUiState
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPUrlUtils
import javax.inject.Inject

class DomainRegistrationDetailsFragment : Fragment() {
    companion object {
        private const val EXTRA_DOMAIN_PRODUCT_DETAILS = "EXTRA_DOMAIN_PRODUCT_DETAILS"
        const val TAG = "DOMAIN_REGISTRATION_DETAILS"

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
    private lateinit var mainViewModel: DomainRegistrationMainViewModel

    private var loadingProgressDialog: ProgressDialog? = null
    private var binding: DomainRegistrationDetailsFragmentBinding? = null

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
        return inflater.inflate(R.layout.domain_registration_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel = ViewModelProvider(requireActivity(), viewModelFactory)
                .get(DomainRegistrationMainViewModel::class.java)
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(DomainRegistrationDetailsViewModel::class.java)
        with(DomainRegistrationDetailsFragmentBinding.bind(view)) {
            binding = this
            setupObservers()

            val domainProductDetails = requireNotNull(
                    arguments?.getParcelable<DomainProductDetails?>(EXTRA_DOMAIN_PRODUCT_DETAILS)
            )
            val site = requireActivity().intent?.getSerializableExtra(WordPress.SITE) as SiteModel

            viewModel.start(site, domainProductDetails)

            domainPrivacyOnRadioButton.setOnClickListener {
                viewModel.togglePrivacyProtection(true)
            }

            domainPrivacyOffRadioButton.setOnClickListener {
                viewModel.togglePrivacyProtection(false)
            }

            // Country and State input could only be populated from the dialog
            countryInput.inputType = 0
            countryInput.setOnClickListener {
                viewModel.onCountrySelectorClicked()
            }

            stateInput.inputType = 0
            stateInput.setOnClickListener {
                viewModel.onStateSelectorClicked()
            }

            registerDomainButton.setOnClickListener {
                if (validateForm()) {
                    viewModel.onRegisterDomainButtonClicked()
                }
            }

            setupTosLink()
            setupInputFieldTextWatchers()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun DomainRegistrationDetailsFragmentBinding.setupInputFieldTextWatchers() {
        arrayOf(
                firstNameInput,
                lastNameInput,
                organizationInput,
                emailInput,
                countryCodeInput,
                phoneNumberInput,
                addressFirstLineInput,
                addressSecondLineInput,
                cityInput,
                postalCodeInput
        ).forEach {
            it.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                    viewModel.onDomainContactDetailsChanged(getDomainContactFormModel())
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            })
        }
    }

    // make link to ToS clickable
    private fun DomainRegistrationDetailsFragmentBinding.setupTosLink() {
        tosExplanation.text = HtmlCompat.fromHtml(
                String.format(
                        resources.getString(R.string.domain_registration_privacy_protection_tos),
                        "<u>",
                        "</u>"
                ),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        tosExplanation.movementMethod = LinkMovementMethod.getInstance()
        tosExplanation.setOnClickListener {
            viewModel.onTosLinkClicked()
        }
    }

    private fun DomainRegistrationDetailsFragmentBinding.setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner,
                {
                    it?.let { uiState -> loadState(uiState) }
                })

        viewModel.domainContactForm.observe(
                viewLifecycleOwner,
                { domainContactFormModel ->
                    val currentModel = getDomainContactFormModel()
                    if (currentModel != domainContactFormModel) {
                        populateContactForm(domainContactFormModel!!)
                    }
                })

        viewModel.showCountryPickerDialog.observe(viewLifecycleOwner,
                {
                    if (it != null && it.isNotEmpty()) {
                        showCountryPicker(it)
                    }
                })

        viewModel.showStatePickerDialog.observe(viewLifecycleOwner,
                {
                    if (it != null && it.isNotEmpty()) {
                        showStatePicker(it)
                    }
                })

        observeFormError(viewModel)

        viewModel.showErrorMessage.observe(viewLifecycleOwner,
                { errorMessage ->
                    ToastUtils.showToast(context, errorMessage)
                })

        viewModel.handleCompletedDomainRegistration.observe(viewLifecycleOwner,
                { domainRegisteredEvent ->
                    mainViewModel.completeDomainRegistration(domainRegisteredEvent)
                })

        viewModel.showTos.observe(viewLifecycleOwner,
                {
                    ActivityLauncher.openUrlExternal(
                            context,
                            WPUrlUtils.buildTermsOfServiceUrl(context)
                    )
                })
    }

    private fun DomainRegistrationDetailsFragmentBinding.loadState(uiState: DomainRegistrationDetailsUiState) {
        toggleFormProgressIndictor(uiState.isFormProgressIndicatorVisible)
        toggleStateProgressIndicator(uiState.isStateProgressIndicatorVisible)
        toggleStateInputEnabledState(uiState.isStateInputEnabled)

        if (uiState.isRegistrationProgressIndicatorVisible) {
            showDomainRegistrationProgressDialog()
        } else {
            hideDomainRegistrationProgressDialog()
        }

        if (uiState.isPrivacyProtectionEnabled) {
            domainPrivacyOptionsRadiogroup.check(R.id.domain_privacy_on_radio_button)
        } else {
            domainPrivacyOptionsRadiogroup.check(R.id.domain_privacy_off_radio_button)
        }

        registerDomainButton.isEnabled = uiState.isDomainRegistrationButtonEnabled

        // Country and State fields treated as UI state, since we only use them for display purpose
        countryInput.setText(uiState.selectedCountry?.name)
        stateInput.setText(uiState.selectedState?.name)
    }

    private fun DomainRegistrationDetailsFragmentBinding.observeFormError(
        viewModel: DomainRegistrationDetailsViewModel
    ) {
        viewModel.formError.observe(viewLifecycleOwner,
                { error ->
                    var affectedInputFields: Array<TextInputEditText>? = null

                    when (error?.type) {
                        FIRST_NAME -> affectedInputFields = arrayOf(firstNameInput)
                        LAST_NAME -> affectedInputFields = arrayOf(lastNameInput)
                        ORGANIZATION -> affectedInputFields = arrayOf(organizationInput)
                        ADDRESS_1 -> affectedInputFields = arrayOf(addressFirstLineInput)
                        ADDRESS_2 -> affectedInputFields = arrayOf(addressSecondLineInput)
                        POSTAL_CODE -> affectedInputFields = arrayOf(postalCodeInput)
                        CITY -> affectedInputFields = arrayOf(cityInput)
                        STATE -> affectedInputFields = arrayOf(stateInput)
                        COUNTRY_CODE -> affectedInputFields = arrayOf(countryInput)
                        EMAIL -> affectedInputFields = arrayOf(emailInput)
                        PHONE -> affectedInputFields = arrayOf(
                                countryCodeInput,
                                phoneNumberInput
                        )
                        else -> {
                        } // Something else, will just show a Toast with an error message
                    }
                    affectedInputFields?.forEach {
                        showFieldError(it, StringEscapeUtils.unescapeHtml4(error?.message))
                    }
                    affectedInputFields?.firstOrNull { it.requestFocus() }
                })
    }

    private fun DomainRegistrationDetailsFragmentBinding.populateContactForm(formModel: DomainContactFormModel) {
        firstNameInput.setText(formModel.firstName)
        lastNameInput.setText(formModel.lastName)
        organizationInput.setText(formModel.organization)
        emailInput.setText(formModel.email)
        countryCodeInput.setText(formModel.phoneNumberPrefix)
        phoneNumberInput.setText(formModel.phoneNumber)
        addressFirstLineInput.setText(formModel.addressLine1)
        addressSecondLineInput.setText(formModel.addressLine2)
        cityInput.setText(formModel.city)
        postalCodeInput.setText(formModel.postalCode)
    }

    // local validation
    private fun DomainRegistrationDetailsFragmentBinding.validateForm(): Boolean {
        var formIsCompleted = true

        val requiredFields = arrayOf(
                firstNameInput,
                lastNameInput,
                emailInput,
                countryCodeInput,
                phoneNumberInput,
                countryInput,
                addressFirstLineInput,
                cityInput,
                postalCodeInput
        )

        var fieldToFocusOn: TextInputEditText? = null

        requiredFields.forEach {
            clearEmptyFieldError(it)

            if (TextUtils.isEmpty(it.text)) {
                if (fieldToFocusOn == null) {
                    fieldToFocusOn = it
                }
                showEmptyFieldError(it)
                if (formIsCompleted) {
                    formIsCompleted = false
                }
            }
        }

        // focusing on first empty field
        fieldToFocusOn?.requestFocus()

        return formIsCompleted
    }

    private fun showEmptyFieldError(editText: EditText) {
        val parent = editText.parent.parent
        if (parent is TextInputLayout) {
            showFieldError(
                    editText,
                    getString(R.string.domain_registration_contact_form_input_error, parent.hint)
            )
        }
    }

    private fun clearEmptyFieldError(editText: EditText) {
        val parent = editText.parent.parent
        if (parent is TextInputLayout) {
            showFieldError(editText, null)
        }
    }

    private fun showFieldError(editText: EditText, errorMessage: String?) {
        editText.error = errorMessage
    }

    private fun getDomainContactFormModel(): DomainContactFormModel = with(binding!!) {
        return DomainContactFormModel(
                firstNameInput.text.toString(),
                lastNameInput.text.toString(),
                StringUtils.notNullStr(organizationInput.text.toString()),
                addressFirstLineInput.text.toString(),
                addressSecondLineInput.text.toString(),
                postalCodeInput.text.toString(),
                cityInput.text.toString(),
                null, // state code will be added in ViewModel
                null, // country code will be added in ViewModel
                emailInput.text.toString(),
                countryCodeInput.text.toString(),
                phoneNumberInput.text.toString()
        )
    }

    private fun showStatePicker(states: List<SupportedStateResponse>) {
        val dialogFragment = StatePickerDialogFragment.newInstance(states.toCollection(ArrayList()))
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(requireFragmentManager(), StatePickerDialogFragment.TAG)
    }

    private fun showCountryPicker(countries: List<SupportedDomainCountry>) {
        val dialogFragment = CountryPickerDialogFragment.newInstance(
                countries.toCollection(
                        ArrayList()
                )
        )
        dialogFragment.setTargetFragment(this, 0)
        dialogFragment.show(requireFragmentManager(), CountryPickerDialogFragment.TAG)
    }

    private fun DomainRegistrationDetailsFragmentBinding.toggleFormProgressIndictor(visible: Boolean) {
        if (visible) {
            formProgressIndicator.visibility = View.VISIBLE
        } else {
            formProgressIndicator.visibility = View.GONE
        }
    }

    private fun DomainRegistrationDetailsFragmentBinding.toggleStateProgressIndicator(visible: Boolean) {
        if (visible) {
            statesLoadingProgressIndicator.visibility = View.VISIBLE
        } else {
            statesLoadingProgressIndicator.visibility = View.GONE
        }

        stateInputContainer.isEnabled = !visible
    }

    private fun DomainRegistrationDetailsFragmentBinding.toggleStateInputEnabledState(enabled: Boolean) {
        stateInputContainer.isEnabled = enabled
        if (enabled) {
            stateInputContainer.hint = getString(R.string.domain_contact_information_state_hint)
        } else {
            stateInputContainer.hint = getString(R.string.domain_contact_information_state_not_available_hint)
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
            states = requireArguments().getParcelableArrayList<SupportedStateResponse>(EXTRA_STATES)
                    as ArrayList<SupportedStateResponse>
        }

        @Suppress("UseCheckOrError")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if (targetFragment == null) {
                throw IllegalStateException("StatePickerDialogFragment is missing a targetFragment ")
            }

            viewModel = ViewModelProvider(targetFragment!!, viewModelFactory)
                    .get(DomainRegistrationDetailsViewModel::class.java)
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle(R.string.domain_registration_state_picker_dialog_title)
            builder.setItems(states.map { it.name }.toTypedArray()) { _, which ->
                viewModel.onStateSelected(states[which])
            }

            builder.setPositiveButton(R.string.dialog_button_cancel) { dialog, _ ->
                dialog.dismiss()
            }

            return builder.create()
        }

        override fun onAttach(context: Context) {
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

        @Suppress("UseCheckOrError")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            if (targetFragment == null) {
                throw IllegalStateException("CountryPickerDialogFragment is missing a targetFragment ")
            }

            viewModel = ViewModelProvider(targetFragment!!, viewModelFactory)
                    .get(DomainRegistrationDetailsViewModel::class.java)
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle(R.string.domain_registration_country_picker_dialog_title)
            builder.setItems(countries.map { it.name }.toTypedArray()) { _, which ->
                viewModel.onCountrySelected(countries[which])
            }

            builder.setPositiveButton(R.string.dialog_button_cancel) { dialog, _ ->
                dialog.dismiss()
            }

            return builder.create()
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            AndroidSupportInjection.inject(this)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? ScrollableViewInitializedListener)?.onScrollableViewInitialized(
                R.id.domain_registration_details_container
        )
    }
}
