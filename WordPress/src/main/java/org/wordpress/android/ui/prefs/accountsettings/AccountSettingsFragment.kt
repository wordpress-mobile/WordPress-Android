package org.wordpress.android.ui.prefs.accountsettings

import android.app.ProgressDialog
import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import android.preference.Preference.OnPreferenceClickListener
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import org.wordpress.android.R
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.R.xml
import org.wordpress.android.WordPress
import org.wordpress.android.ui.FullScreenDialogFragment
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener
import org.wordpress.android.ui.accounts.signup.BaseUsernameChangerFullScreenDialogFragment
import org.wordpress.android.ui.accounts.signup.SettingsUsernameChangerFragment
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.DetailListPreference
import org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation
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
const val SNACKBAR_NO_OF_LINES_FOUR = 4
class AccountSettingsFragment : PreferenceFragmentLifeCycleOwner(),
        OnPreferenceChangeListener, OnConfirmListener, OnPreferenceClickListener {
    @set:Inject lateinit var uiHelpers: UiHelpers
    @set:Inject lateinit var viewModel: AccountSettingsViewModel
    private lateinit var mUsernamePreference: Preference
    private lateinit var mEmailPreference: EditTextPreferenceWithValidation
    private lateinit var mPrimarySitePreference: DetailListPreference
    private lateinit var mWebAddressPreference: EditTextPreferenceWithValidation
    private lateinit var mChangePasswordPreference: EditTextPreferenceWithValidation
    private var mChangePasswordProgressDialog: ProgressDialog? = null
    private var mEmailSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity.application as WordPress).component().inject(this)
        retainInstance = true
        addPreferencesFromResource(xml.account_settings)
        mUsernamePreference = findPreference(getString(string.pref_key_username))
        mEmailPreference = findPreference(getString(string.pref_key_email)) as EditTextPreferenceWithValidation
        mPrimarySitePreference = findPreference(getString(string.pref_key_primary_site)) as DetailListPreference
        mWebAddressPreference = findPreference(getString(string.pref_key_web_address))
                as EditTextPreferenceWithValidation
        mChangePasswordPreference = findPreference(getString(string.pref_key_change_password))
                as EditTextPreferenceWithValidation
        mEmailPreference.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        mEmailPreference.setValidationType(EMAIL)
        mWebAddressPreference.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        mWebAddressPreference.setValidationType(URL)
        mWebAddressPreference.setDialogMessage(string.web_address_dialog_hint)
        mChangePasswordPreference.editText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        mChangePasswordPreference.setValidationType(PASSWORD)
        mChangePasswordPreference.setDialogMessage(string.change_password_dialog_hint)
        mUsernamePreference.onPreferenceClickListener = this
        mEmailPreference.onPreferenceChangeListener = this
        mPrimarySitePreference.onPreferenceChangeListener = this
        mWebAddressPreference.onPreferenceChangeListener = this
        mChangePasswordPreference.onPreferenceChangeListener = this
        setTextAlignment(mEmailPreference.editText)
        setTextAlignment(mWebAddressPreference.editText)
        setTextAlignment(mChangePasswordPreference.editText)
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
        mWebAddressPreference.summary = accountSettingsUiState.webAddressSettingsUiState.webAddress
        updatePrimarySitePreference(accountSettingsUiState.primarySiteSettingsUiState)
        updateChangePasswordPreference(accountSettingsUiState.changePasswordSettingsUiState)
        accountSettingsUiState.error?.let {
            showToastMessage(it)
        }
    }

    private fun updateChangePasswordPreference(changePasswordSettingsUiState: ChangePasswordSettingsUiState) {
        showChangePasswordProgressDialog(changePasswordSettingsUiState.showChangePasswordProgressDialog)
    }

    private fun updateUserNamePreferenceUi(userNameSettingUiState: UserNameSettingsUiState) {
        mUsernamePreference.apply {
            summary = userNameSettingUiState.userName
            isEnabled = userNameSettingUiState.canUserNameBeChanged
        }
        if (userNameSettingUiState.showUserNameConfirmedSnackBar) {
            showUserNameSnackBar(userNameSettingUiState.newUserChangeConfirmedSnackBarMessageHolder)
        }
    }

    private fun updateEmailPreferenceUi(emailSettingsUiState: EmailSettingsUiState) {
        mEmailPreference.apply {
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
            mPrimarySitePreference.apply {
                value = (state.primarySite?.siteId ?: "").toString()
                summary = state.primarySite?.siteName
                entries = state.siteNames
                entryValues = state.siteIds
                setDetails(state.homeURLOrHostNames)
                refreshAdapter()
            }
        } ?: run {
            mPrimarySitePreference.apply {
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
        if (mEmailSnackbar == null) {
            mEmailSnackbar = WPSnackbar.make(
                    view!!,
                    uiHelpers.getTextOfUiString(context, snackBarMessage.message),
                    BaseTransientBottomBar.LENGTH_INDEFINITE
            )
            snackBarMessage.buttonTitle?.let {
                mEmailSnackbar?.setAction(
                        uiHelpers.getTextOfUiString(
                                context,
                                snackBarMessage.buttonTitle
                        )
                ) { snackBarMessage.buttonAction }
            }
            val textView = mEmailSnackbar?.view?.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView?.maxLines = SNACKBAR_NO_OF_LINES_FOUR
        }
        mEmailSnackbar?.let {
            if (!it.isShown) {
                mEmailSnackbar?.show()
            }
        }
    }

    private fun dismissSnackBar() {
        mEmailSnackbar?.dismiss()
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        if (preference == mUsernamePreference) {
            showUsernameChangerFragment(
                    viewModel.accountSettingsUiState.value.userNameSettingsUiState.userName,
                    viewModel.accountSettingsUiState.value.userNameSettingsUiState.displayName
            )
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference) {
            mEmailPreference -> viewModel.onEmailChanged(newValue.toString())
            mPrimarySitePreference -> viewModel.onPrimarySiteChanged(newValue.toString().toLong())
            mWebAddressPreference -> viewModel.onWebAddressChanged(newValue.toString())
            mChangePasswordPreference -> viewModel.onPasswordChanged(newValue.toString())
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> activity.finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setTextAlignment(editText: EditText) {
        editText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun showChangePasswordProgressDialog(show: Boolean) {
        if (!show) {
            mChangePasswordProgressDialog?.dismiss()
            mChangePasswordProgressDialog = null
            return
        }
        createChangePasswordDialogIfNull()
        mChangePasswordProgressDialog?.show()
    }

    private fun createChangePasswordDialogIfNull() {
        if (mChangePasswordProgressDialog == null) {
            mChangePasswordProgressDialog = ProgressDialog(activity).apply {
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
                .setOnDismissListener(null)
                .setContent(SettingsUsernameChangerFragment::class.java, bundle)
                .build()
                .show((activity as AppCompatActivity).supportFragmentManager,
                        FullScreenDialogFragment.TAG
                )
    }

    override fun onConfirm(result: Bundle?) {
        result?.getString(BaseUsernameChangerFullScreenDialogFragment.RESULT_USERNAME)?.let {
            viewModel.onUsernameChangeConfirmedFromServer(it)
        }
    }
}
