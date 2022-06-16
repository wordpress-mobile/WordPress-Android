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
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.R.xml
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.CHANGE_USERNAME_DISMISSED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.CHANGE_USERNAME_DISPLAYED
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.FullScreenDialogFragment.OnDismissListener
import org.wordpress.android.ui.FullScreenDialogFragment.OnShownListener
import org.wordpress.android.ui.accounts.signup.BaseUsernameChangerFullScreenDialogFragment
import org.wordpress.android.ui.accounts.signup.SettingsUsernameChangerFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.DetailListPreference
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType.EMAIL
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType.PASSWORD
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation.ValidationType.URL
import org.wordpress.android.ui.prefs.PreferenceFragmentLifeCycleOwner
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.ChangePasswordSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.EmailSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.PrimarySiteSettingsUiState
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.UserNameSettingsUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.SETTINGS
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration.LONG
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

private const val SNACKBAR_NO_OF_LINES_FOUR = 4
private const val SOURCE = "source"
private const val SOURCE_ACCOUNT_SETTINGS = "account_settings"

@Suppress("DEPRECATION")
class AccountSettingsFragment : PreferenceFragmentLifeCycleOwner(),
        OnPreferenceChangeListener, OnPreferenceClickListener, OnConfirmListener, OnShownListener, OnDismissListener {
    @set:Inject lateinit var uiHelpers: UiHelpers
    @set:Inject lateinit var viewModel: AccountSettingsViewModel
    private lateinit var usernamePreference: Preference
    private lateinit var emailPreference: EditTextPreferenceWithValidation
    private lateinit var primarySitePreference: DetailListPreference
    private lateinit var webAddressPreference: EditTextPreferenceWithValidation
    private lateinit var changePasswordPreference: EditTextPreferenceWithValidation
    private var changePasswordProgressDialog: ProgressDialog? = null
    private var emailSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity.application as WordPress).component().inject(this)
        retainInstance = true
        addPreferencesFromResource(xml.account_settings)
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
        usernamePreference = findPreference(getString(string.pref_key_username))
        emailPreference = findPreference(getString(string.pref_key_email)) as EditTextPreferenceWithValidation
        primarySitePreference = findPreference(getString(string.pref_key_primary_site)) as DetailListPreference
        webAddressPreference = findPreference(getString(string.pref_key_web_address))
                as EditTextPreferenceWithValidation
        changePasswordPreference = findPreference(getString(string.pref_key_change_password))
                as EditTextPreferenceWithValidation
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val coordinatorView = inflater.inflate(layout.preference_coordinator, container, false)
        val coordinator: CoordinatorLayout = coordinatorView.findViewById(R.id.coordinator)
        val preferenceView = super.onCreateView(inflater, coordinator, savedInstanceState)
        val listOfPreferences = preferenceView?.findViewById<ListView>(android.R.id.list)
        if (listOfPreferences != null) {
            ViewCompat.setNestedScrollingEnabled(listOfPreferences, true)
        }
        coordinator.addView(preferenceView)
        return coordinatorView
    }

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
        updatePrimarySitePreference(accountSettingsUiState.primarySiteSettingsUiState)
        updateChangePasswordPreference(accountSettingsUiState.changePasswordSettingsUiState)
        accountSettingsUiState.toastMessage?.let {
            showToastMessage(it)
        }
    }

    private fun updateChangePasswordPreference(changePasswordSettingsUiState: ChangePasswordSettingsUiState) {
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

    private fun updatePrimarySitePreference(primarySiteSettingsUiState: PrimarySiteSettingsUiState?) {
        primarySiteSettingsUiState?.let { state ->
            primarySitePreference.apply {
                value = (state.primarySite?.siteId ?: "").toString()
                summary = state.primarySite?.siteName
                entries = state.siteNames
                entryValues = state.siteIds
                setDetails(state.homeURLOrHostNames)
                refreshAdapter()
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
        WPSnackbar.make(view!!, uiHelpers.getTextOfUiString(context, userName.message),
                userName.duration).show()
    }

    private fun showSnackBar(snackBarMessage: SnackbarMessageHolder) {
        if (emailSnackbar == null) {
            emailSnackbar = WPSnackbar.make(
                    view!!,
                    uiHelpers.getTextOfUiString(context, snackBarMessage.message),
                    BaseTransientBottomBar.LENGTH_INDEFINITE
            )
            snackBarMessage.buttonTitle?.let {
                emailSnackbar?.setAction(
                        uiHelpers.getTextOfUiString(
                                context,
                                snackBarMessage.buttonTitle
                        )
                ) { snackBarMessage.buttonAction }
            }
            val textView = emailSnackbar?.view?.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView?.maxLines = SNACKBAR_NO_OF_LINES_FOUR
        }
        emailSnackbar?.let {
            if (!it.isShown) {
                emailSnackbar?.show()
            }
        }
    }

    private fun dismissSnackBar() {
        emailSnackbar?.dismiss()
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        if (preference == usernamePreference) {
            showUsernameChangerFragment(
                    viewModel.accountSettingsUiState.value.userNameSettingsUiState.userName,
                    viewModel.accountSettingsUiState.value.userNameSettingsUiState.displayName
            )
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference) {
            emailPreference -> viewModel.onEmailChanged(newValue.toString())
            primarySitePreference -> viewModel.onPrimarySiteChanged(newValue.toString().toLong())
            webAddressPreference -> viewModel.onWebAddressChanged(newValue.toString())
            changePasswordPreference -> viewModel.onPasswordChanged(newValue.toString())
        }
        return true
    }

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

    private fun createChangePasswordDialogIfNull() {
        if (changePasswordProgressDialog == null) {
            changePasswordProgressDialog = ProgressDialog(activity).apply {
                setCancelable(false)
                isIndeterminate = true
                setMessage(getString(string.change_password_dialog_message))
            }
        }
    }

    private fun showUsernameChangerFragment(userName: String, displayName: String) {
        val bundle: Bundle = BaseUsernameChangerFullScreenDialogFragment.newBundle(displayName, userName)
        FullScreenDialogFragment.Builder(activity)
                .setTitle(string.username_changer_title)
                .setAction(string.username_changer_action)
                .setOnConfirmListener(this)
                .setHideActivityBar(true)
                .setIsLifOnScroll(false)
                .setOnDismissListener(this)
                .setOnShownListener(this)
                .setContent(SettingsUsernameChangerFragment::class.java, bundle)
                .build()
                .show((activity as AppCompatActivity).supportFragmentManager,
                        FullScreenDialogFragment.TAG
                )
    }

    override fun onShown() {
        val props = mutableMapOf<String, String?>()
        props[SOURCE] = SOURCE_ACCOUNT_SETTINGS
        AnalyticsTracker.track(CHANGE_USERNAME_DISPLAYED, props)
    }

    override fun onDismiss() {
        val props = mutableMapOf<String, String?>()
        props[SOURCE] = SOURCE_ACCOUNT_SETTINGS
        AnalyticsTracker.track(CHANGE_USERNAME_DISMISSED, props)
    }

    override fun onConfirm(result: Bundle?) {
        result?.getString(BaseUsernameChangerFullScreenDialogFragment.RESULT_USERNAME)?.let {
            viewModel.onUsernameChangeConfirmedFromServer(it)
        }
    }
}
