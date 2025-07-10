package org.wordpress.android.ui.accounts.login.applicationpassword

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
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
import org.wordpress.android.login.LoginBaseFormFragment
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment
import org.wordpress.android.login.LoginSiteAddressValidator
import org.wordpress.android.login.R
import org.wordpress.android.login.widgets.WPLoginInputRow
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class LoginSiteApplicationPasswordFragment : LoginBaseFormFragment<LoginListener>(), TextWatcher,
    OnEditorCommitListener {
    private var siteAddressInput: WPLoginInputRow? = null

    private var loginSiteAddressValidator = LoginSiteAddressValidator()

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginSiteApplicationPasswordViewModel

    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.login_site_address_screen

    @LayoutRes
    override fun getProgressBarText(): Int = R.string.login_checking_site_address

    override fun setupLabel(label: TextView) {
        label.setText(R.string.enter_site_address)
    }

    override fun setupContent(rootView: ViewGroup) {
        // Stub
    }

    override fun setupBottomButton(button: Button) {
        button.setOnClickListener { discover() }
    }

    override fun buildToolbar(toolbar: Toolbar, actionBar: ActionBar) {
        actionBar.setTitle(R.string.log_in)
    }

    override fun getEditTextToFocusOnStart(): EditText? = siteAddressInput?.editText

    override fun onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpSiteAddress(loginSiteAddressValidator.cleanedSiteAddress)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAnalyticsListener.trackUrlFormViewed()

        requireActivity().setTitle(R.string.site_address_login_title)
        this.siteAddressInput = view.findViewById(R.id.login_site_address_row)
        siteAddressInput?.addTextChangedListener(this)
        siteAddressInput?.setOnEditorCommitListener(this)

        view.findViewById<View>(R.id.login_site_address_help_button).setOnClickListener {
            mAnalyticsListener.trackShowHelpClick()
            showSiteAddressHelp()
        }

        loginSiteAddressValidator.isValid.observe(viewLifecycleOwner) { enabled ->
            bottomButton.isEnabled = enabled
        }
        loginSiteAddressValidator.errorMessageResId.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                showError(resId)
            } else {
                siteAddressInput?.setError(null)
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
            .flowWithLifecycle(viewLifecycleOwner.lifecycle,  Lifecycle.State.STARTED)
            .onEach { loading ->
                if (loading) {
                    startProgress()
                } else {
                    endProgressIfNeeded()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        mAnalyticsListener.siteAddressFormScreenResumed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loginSiteAddressValidator.dispose()
        siteAddressInput = null
    }

    override fun onEditorCommit() {
        if (bottomButton.isEnabled) {
            discover()
        }
    }

    override fun afterTextChanged(s: Editable) {
        siteAddressInput?.let { siteAddressInput ->
            loginSiteAddressValidator.setAddress(EditTextUtils.getText(siteAddressInput.editText))
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // Stub
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        siteAddressInput?.setError(null)
    }

    private fun showError(messageId: Int) {
        val message = getString(messageId)
        mAnalyticsListener.trackFailure(message)
        siteAddressInput?.setError(message)
    }

    private fun showSiteAddressHelp() {
        LoginSiteAddressHelpDialogFragment().show(
            parentFragmentManager,
            LoginSiteAddressHelpDialogFragment.TAG
        )
    }

    private fun discover() {
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }
        mAnalyticsListener.trackSubmitClicked()

        val cleanedUrl = loginSiteAddressValidator.cleanedSiteAddress
        mAnalyticsListener.trackConnectedSiteInfoRequested(cleanedUrl)
        viewModel.runApiDiscovery(cleanedUrl)
    }

    companion object {
        const val TAG: String = "login_site_application_password_fragment_tag"
    }
}

