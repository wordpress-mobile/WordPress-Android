package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.FullScreenDialogFragment;
import org.wordpress.android.ui.FullScreenDialogFragment.OnConfirmListener;
import org.wordpress.android.ui.accounts.signup.BaseUsernameChangerFullScreenDialogFragment;
import org.wordpress.android.ui.accounts.signup.SettingsUsernameChangerFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

@SuppressWarnings("deprecation")
public class AccountSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener,
        OnConfirmListener {
    private Preference mUsernamePreference;
    private EditTextPreferenceWithValidation mEmailPreference;
    private DetailListPreference mPrimarySitePreference;
    private EditTextPreferenceWithValidation mWebAddressPreference;
    private EditTextPreferenceWithValidation mChangePasswordPreference;
    private ProgressDialog mChangePasswordProgressDialog;
    private Snackbar mEmailSnackbar;

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.account_settings);

        mUsernamePreference = findPreference(getString(R.string.pref_key_username));
        mEmailPreference = (EditTextPreferenceWithValidation) findPreference(getString(R.string.pref_key_email));
        mPrimarySitePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_primary_site));
        mWebAddressPreference =
                (EditTextPreferenceWithValidation) findPreference(getString(R.string.pref_key_web_address));
        mChangePasswordPreference =
                (EditTextPreferenceWithValidation) findPreference(getString(R.string.pref_key_change_password));

        mEmailPreference.getEditText()
                        .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mEmailPreference.setValidationType(EditTextPreferenceWithValidation.ValidationType.EMAIL);
        mWebAddressPreference.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        mWebAddressPreference.setValidationType(EditTextPreferenceWithValidation.ValidationType.URL);
        mWebAddressPreference.setDialogMessage(R.string.web_address_dialog_hint);
        mChangePasswordPreference.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mChangePasswordPreference.setValidationType(EditTextPreferenceWithValidation.ValidationType.PASSWORD);
        mChangePasswordPreference.setDialogMessage(R.string.change_password_dialog_hint);

        mEmailPreference.setOnPreferenceChangeListener(this);
        mPrimarySitePreference.setOnPreferenceChangeListener(this);
        mWebAddressPreference.setOnPreferenceChangeListener(this);
        mChangePasswordPreference.setOnPreferenceChangeListener(this);

        setTextAlignment(mEmailPreference.getEditText());
        setTextAlignment(mWebAddressPreference.getEditText());
        setTextAlignment(mChangePasswordPreference.getEditText());

        // load site list asynchronously
        new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View coordinatorView = inflater.inflate(R.layout.preference_coordinator, container, false);
        CoordinatorLayout coordinator = coordinatorView.findViewById(R.id.coordinator);
        View preferenceView = super.onCreateView(inflater, coordinator, savedInstanceState);
        coordinator.addView(preferenceView);
        return coordinatorView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshAccountDetails();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) {
            return false;
        }

        if (preference == mEmailPreference) {
            updateEmail(newValue.toString());
            showPendingEmailChangeSnackbar(newValue.toString());
            mEmailPreference.setEnabled(false);
            return false;
        } else if (preference == mPrimarySitePreference) {
            changePrimaryBlogPreference(Long.parseLong(newValue.toString()));
            updatePrimaryBlog(newValue.toString());
            return false;
        } else if (preference == mWebAddressPreference) {
            mWebAddressPreference.setSummary(newValue.toString());
            updateWebAddress(newValue.toString());
            return false;
        } else if (preference == mChangePasswordPreference) {
            showChangePasswordProgressDialog(true);
            updatePassword(newValue.toString());
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTextAlignment(EditText editText) {
        editText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private void showChangePasswordProgressDialog(boolean show) {
        if (show && mChangePasswordProgressDialog == null) {
            mChangePasswordProgressDialog = new ProgressDialog(getActivity());
            mChangePasswordProgressDialog.setCancelable(false);
            mChangePasswordProgressDialog.setIndeterminate(true);
            mChangePasswordProgressDialog.setMessage(getString(R.string.change_password_dialog_message));
            mChangePasswordProgressDialog.show();
        } else if (!show && mChangePasswordProgressDialog != null) {
            mChangePasswordProgressDialog.dismiss();
            mChangePasswordProgressDialog = null;
        }
    }

    private void refreshAccountDetails() {
        AccountModel account = mAccountStore.getAccount();
        mUsernamePreference.setSummary(account.getUserName());
        mEmailPreference.setSummary(account.getEmail());
        mWebAddressPreference.setSummary(account.getWebAddress());
        changePrimaryBlogPreference(account.getPrimarySiteId());
        checkIfEmailChangeIsPending();
        checkIfUsernameCanBeChanged();
    }

    private void checkIfEmailChangeIsPending() {
        AccountModel account = mAccountStore.getAccount();
        if (account.getPendingEmailChange()) {
            showPendingEmailChangeSnackbar(account.getNewEmail());
        } else if (mEmailSnackbar != null && mEmailSnackbar.isShown()) {
            mEmailSnackbar.dismiss();
        }
        mEmailPreference.setEnabled(!account.getPendingEmailChange());
    }

    // BaseTransientBottomBar.LENGTH_LONG is pointing to Snackabr.LENGTH_LONG which confuses checkstyle
    @SuppressLint("WrongConstant")
    private void showPendingEmailChangeSnackbar(String newEmail) {
        if (getView() != null) {
            if (mEmailSnackbar == null || !mEmailSnackbar.isShown()) {
                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelPendingEmailChange();
                    }
                };

                mEmailSnackbar = Snackbar
                        .make(getView(), "", BaseTransientBottomBar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.button_discard), clickListener);
                TextView textView =
                        mEmailSnackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setMaxLines(4);
            }
            // instead of creating a new snackbar, update the current one to avoid the jumping animation
            mEmailSnackbar.setText(getString(R.string.pending_email_change_snackbar, newEmail));
            if (!mEmailSnackbar.isShown()) {
                mEmailSnackbar.show();
            }
        }
    }

    private void changePrimaryBlogPreference(long siteRemoteId) {
        mPrimarySitePreference.setValue(String.valueOf(siteRemoteId));
        SiteModel site = mSiteStore.getSiteBySiteId(siteRemoteId);
        if (site != null) {
            mPrimarySitePreference.setSummary(SiteUtils.getSiteNameOrHomeURL(site));
            mPrimarySitePreference.refreshAdapter();
        }
    }

    private void cancelPendingEmailChange() {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("user_email_change_pending", "false");
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        if (mEmailSnackbar != null && mEmailSnackbar.isShown()) {
            mEmailSnackbar.dismiss();
        }
    }

    private void updateEmail(String newEmail) {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("user_email", newEmail);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
    }

    private void updatePrimaryBlog(String blogId) {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("primary_site_ID", blogId);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
    }

    public void updateWebAddress(String newWebAddress) {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("user_URL", newWebAddress);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
    }

    public void updatePassword(String newPassword) {
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        payload.params = new HashMap<>();
        payload.params.put("password", newPassword);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!isAdded()) {
            return;
        }

        // When account change is caused by password change, progress dialog will be shown (i.e. not null).
        if (mChangePasswordProgressDialog != null) {
            showChangePasswordProgressDialog(false);

            if (event.isError()) {
                // We usually rely on event.error.type and provide our own localized message.
                // This case is exceptional because:
                // 1. The server-side error type is generic, but patching this server-side is quite involved
                // 2. We know the error string return from the server has decent localization
                String errorMessage = !TextUtils.isEmpty(event.error.message) ? event.error.message
                        : getString(R.string.error_post_account_settings);
                ToastUtils.showToast(getActivity(), errorMessage, ToastUtils.Duration.LONG);
                AppLog.e(T.SETTINGS, event.error.message);
            } else {
                ToastUtils.showToast(getActivity(), R.string.change_password_confirmation, ToastUtils.Duration.LONG);
                refreshAccountDetails();
            }
        } else {
            if (event.isError()) {
                switch (event.error.type) {
                    case SETTINGS_FETCH_GENERIC_ERROR:
                        ToastUtils.showToast(getActivity(), R.string.error_fetch_account_settings,
                                ToastUtils.Duration.LONG);
                        break;
                    case SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR:
                        ToastUtils.showToast(getActivity(), R.string.error_disabled_apis,
                                ToastUtils.Duration.LONG);
                        break;
                    case SETTINGS_POST_ERROR:
                        // We usually rely on event.error.type and provide our own localized message.
                        // This case is exceptional because:
                        // 1. The server-side error type is generic, but patching this server-side is quite involved
                        // 2. We know the error string return from the server has decent localization
                        String errorMessage = !TextUtils.isEmpty(event.error.message) ? event.error.message
                                : getString(R.string.error_post_account_settings);
                        ToastUtils.showToast(getActivity(), errorMessage, ToastUtils.Duration.LONG);
                        // we optimistically show the email change snackbar, if that request fails, we should
                        // remove the snackbar
                        checkIfEmailChangeIsPending();
                        break;
                }
            } else {
                refreshAccountDetails();
            }
        }
    }

    /**
     * If the username can be changed then the control can be clicked to open to the
     * Username Changer screen.
     */
    private void checkIfUsernameCanBeChanged() {
        AccountModel account = mAccountStore.getAccount();
        mUsernamePreference.setEnabled(account.getUsernameCanBeChanged());
        mUsernamePreference.setOnPreferenceClickListener(preference -> {
            showUsernameChangerFragment();
            return true;
        });
    }

    private void showUsernameChangerFragment() {
        AccountModel account = mAccountStore.getAccount();

        final Bundle bundle =
                SettingsUsernameChangerFragment.newBundle(account.getDisplayName(), account.getUserName());

        new FullScreenDialogFragment.Builder(getActivity())
                .setTitle(R.string.username_changer_title)
                .setAction(R.string.username_changer_action)
                .setOnConfirmListener(this)
                .setHideActivityBar(true)
                .setOnDismissListener(null)
                .setContent(SettingsUsernameChangerFragment.class, bundle)
                .build()
                .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(), FullScreenDialogFragment.TAG);
    }

    @Override public void onConfirm(@Nullable Bundle result) {
        if (result != null) {
            String username = result.getString(BaseUsernameChangerFullScreenDialogFragment.RESULT_USERNAME);

            if (username != null) {
                WPSnackbar.make(getView(),
                        String.format(getString(R.string.settings_username_changer_toast_content), username),
                        Snackbar.LENGTH_LONG).show();
                mUsernamePreference.setSummary(username);
            }
        }
    }

    public static String[] getSiteNamesFromSites(List<SiteModel> sites) {
        List<String> blogNames = new ArrayList<>();
        for (SiteModel site : sites) {
            blogNames.add(SiteUtils.getSiteNameOrHomeURL(site));
        }
        return blogNames.toArray(new String[blogNames.size()]);
    }

    public static String[] getHomeURLOrHostNamesFromSites(List<SiteModel> sites) {
        List<String> urls = new ArrayList<>();
        for (SiteModel site : sites) {
            urls.add(SiteUtils.getHomeURLOrHostName(site));
        }
        return urls.toArray(new String[urls.size()]);
    }

    public static String[] getSiteIdsFromSites(List<SiteModel> sites) {
        List<String> ids = new ArrayList<>();
        for (SiteModel site : sites) {
            ids.add(String.valueOf(site.getSiteId()));
        }
        return ids.toArray(new String[ids.size()]);
    }

    /*
     * AsyncTask which loads sites from database for primary site preference
     */
    private class LoadSitesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<SiteModel> sites = mSiteStore.getSitesAccessedViaWPComRest();
            mPrimarySitePreference.setEntries(getSiteNamesFromSites(sites));
            mPrimarySitePreference.setEntryValues(getSiteIdsFromSites(sites));
            mPrimarySitePreference.setDetails(getHomeURLOrHostNamesFromSites(sites));
            return null;
        }

        @Override
        protected void onPostExecute(Void results) {
            super.onPostExecute(results);
            mPrimarySitePreference.refreshAdapter();
        }
    }
}
