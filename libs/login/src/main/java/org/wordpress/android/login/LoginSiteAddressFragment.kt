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
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
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
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.login.viewmodel.LoginSiteAddressViewModel

class LoginSiteAddressFragment : LoginBaseDiscoveryFragment(), TextWatcher, OnEditorCommitListener,
    LoginBaseDiscoveryListener {
    private var siteAddressInput: WPLoginInputRow? = null

    private var requestedSiteAddress: String? = null

    private var connectSiteInfoUrl: String? = null
    private var connectSiteInfoUrlRedirect: String? = null
    private var connectSiteInfoCalculatedHasJetpack = false

    private var loginSiteAddressValidator: LoginSiteAddressValidator? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LoginSiteAddressViewModel

    @JvmField
    @Inject
    var accountStore: AccountStore? = null

    @JvmField
    @Inject
    var dispatcher: Dispatcher? = null

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
        // important for accessibility - talkback
        requireActivity().setTitle(R.string.site_address_login_title)
        val siteAddressInput: WPLoginInputRow = rootView.findViewById(R.id.login_site_address_row)
        this.siteAddressInput = siteAddressInput
        if (BuildConfig.DEBUG) {
            siteAddressInput.editText.setText(BuildConfig.DEBUG_WPCOM_WEBSITE_URL)
        }
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

        viewModel = ViewModelProvider(this, viewModelFactory)[LoginSiteAddressViewModel::class.java]

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

        mLoginBaseDiscoveryListener = this

        requestedSiteAddress = loginSiteAddressValidator?.cleanedSiteAddress

        val cleanedUrl = stripKnownPaths(requestedSiteAddress.orEmpty())

        // This work is in progress as right now we are just testing the API discovery through the RS library
        // No further actions are taken
        viewModel.runApiDiscovery(cleanedUrl)

        mAnalyticsListener.trackConnectedSiteInfoRequested(cleanedUrl)
        dispatcher?.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(cleanedUrl))

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

    override fun getRequestedSiteAddress(): String {
        return requestedSiteAddress.orEmpty()
    }

    override fun handleDiscoveryError(error: DiscoveryError, failedEndpoint: String?) {
        when (error) {
            DiscoveryError.ERRONEOUS_SSL_CERTIFICATE -> mLoginListener.handleSslCertificateError(
                memorizingTrustManager,
                SelfSignedSSLCallback {
                    if (failedEndpoint == null) {
                        return@SelfSignedSSLCallback
                    }
                    // retry site lookup
                    discover()
                })

            DiscoveryError.HTTP_AUTH_REQUIRED -> askForHttpAuthCredentials(
                failedEndpoint.orEmpty(),
                R.string.login_error_xml_rpc_cannot_read_site_auth_required
            )

            DiscoveryError.NO_SITE_ERROR -> showError(R.string.no_site_error)
            DiscoveryError.INVALID_URL -> {
                showError(R.string.invalid_site_url_message)
                mAnalyticsListener.trackInsertedInvalidUrl()
            }

            DiscoveryError.MISSING_XMLRPC_METHOD -> showError(R.string.xmlrpc_missing_method_error)
            DiscoveryError.WORDPRESS_COM_SITE -> {}
            DiscoveryError.XMLRPC_BLOCKED -> showError(R.string.xmlrpc_post_blocked_error)
            DiscoveryError.XMLRPC_FORBIDDEN -> showError(R.string.xmlrpc_endpoint_forbidden_error)
            DiscoveryError.GENERIC_ERROR -> showError(R.string.error_generic)
        }
    }

    override fun handleWpComDiscoveryError(failedEndpoint: String) {
        AppLog.e(AppLog.T.API, "Inputted a wpcom address in site address screen.")

        // If the user is already logged in a wordpress.com account, bail out
        val accountStore = this.accountStore
        if (accountStore?.hasAccessToken() == true) {
            val currentUsername = accountStore.account.userName
            AppLog.e(
                AppLog.T.NUX,
                "User is already logged in WordPress.com: $currentUsername"
            )

            val oldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, true)
            mLoginListener.alreadyLoggedInWpcom(oldSitesIDs)
        } else {
            mLoginListener.gotWpcomSiteInfo(failedEndpoint)
        }
    }

    override fun handleDiscoverySuccess(endpointAddress: String) {
        AppLog.i(
            AppLog.T.NUX,
            "Discovery succeeded, endpoint: $endpointAddress"
        )

        // hold the URL in a variable to use below otherwise it gets cleared up by endProgress
        val inputSiteAddress = requestedSiteAddress
        endProgress()
        if (mLoginListener.loginMode == LoginMode.WOO_LOGIN_MODE) {
            mLoginListener.gotConnectedSiteInfo(
                connectSiteInfoUrl.orEmpty(),
                connectSiteInfoUrlRedirect,
                connectSiteInfoCalculatedHasJetpack
            )
        } else {
            mLoginListener.gotXmlRpcEndpoint(inputSiteAddress, endpointAddress)
        }
    }

    @Suppress("DEPRECATION")
    private fun askForHttpAuthCredentials(url: String, messageId: Int) {
        val loginHttpAuthDialogFragment = LoginHttpAuthDialogFragment.newInstance(
            url,
            getString(messageId)
        )
        loginHttpAuthDialogFragment.setTargetFragment(
            this,
            LoginHttpAuthDialogFragment.DO_HTTP_AUTH
        )
        loginHttpAuthDialogFragment.show(parentFragmentManager, LoginHttpAuthDialogFragment.TAG)
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

    // OnChanged events
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFetchedConnectSiteInfo(event: OnConnectSiteInfoChecked) {
        if (requestedSiteAddress == null) {
            // bail if user canceled
            return
        }

        if (!isAdded) {
            return
        }

        if (event.isError) {
            mAnalyticsListener.trackConnectedSiteInfoFailed(
                requestedSiteAddress,
                event.javaClass.simpleName,
                event.error.type.name,
                event.error.message
            )

            AppLog.e(AppLog.T.API, "onFetchedConnectSiteInfo has error: " + event.error.message)
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                showError(R.string.invalid_site_url_message)
            } else {
                showError(R.string.error_generic_network)
            }

            endProgressIfNeeded()
        } else {
            val hasJetpack = calculateHasJetpack(event.info)

            connectSiteInfoUrl = event.info.url
            connectSiteInfoUrlRedirect = event.info.urlAfterRedirects
            connectSiteInfoCalculatedHasJetpack = hasJetpack

            mAnalyticsListener.trackConnectedSiteInfoSucceeded(
                createConnectSiteInfoProperties(
                    event.info,
                    hasJetpack
                )
            )

            if (mLoginListener.loginMode == LoginMode.WOO_LOGIN_MODE) {
                handleConnectSiteInfoForWoo(event.info)
            } else {
                handleConnectSiteInfoForWordPress(event.info)
            }
        }
    }

    private fun handleConnectSiteInfoForWoo(siteInfo: ConnectSiteInfoPayload) {
        if (!siteInfo.exists) {
            endProgressIfNeeded()
            // Site does not exist
            showError(R.string.invalid_site_url_message)
        } else if (!siteInfo.isWordPress) {
            endProgressIfNeeded()
            // Not a WordPress site
            mLoginListener.handleSiteAddressError(siteInfo)
        } else {
            endProgressIfNeeded()
            mLoginListener.gotConnectedSiteInfo(
                connectSiteInfoUrl.orEmpty(),
                connectSiteInfoUrlRedirect,
                connectSiteInfoCalculatedHasJetpack
            )
        }
    }

    private fun handleConnectSiteInfoForWordPress(siteInfo: ConnectSiteInfoPayload) {
        if (siteInfo.isWPCom) {
            // It's a Simple or Atomic site
            val mode = mLoginListener.loginMode
            if (mode == LoginMode.SELFHOSTED_ONLY || mode == LoginMode.JETPACK_SELFHOSTED) {
                // We're only interested in self-hosted sites
                if (siteInfo.hasJetpack) {
                    // This is an Atomic site, so treat it as self-hosted and start the discovery process
                    initiateDiscovery()
                    return
                }
            }
            endProgressIfNeeded()
            mLoginListener.gotWpcomSiteInfo(UrlUtils.removeScheme(siteInfo.url))
        } else {
            // It's a Jetpack or self-hosted site
            if (mLoginListener.loginMode == LoginMode.WPCOM_LOGIN_ONLY) {
                // We're only interested in WordPress.com accounts
                showError(R.string.enter_wpcom_or_jetpack_site)
                endProgressIfNeeded()
            } else {
                // Start the discovery process
                initiateDiscovery()
            }
        }
    }

    private fun calculateHasJetpack(siteInfo: ConnectSiteInfoPayload): Boolean {
        // Determining if jetpack is actually installed takes additional logic. This final
        // calculated event property will make querying this event more straight-forward.
        // Internal reference: p99K0U-1vO-p2#comment-3574
        var hasJetpack = false
        if (siteInfo.isWPCom && siteInfo.hasJetpack) {
            // This is likely an atomic site.
            hasJetpack = true
        } else if (siteInfo.isJetpackConnected) {
            hasJetpack = true
        }
        return hasJetpack
    }

    private fun createConnectSiteInfoProperties(
        siteInfo: ConnectSiteInfoPayload,
        hasJetpack: Boolean
    ): Map<String, String?> {
        val properties = HashMap<String, String?>()
        properties[KEY_SITE_INFO_URL] = siteInfo.url
        properties[KEY_SITE_INFO_URL_AFTER_REDIRECTS] =
            siteInfo.urlAfterRedirects
        properties[KEY_SITE_INFO_EXISTS] =
            siteInfo.exists.toString()
        properties[KEY_SITE_INFO_HAS_JETPACK] =
            siteInfo.hasJetpack.toString()
        properties[KEY_SITE_INFO_IS_JETPACK_ACTIVE] =
            siteInfo.isJetpackActive.toString()
        properties[KEY_SITE_INFO_IS_JETPACK_CONNECTED] =
            siteInfo.isJetpackConnected.toString()
        properties[KEY_SITE_INFO_IS_WORDPRESS] =
            siteInfo.isWordPress.toString()
        properties[KEY_SITE_INFO_IS_WPCOM] =
            siteInfo.isWPCom.toString()
        properties[KEY_SITE_INFO_CALCULATED_HAS_JETPACK] =
            hasJetpack.toString()
        return properties
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

