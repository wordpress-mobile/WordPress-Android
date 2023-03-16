@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.prefs.accountsettings

import android.app.ProgressDialog
import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference.OnPreferenceClickListener
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.signup.BaseUsernameChangerFullScreenDialogFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.DetailListPreference
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType.EMAIL
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType.PASSWORD
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType.URL
import org.wordpress.android.ui.prefs.PreferenceFragmentLifeCycleOwner
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.EMAIL_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.PASSWORD_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.PRIMARY_SITE_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.USERNAME_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.USERNAME_CHANGE_SCREEN_DISMISSED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.USERNAME_CHANGE_SCREEN_DISPLAYED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsEvent.WEB_ADDRESS_CHANGED
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.ChangePasswordSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.EmailSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.PrimarySiteSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.UserNameSettingsUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.SETTINGS
import org.wordpress.android.util.SnackbarItem
import org.wordpress.android.util.SnackbarItem.Info
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

private const val SNACKBAR_NO_OF_LINES_FOUR = 4
private const val EMPTY_STRING = ""

@Suppress("DEPRECATION")
class AccountSettingsFragment : PreferenceFragmentLifeCycleOwner(),
    OnPreferenceChangeListener, OnPreferenceClickListener {
    @set:Inject
    lateinit var uiHelpers: UiHelpers

    @set:Inject
    lateinit var viewModel: AccountSettingsViewModel

    @set:Inject
    lateinit var snackbarSequencer: SnackbarSequencer

    @set:Inject
    lateinit var analyticsTracker: AccountSettingsAnalyticsTracker

    @set:Inject
    lateinit var navigationHandler: AccountSettingsNavigationHandler
    private lateinit var usernamePreference: Preference
    private lateinit var emailPreference: EditTextPreferenceWithValidation
    private lateinit var primarySitePreference: DetailListPreference
    private lateinit var webAddressPreference: EditTextPreferenceWithValidation
    private lateinit var changePasswordPreference: EditTextPreferenceWithValidation

    @Suppress("DEPRECATION")
    private var changePasswordProgressDialog: ProgressDialog? = null
    private var emailSnackbar: Snackbar? = null

    @Deprecated("Deprecated")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity.application as WordPress).component().inject(this)
        retainInstance = true
        addPreferencesFromResource(R.xml.account_settings)
        bindPreferences()
        setUpListeners()
        emailPreference.configure(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            validationType = EMAIL
        )
        webAddressPreference.configure(
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
            validationType = URL,
            dialogMessage = R.string.web_address_dialog_hint
        )
        changePasswordPreference.configure(
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD,
            validationType = PASSWORD,
            dialogMessage = R.string.change_password_dialog_hint
        )
    }

    private fun bindPreferences() {
        usernamePreference = findPreference(getString(R.string.pref_key_username))
        emailPreference = findPreference(getString(R.string.pref_key_email)) as EditTextPreferenceWithValidation
        primarySitePreference = findPreference(getString(R.string.pref_key_primary_site)) as DetailListPreference
        webAddressPreference = findPreference(getString(R.string.pref_key_web_address))
                as EditTextPreferenceWithValidation
        changePasswordPreference = findPreference(getString(R.string.pref_key_change_password))
                as EditTextPreferenceWithValidation
        changePasswordPreference.summary = EMPTY_STRING
    }

    private fun setUpListeners() {
        usernamePreference.onPreferenceClickListener = this@AccountSettingsFragment
        primarySitePreference.onPreferenceChangeListener = this@AccountSettingsFragment
        emailPreference.onPreferenceChangeListener = this@AccountSettingsFragment
        webAddressPreference.onPreferenceChangeListener = this@AccountSettingsFragment
        changePasswordPreference.onPreferenceChangeListener = this@AccountSettingsFragment
    }

    private fun EditTextPreferenceWithValidation.configure(
        inputType: Int,
        validationType: ValidationType,
        dialogMessage: Int? = null
    ) {
        editText.inputType = inputType
        editText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        setValidationType(validationType)
        dialogMessage?.let { setDialogMessage(it) }
    }

    @Deprecated("Deprecated")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val coordinatorView = inflater.inflate(R.layout.preference_coordinator, container, false)
        val coordinator: CoordinatorLayout = coordinatorView.findViewById(R.id.coordinator)
        val preferenceView = super.onCreateView(inflater, coordinator, savedInstanceState)
        val listOfPreferences = preferenceView?.findViewById<ListView>(android.R.id.list)
        if (listOfPreferences != null) {
            ViewCompat.setNestedScrollingEnabled(listOfPreferences, true)
        }
        coordinator.addView(preferenceView)
        return coordinatorView
    }

    @Deprecated("Deprecated")
    override fun onStart() {
        super.onStart()
        observeAccountSettingsViewState()
    }

    private fun observeAccountSettingsViewState() {
        this.lifecycleScope.launchWhenStarted {
            viewModel.accountSettingsUiState.collect { updateAccountSettings(it) }
        }
    }

    private fun updateAccountSettings(accountSettingsUiState: AccountSettingsUiState) {
        updateUserNamePreferenceUi(accountSettingsUiState.userNameSettingsUiState)
        updateEmailPreferenceUi(accountSettingsUiState.emailSettingsUiState)
        webAddressPreference.summary = accountSettingsUiState.webAddressSettingsUiState.webAddress
        updatePrimarySitePreferenceUi(accountSettingsUiState.primarySiteSettingsUiState)
        updateChangePasswordPreferenceUi(accountSettingsUiState.changePasswordSettingsUiState)
        accountSettingsUiState.toastMessage?.let {
            showToastMessage(it)
        }
    }

    private fun updateChangePasswordPreferenceUi(changePasswordSettingsUiState: ChangePasswordSettingsUiState) {
        showChangePasswordProgressDialog(changePasswordSettingsUiState.showChangePasswordProgressDialog)
    }

    private fun updateUserNamePreferenceUi(userNameSettingUiState: UserNameSettingsUiState) {
        usernamePreference.apply {
            summary = userNameSettingUiState.userName
            isEnabled = userNameSettingUiState.canUserNameBeChanged
        }
        if (userNameSettingUiState.showUserNameConfirmedSnackBar) {
            showUserNameSnackBar(userNameSettingUiState.newUserChangeConfirmedSnackBarMessageHolder)
            viewModel.onUserConfirmedSnackBarShown()
        }
    }

    private fun updateEmailPreferenceUi(emailSettingsUiState: EmailSettingsUiState) {
        emailPreference.apply {
            summary = emailSettingsUiState.email
            isEnabled = emailSettingsUiState.hasPendingEmailChange.not()
        }
        if (emailSettingsUiState.hasPendingEmailChange) {
            showSnackBar(emailSettingsUiState.emailVerificationMsgSnackBarMessageHolder)
        } else {
            dismissSnackBar()
        }
    }

    private fun updatePrimarySitePreferenceUi(primarySiteSettingsUiState: PrimarySiteSettingsUiState?) {
        primarySiteSettingsUiState?.let { state ->
            primarySitePreference.apply {
                value = (state.primarySite?.siteId ?: "").toString()
                summary = state.primarySite?.siteName
                entries = state.siteNames
                entryValues = state.siteIds
                canShowDialog = state.canShowChoosePrimarySiteDialog
                setDetails(state.homeURLOrHostNames)
                refreshAdapter()
                 // Add click listener to show toast
                setOnPreferenceClickListener {
                    if (state.sites?.size == 1) {
                        val message = getString(R.string.only_one_primary_site_message)
                        showToastMessage(message)
                    }
                    true
                }
            }
        } ?: run {
            primarySitePreference.apply {
                refreshAdapter()
            }
        }
    }

    private fun showToastMessage(toastMessage: String) {
        ToastUtils.showToast(activity, toastMessage, LONG)
        viewModel.onToastShown(toastMessage)
        AppLog.e(SETTINGS, toastMessage)
    }

    private fun showUserNameSnackBar(userName: SnackbarMessageHolder) {
        view?.let {
            snackbarSequencer.enqueue(
                SnackbarItem(Info(view = it, textRes = userName.message, duration = userName.duration))
            )
        }
    }

    private fun showSnackBar(snackBarMessage: SnackbarMessageHolder) {
        emailSnackbar?.let {
            if (!it.isShown) {
                emailSnackbar?.show()
            }
        } ?: run {
            view?.let {
                emailSnackbar = WPSnackbar.make(
                    it,
                    uiHelpers.getTextOfUiString(context, snackBarMessage.message),
                    BaseTransientBottomBar.LENGTH_INDEFINITE
                )
                snackBarMessage.buttonTitle?.let {
                    emailSnackbar?.setAction(
                        uiHelpers.getTextOfUiString(context, snackBarMessage.buttonTitle)
                    ) { snackBarMessage.buttonAction }
                }
                val textView = emailSnackbar?.view
                    ?.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                textView?.maxLines = SNACKBAR_NO_OF_LINES_FOUR
            }
        }
    }

    private fun dismissSnackBar() {
        emailSnackbar?.dismiss()
    }

    @Deprecated("Deprecated")
    override fun onPreferenceClick(preference: Preference?): Boolean {
        if (preference == usernamePreference) {
            val onUserNameConfirmed = { result: Bundle? ->
                result?.getString(BaseUsernameChangerFullScreenDialogFragment.RESULT_USERNAME)?.let {
                    viewModel.onUsernameChangeConfirmedFromServer(it)
                    analyticsTracker.track(USERNAME_CHANGED)
                }
            }
            navigationHandler.showUsernameChangerScreen(
                activity,
                UserNameChangeScreenRequest(
                    userName = viewModel.accountSettingsUiState.value.userNameSettingsUiState.userName,
                    displayName = viewModel.accountSettingsUiState.value.userNameSettingsUiState.displayName,
                    onConfirm = onUserNameConfirmed,
                    onShown = { analyticsTracker.track(USERNAME_CHANGE_SCREEN_DISPLAYED) },
                    onDismiss = { analyticsTracker.track(USERNAME_CHANGE_SCREEN_DISMISSED) }
                )
            )
        }
        return true
    }

    @Deprecated("Deprecated")
    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        var action: AccountSettingsEvent? = null
        when (preference) {
            emailPreference -> {
                viewModel.onEmailChanged(newValue.toString())
                action = EMAIL_CHANGED
            }
            primarySitePreference -> {
                viewModel.onPrimarySiteChanged(newValue.toString().toLong())
                action = PRIMARY_SITE_CHANGED
            }
            webAddressPreference -> {
                viewModel.onWebAddressChanged(newValue.toString())
                action = WEB_ADDRESS_CHANGED
            }
            changePasswordPreference -> {
                viewModel.onPasswordChanged(newValue.toString())
                action = PASSWORD_CHANGED
            }
        }
        if (!preference.summary.toString().equals(newValue.toString(), ignoreCase = true)) {
            action?.let { analyticsTracker.track(it) }
        }
        return true
    }

    @Deprecated("Deprecated")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity.finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showChangePasswordProgressDialog(show: Boolean) {
        if (!show) {
            changePasswordProgressDialog?.dismiss()
            changePasswordProgressDialog = null
            return
        }
        createChangePasswordDialogIfNull()
        changePasswordProgressDialog?.show()
    }

    @Suppress("DEPRECATION")
    private fun createChangePasswordDialogIfNull() {
        if (changePasswordProgressDialog == null) {
            changePasswordProgressDialog = ProgressDialog(activity).apply {
                setCancelable(false)
                isIndeterminate = true
                setMessage(getString(R.string.change_password_dialog_message))
            }
        }
    }
}
