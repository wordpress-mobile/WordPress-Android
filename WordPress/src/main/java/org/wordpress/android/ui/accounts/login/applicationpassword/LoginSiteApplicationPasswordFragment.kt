@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.accounts.login.applicationpassword

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.accounts.login.LoginAnalyticsListener
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.LoginActivity
import org.wordpress.android.ui.accounts.login.LoginSiteAddressValidator
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class LoginSiteApplicationPasswordFragment : Fragment(), TextWatcher {
    private var siteAddressInputLayout: TextInputLayout? = null
    private var siteAddressInput: TextInputEditText? = null
    private var bottomButton: Button? = null
    private var progressDialog: ProgressDialog? = null
    private var inProgress = false
    private var loginActivity: LoginActivity? = null

    private var loginSiteAddressValidator = LoginSiteAddressValidator()

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginSiteApplicationPasswordViewModel

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var analyticsListener: LoginAnalyticsListener

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.login_form_screen, container, false) as ViewGroup
        val formContainer = rootView.findViewById<ViewStub>(R.id.login_form_content_stub)
        formContainer.layoutResource = R.layout.login_site_address_screen
        formContainer.inflate()

        rootView.findViewById<TextView>(R.id.label).setText(R.string.enter_site_address)
        bottomButton = rootView.findViewById(R.id.bottom_button)
        bottomButton?.setOnClickListener { discover() }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)

        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(R.string.log_in)
        }

        analyticsListener.trackUrlFormViewed()

        requireActivity().setTitle(R.string.site_address_login_title)
        siteAddressInputLayout = view.findViewById(R.id.login_site_address_input_layout)
        siteAddressInput = view.findViewById(R.id.login_site_address_input)
        siteAddressInput?.addTextChangedListener(this)
        siteAddressInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && bottomButton?.isEnabled == true) {
                discover()
            }
            true
        }

        if (savedInstanceState == null) {
            @Suppress("DEPRECATION")
            try {
                EditTextUtils.showSoftInput(siteAddressInput)
            } catch (e: Exception) {
                AppLog.e(T.MAIN, "Error showing soft input", e)
            }
        }

        loginSiteAddressValidator.isValid.observe(viewLifecycleOwner) { enabled ->
            bottomButton?.isEnabled = enabled
        }
        loginSiteAddressValidator.errorMessageResId.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                showError(resId)
            } else {
                siteAddressInputLayout?.error = null
                siteAddressInputLayout?.isErrorEnabled = false
            }
        }

        viewModel = ViewModelProvider(this, viewModelFactory)[LoginSiteApplicationPasswordViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.discoveryURL.collect { url ->
                    if (url.isEmpty()) {
                        showError(R.string.application_password_not_supported_error)
                        return@collect
                    } else {
                        activityNavigator.openApplicationPasswordLogin(requireActivity(), url)
                    }
                }
            }
        }

        viewModel.loadingStateFlow
            .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .onEach { loading ->
                if (loading) {
                    startProgress()
                } else {
                    endProgressIfNeeded()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is LoginActivity) { "$context must be LoginActivity" }
        loginActivity = context
    }

    override fun onDetach() {
        super.onDetach()
        loginActivity = null
    }

    override fun onResume() {
        super.onResume()
        analyticsListener.siteAddressFormScreenResumed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loginSiteAddressValidator.dispose()
        siteAddressInputLayout = null
        siteAddressInput = null
        bottomButton = null
        progressDialog?.setOnCancelListener(null)
        progressDialog = null
    }

    override fun onDestroy() {
        endProgress()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_login, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            analyticsListener.trackShowHelpClick()
            loginActivity?.helpSiteAddress(loginSiteAddressValidator.cleanedSiteAddress)
            return true
        }
        return false
    }

    override fun afterTextChanged(s: Editable) {
        siteAddressInput?.let { input ->
            loginSiteAddressValidator.setAddress(EditTextUtils.getText(input))
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // Stub
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        siteAddressInputLayout?.error = null
        siteAddressInputLayout?.isErrorEnabled = false
    }

    private fun showError(messageId: Int) {
        val message = getString(messageId)
        analyticsListener.trackFailure(message)
        siteAddressInputLayout?.error = message
    }

    private fun discover() {
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }
        analyticsListener.trackSubmitClicked()

        val cleanedUrl = loginSiteAddressValidator.cleanedSiteAddress
        analyticsListener.trackConnectedSiteInfoRequested(cleanedUrl)
        viewModel.runApiDiscovery(cleanedUrl)
    }

    @Suppress("DEPRECATION")
    private fun startProgress() {
        bottomButton?.isEnabled = false
        progressDialog = ProgressDialog.show(
            activity,
            "",
            getString(R.string.login_checking_site_address),
            true,
            true
        ) { endProgressIfNeeded() }
        inProgress = true
    }

    private fun endProgressIfNeeded() {
        if (inProgress) {
            endProgress()
        }
    }

    private fun endProgress() {
        inProgress = false
        progressDialog?.cancel()
        progressDialog?.setOnCancelListener(null)
        progressDialog = null
        bottomButton?.isEnabled = true
    }

    companion object {
        const val TAG: String = "login_site_application_password_fragment_tag"
    }
}
