package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.ActivityNotFoundException
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
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
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
import org.wordpress.android.login.LoginMode
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment
import org.wordpress.android.login.LoginSiteAddressValidator
import org.wordpress.android.login.R
import org.wordpress.android.login.widgets.WPLoginInputRow
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.util.AppLog
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
            mLoginListener.helpSiteAddress(loginSiteAddressValidator.cleanedSiteAddress)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                    openApplicationPasswordLogin(url)
                }
            }
        }

        viewModel.loadingStateFlow
            .flowWithLifecycle(viewLifecycleOwner.lifecycle,  Lifecycle.State.STARTED)
            .onEach({ loading ->
                if (loading) {
                    startProgress()
                } else {
                    endProgressIfNeeded()
                }
            })
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun openApplicationPasswordLogin(url: String) {
        val intent = getCustomTabsIntent()
        val loginUri = url.toUri()
        val activity = requireActivity()
        try {
            intent.launchUrl(activity, loginUri)
        } catch (e: SecurityException) {
            AppLog.e(
                AppLog.T.UTILS,
                "Error opening login uri in CustomTabsIntent, attempting external browser",
                e
            )
            ActivityLauncher.openUrlExternal(activity, loginUri.toString())
        } catch (e: ActivityNotFoundException) {
            AppLog.e(
                AppLog.T.UTILS,
                "Error opening login uri in CustomTabsIntent, attempting external browser",
                e
            )
            ActivityLauncher.openUrlExternal(activity, loginUri.toString())
        }
    }

    private fun getCustomTabsIntent(): CustomTabsIntent {
        val activity = requireActivity()
        return CustomTabsIntent.Builder()
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setStartAnimations(
                requireActivity(),
                org.wordpress.android.R.anim.activity_slide_in_from_right,
                org.wordpress.android.R.anim.activity_slide_out_to_left
            )
            .setExitAnimations(
                activity,
                org.wordpress.android.R.anim.activity_slide_in_from_left,
                org.wordpress.android.R.anim.activity_slide_out_to_right
            )
            .setUrlBarHidingEnabled(true)
            .setInstantAppsEnabled(false)
            .setShowTitle(false)
            .build()
    }

    override fun onResume() {
        super.onResume()

        mAnalyticsListener.siteAddressFormScreenResumed()
    }

    override fun onDestroyView() {
        loginSiteAddressValidator.dispose()
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

