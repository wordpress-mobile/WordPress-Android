package org.wordpress.android.login

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.fluxc.network.discovery.DiscoveryUtils
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.login.widgets.WPLoginInputRow
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class LoginSiteApplicationPasswordFragment : LoginBaseFormFragment<LoginListener>(), TextWatcher, OnEditorCommitListener {
    private var siteAddressInput: WPLoginInputRow? = null

    private var requestedSiteAddress: String? = null

    private var loginSiteAddressValidator: LoginSiteAddressValidator? = null

    @JvmField
    @Inject
    var accountStore: AccountStore? = null

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.login_site_address_screen
    }

    @LayoutRes
    override fun getProgressBarText(): Int {
        return R.string.login_checking_site_address
    }

    override fun setupLabel(label: TextView) {
        if (mLoginListener.loginMode == LoginMode.SHARE_INTENT) {
            label.setText(R.string.enter_site_address_share_intent)
        } else {
            label.setText(R.string.enter_site_address)
        }
    }

    override fun setupContent(rootView: ViewGroup) {
        requireActivity().setTitle(R.string.site_address_login_title)
        val siteAddressInput: WPLoginInputRow = rootView.findViewById(R.id.login_site_address_row)
        this.siteAddressInput = siteAddressInput
        siteAddressInput.addTextChangedListener(this)
        siteAddressInput.setOnEditorCommitListener(this)

        rootView.findViewById<View>(R.id.login_site_address_help_button).setOnClickListener {
            mAnalyticsListener.trackShowHelpClick()
            showSiteAddressHelp()
        }
    }

    override fun setupBottomButton(button: Button) {
        button.setOnClickListener { discover() }
    }

    override fun buildToolbar(toolbar: Toolbar, actionBar: ActionBar) {
        actionBar.setTitle(R.string.log_in)
    }

    override fun getEditTextToFocusOnStart(): EditText? {
        return siteAddressInput?.editText
    }

    override fun onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpSiteAddress(requestedSiteAddress.orEmpty())
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            requestedSiteAddress = savedInstanceState.getString(KEY_REQUESTED_SITE_ADDRESS)
        } else {
            mAnalyticsListener.trackUrlFormViewed()
        }

        loginSiteAddressValidator = LoginSiteAddressValidator()

        loginSiteAddressValidator?.isValid?.observe(viewLifecycleOwner) { enabled ->
            bottomButton.isEnabled = enabled
        }
        loginSiteAddressValidator?.errorMessageResId?.observe(viewLifecycleOwner) { resId ->
            if (resId != null) {
                showError(resId)
            } else {
                siteAddressInput?.setError(null)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mAnalyticsListener.siteAddressFormScreenResumed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_REQUESTED_SITE_ADDRESS, requestedSiteAddress)
    }

    override fun onDestroyView() {
        loginSiteAddressValidator?.dispose()
        siteAddressInput = null

        super.onDestroyView()
    }

    override fun onEditorCommit() {
        if (bottomButton.isEnabled) {
            discover()
        }
    }

    override fun afterTextChanged(s: Editable) {
        siteAddressInput?.let { siteAddressInput ->
            loginSiteAddressValidator?.setAddress(EditTextUtils.getText(siteAddressInput.editText))
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            siteAddressInput?.setError(null)
    }

    private fun showError(messageId: Int) {
        val message = getString(messageId)
        mAnalyticsListener.trackFailure(message)
        siteAddressInput?.setError(message)
    }

    override fun endProgress() {
        super.endProgress()
        requestedSiteAddress = null
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

        requestedSiteAddress = loginSiteAddressValidator?.cleanedSiteAddress

        val cleanedUrl = stripKnownPaths(requestedSiteAddress.orEmpty())

        mAnalyticsListener.trackConnectedSiteInfoRequested(cleanedUrl)

        // TODO discover
        Toast.makeText(
            requireContext(),
            "DISCOVER",
            Toast.LENGTH_LONG
        ).show()

        startProgress()
    }

    private fun stripKnownPaths(url: String): String {
        val cleanedXmlrpcSuffix = UrlUtils.removeXmlrpcSuffix(url)

        // Make sure to use a valid URL so that DiscoveryUtils#stripKnownPaths is able to strip paths
        val scheme = cleanedXmlrpcSuffix.toUri().scheme
        val urlWithScheme = if (scheme == null) {
            UrlUtils.addUrlSchemeIfNeeded(cleanedXmlrpcSuffix, false)
        } else {
            cleanedXmlrpcSuffix
        }

        val cleanedUrl = DiscoveryUtils.stripKnownPaths(urlWithScheme.orEmpty())

        // Revert the scheme changes
        return if (scheme == null) UrlUtils.removeScheme(cleanedUrl) else cleanedUrl
    }

    companion object {
        private const val KEY_REQUESTED_SITE_ADDRESS = "KEY_REQUESTED_SITE_ADDRESS"

        const val TAG: String = "login_site_application_password_fragment_tag"
    }
}

