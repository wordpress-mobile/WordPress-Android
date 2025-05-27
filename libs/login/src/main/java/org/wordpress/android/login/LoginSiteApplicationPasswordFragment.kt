package org.wordpress.android.login

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.network.discovery.DiscoveryUtils
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import org.wordpress.android.login.LoginBaseDiscoveryFragment.LoginBaseDiscoveryListener
import org.wordpress.android.login.LoginListener.SelfSignedSSLCallback
import org.wordpress.android.login.util.SiteUtils
import org.wordpress.android.login.widgets.WPLoginInputRow
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class LoginSiteApplicationPasswordFragment : LoginBaseFormFragment<LoginListener>(), TextWatcher, OnEditorCommitListener {
    private var siteAddressInput: WPLoginInputRow? = null

    private var requestedSiteAddress: String? = null

    private var connectSiteInfoUrl: String? = null
    private var connectSiteInfoUrlRedirect: String? = null
    private var connectSiteInfoCalculatedHasJetpack = false

    private var loginSiteAddressValidator: LoginSiteAddressValidator? = null

    @JvmField
    @Inject
    var accountStore: AccountStore? = null

    @JvmField
    @Inject
    var httpAuthManager: HTTPAuthManager? = null

    @JvmField
    @Inject
    var memorizingTrustManager: MemorizingTrustManager? = null

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
            connectSiteInfoUrl = savedInstanceState.getString(KEY_SITE_INFO_URL)
            connectSiteInfoUrlRedirect =
                savedInstanceState.getString(KEY_SITE_INFO_URL_AFTER_REDIRECTS)
            connectSiteInfoCalculatedHasJetpack =
                savedInstanceState.getBoolean(KEY_SITE_INFO_CALCULATED_HAS_JETPACK)
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
        outState.putString(KEY_SITE_INFO_URL, connectSiteInfoUrl)
        outState.putString(KEY_SITE_INFO_URL_AFTER_REDIRECTS, connectSiteInfoUrlRedirect)
        outState.putBoolean(
            KEY_SITE_INFO_CALCULATED_HAS_JETPACK,
            connectSiteInfoCalculatedHasJetpack
        )
    }

    override fun onDestroyView() {
        loginSiteAddressValidator?.dispose()
        siteAddressInput = null

        super.onDestroyView()
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

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        connectSiteInfoUrl = null
        connectSiteInfoUrlRedirect = null
        connectSiteInfoCalculatedHasJetpack = false
    }

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

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LoginHttpAuthDialogFragment.DO_HTTP_AUTH &&
            resultCode == Activity.RESULT_OK && data != null) {
            val url = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_URL)
            val httpUsername = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_USERNAME)
            val httpPassword = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_PASSWORD)
            httpAuthManager?.addHTTPAuthCredentials(
                httpUsername.orEmpty(),
                httpPassword.orEmpty(),
                url.orEmpty(),
                null
            )
            discover()
        }
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

        private const val KEY_SITE_INFO_URL = "url"
        private const val KEY_SITE_INFO_URL_AFTER_REDIRECTS = "url_after_redirects"
        private const val KEY_SITE_INFO_EXISTS = "exists"
        private const val KEY_SITE_INFO_HAS_JETPACK = "has_jetpack"
        private const val KEY_SITE_INFO_IS_JETPACK_ACTIVE = "is_jetpack_active"
        private const val KEY_SITE_INFO_IS_JETPACK_CONNECTED = "is_jetpack_connected"
        private const val KEY_SITE_INFO_IS_WORDPRESS = "is_wordpress"
        private const val KEY_SITE_INFO_IS_WPCOM = "is_wp_com"
        private const val KEY_SITE_INFO_CALCULATED_HAS_JETPACK = "login_calculated_has_jetpack"

        const val TAG: String = "login_site_address_fragment_tag"
    }
}

