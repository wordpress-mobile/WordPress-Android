package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.NewAccountPayload;
import org.wordpress.android.fluxc.store.AccountStore.NewUserErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnNewUserCreated;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.NewSitePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnNewSiteCreated;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.UserEmailUtils;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker2.EmailChecker;
import org.wordpress.persistentedittext.PersistentEditTextHelper;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class NewUserFragment extends AbstractFragment implements TextWatcher {
    public static final int NEW_USER = 1;
    private EditText mSiteUrlTextField;
    private EditText mEmailTextField;
    private EditText mPasswordTextField;
    private EditText mUsernameTextField;
    private WPTextView mSignupButton;
    private WPTextView mProgressTextSignIn;
    private RelativeLayout mProgressBarSignIn;
    private boolean mEmailAutoCorrected;
    private boolean mAutoCompleteUrl;
    private String mUsername;
    private String mEmail;
    private String mPassword;

    private NewSitePayload mNewSitePayload;
    private NewAccountPayload mNewAccountPayload;

    protected boolean mSitesFetched = false;
    protected boolean mAccountSettingsFetched = false;
    protected boolean mAccountFetched = false;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;

    public static NewUserFragment newInstance() {
        return new NewUserFragment();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        checkIfFieldsFilled();
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mEmailTextField).trim().length() > 0
                && EditTextUtils.getText(mPasswordTextField).trim().length() > 0
                && EditTextUtils.getText(mUsernameTextField).trim().length() > 0
                && EditTextUtils.getText(mSiteUrlTextField).trim().length() > 0;
    }

    protected void startProgress(String message) {
        mProgressBarSignIn.setVisibility(View.VISIBLE);
        mProgressTextSignIn.setVisibility(View.VISIBLE);
        mSignupButton.setVisibility(View.GONE);
        mProgressBarSignIn.setEnabled(false);
        mProgressTextSignIn.setText(message);
        mEmailTextField.setEnabled(false);
        mPasswordTextField.setEnabled(false);
        mUsernameTextField.setEnabled(false);
        mSiteUrlTextField.setEnabled(false);
    }

    protected void updateProgress(String message) {
        mProgressTextSignIn.setText(message);
    }

    protected void endProgress() {
        mProgressBarSignIn.setVisibility(View.GONE);
        mProgressTextSignIn.setVisibility(View.GONE);
        mSignupButton.setVisibility(View.VISIBLE);
        mEmailTextField.setEnabled(true);
        mPasswordTextField.setEnabled(true);
        mUsernameTextField.setEnabled(true);
        mSiteUrlTextField.setEnabled(true);
    }

    protected void clearErrors() {
        mEmailTextField.setError(null);
        mUsernameTextField.setError(null);
        mPasswordTextField.setError(null);
        mSiteUrlTextField.setError(null);
    }

    protected boolean userDataValidEh() {
        // try to create the user
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        final String password = EditTextUtils.getText(mPasswordTextField).trim();
        final String username = EditTextUtils.getText(mUsernameTextField).trim();
        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        boolean retValue = true;

        if (email.equals("")) {
            showEmailError(getString(R.string.required_field));
            retValue = false;
        }

        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find() || email.length() > 100) {
            showEmailError(getString(R.string.invalid_email_message));
            retValue = false;
        }

        if (username.equals("")) {
            showUsernameError(getString(R.string.required_field));
            retValue = false;
        }

        if (username.length() < 4) {
            showUsernameError(getString(R.string.invalid_username_too_short));
            retValue = false;
        }

        if (username.length() > 60) {
            showUsernameError(getString(R.string.invalid_username_too_long));
            retValue = false;
        }

        if (username.contains(" ")) {
            showUsernameError(getString(R.string.invalid_username_no_spaces));
            retValue = false;
        }

        if (siteUrl.contains(" ")) {
            showSiteUrlError(getString(R.string.blog_name_no_spaced_allowed));
            retValue = false;
        }

        if (siteUrl.length() < 4) {
            showSiteUrlError(getString(R.string.blog_name_must_be_at_least_four_characters));
            retValue = false;
        }

        if (password.equals("")) {
            showPasswordError(getString(R.string.required_field));
            retValue = false;
        }

        if (password.length() < 4) {
            showPasswordError(getString(R.string.invalid_password_message));
            retValue = false;
        }

        return retValue;
    }

    protected void onDoneAction() {
        validateAndCreateUserAndBlog();
    }

    private final OnClickListener mSignupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndCreateUserAndBlog();
        }
    };

    private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };

    private String siteUrlToSiteName(String siteUrl) {
        return siteUrl;
    }

    private void showPasswordError(String message) {
        mPasswordTextField.setError(message);
        mPasswordTextField.requestFocus();
    }

    private void showEmailError(String message) {
        mEmailTextField.setError(message);
        mEmailTextField.requestFocus();
    }

    private void showUsernameError(String message) {
        mUsernameTextField.setError(message);
        mUsernameTextField.requestFocus();
    }

    private void showSiteUrlError(String message) {
        mSiteUrlTextField.setError(message);
        mSiteUrlTextField.requestFocus();
    }

    private void showSiteError(NewSiteErrorType newUserError, String message) {
        if (!isAdded()) {
            return;
        }
        switch (newUserError) {
            case SITE_NAME_REQUIRED:
                showSiteUrlError(getString(R.string.blog_name_required));
                break;
            case SITE_NAME_NOT_ALLOWED:
                showSiteUrlError(getString(R.string.blog_name_not_allowed));
                break;
            case SITE_NAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS:
                showSiteUrlError(getString(R.string.blog_name_must_be_at_least_four_characters));
                break;
            case SITE_NAME_MUST_BE_LESS_THAN_SIXTY_FOUR_CHARACTERS:
                showSiteUrlError(getString(R.string.blog_name_must_be_less_than_sixty_four_characters));
                break;
            case SITE_NAME_CONTAINS_INVALID_CHARACTERS:
                showSiteUrlError(getString(R.string.blog_name_contains_invalid_characters));
                break;
            case SITE_NAME_CANT_BE_USED:
                showSiteUrlError(getString(R.string.blog_name_cant_be_used));
                break;
            case SITE_NAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS:
                showSiteUrlError(getString(R.string.blog_name_only_lowercase_letters_and_numbers));
                break;
            case SITE_NAME_MUST_INCLUDE_LETTERS:
                showSiteUrlError(getString(R.string.blog_name_must_include_letters));
                break;
            case SITE_NAME_EXISTS:
                showSiteUrlError(getString(R.string.blog_name_exists));
                break;
            case SITE_NAME_RESERVED:
                showSiteUrlError(getString(R.string.blog_name_reserved));
                break;
            case SITE_NAME_RESERVED_BUT_MAY_BE_AVAILABLE:
                showSiteUrlError(getString(R.string.blog_name_reserved_but_may_be_available));
                break;
            case SITE_NAME_INVALID:
                showSiteUrlError(getString(R.string.blog_name_invalid));
                break;
            case SITE_TITLE_INVALID:
                showSiteUrlError(getString(R.string.blog_title_invalid));
                break;
            default:
            case GENERIC_ERROR:
                // Show other errors on the URL field
                showSiteUrlError(message);
                break;
        }
    }

    private void showUserError(NewUserErrorType newUserError, String message) {
        if (!isAdded()) {
            return;
        }
        switch (newUserError) {
            case USERNAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS:
                showUsernameError(getString(R.string.username_only_lowercase_letters_and_numbers));
                break;
            case USERNAME_REQUIRED:
                showUsernameError(getString(R.string.username_required));
                break;
            case USERNAME_NOT_ALLOWED:
                showUsernameError(getString(R.string.username_not_allowed));
                break;
            case USERNAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS:
                showUsernameError(getString(R.string.username_must_be_at_least_four_characters));
                break;
            case USERNAME_CONTAINS_INVALID_CHARACTERS:
                showUsernameError(getString(R.string.username_contains_invalid_characters));
                break;
            case USERNAME_MUST_INCLUDE_LETTERS:
                showUsernameError(getString(R.string.username_must_include_letters));
                break;
            case USERNAME_RESERVED_BUT_MAY_BE_AVAILABLE:
                showUsernameError(getString(R.string.username_reserved_but_may_be_available));
                break;
            case USERNAME_INVALID:
                showUsernameError(getString(R.string.username_invalid));
                break;
            case USERNAME_EXISTS:
                showUsernameError(getString(R.string.username_exists));
                AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_USERNAME_EXISTS);
                break;
            case PASSWORD_INVALID:
                showPasswordError(getString(R.string.password_invalid));
                break;
            case EMAIL_CANT_BE_USED_TO_SIGNUP:
                showEmailError(getString(R.string.email_cant_be_used_to_signup));
                break;
            case EMAIL_INVALID:
                showEmailError(getString(R.string.email_invalid));
                break;
            case EMAIL_NOT_ALLOWED:
                showEmailError(getString(R.string.email_not_allowed));
                break;
            case EMAIL_RESERVED:
                showEmailError(getString(R.string.email_reserved));
                break;
            case EMAIL_EXISTS:
                showEmailError(getString(R.string.email_exists));
                AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_EMAIL_EXISTS);
                break;
            default:
            case GENERIC_ERROR:
                // Show other errors on the first field (email)
                showEmailError(message);
                break;
        }
    }


    private void validateAndCreateUserAndBlog() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtils.showAlert(getActivity(), R.string.no_network_title, R.string.no_network_message);
            return;
        }
        if (!userDataValidEh()) {
            return;
        }

        // Prevent double tapping of the "done" btn in keyboard for those clients that don't dismiss the keyboard.
        // Samsung S4 for example
        if (View.VISIBLE == mProgressBarSignIn.getVisibility()) {
            return;
        }

        startProgress(getString(R.string.validating_user_data));
        clearErrors();

        mSitesFetched = false;
        mAccountSettingsFetched = false;
        mAccountFetched = false;

        String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        mEmail = EditTextUtils.getText(mEmailTextField).trim();
        mUsername = EditTextUtils.getText(mUsernameTextField).trim();
        mPassword = EditTextUtils.getText(mPasswordTextField).trim();

        String siteTitle = siteUrlToSiteName(siteUrl);
        String language = LanguageUtils.getPatchedCurrentDeviceLanguage(getActivity());

        mNewAccountPayload = new NewAccountPayload(mUsername, mPassword, mEmail, true);
        mNewSitePayload = new NewSitePayload(siteUrl, siteTitle, language, SiteVisibility.PUBLIC, true);

        mDispatcher.dispatch(AccountActionBuilder.newCreateNewAccountAction(mNewAccountPayload));
        updateProgress(getString(R.string.validating_site_data));

        AppLog.i(T.NUX, "User tries to create a new account, username: " + mUsername + ", email: " + mEmail
                + ", site title: " + siteTitle + ", site URL: " + siteUrl);
    }

    private void finishCurrentActivity() {
        if (!isAdded()) {
            return;
        }
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
        PersistentEditTextHelper persistentEditTextHelper = new PersistentEditTextHelper(getActivity());
        persistentEditTextHelper.clearSavedText(mEmailTextField, null);
        persistentEditTextHelper.clearSavedText(mUsernameTextField, null);
        persistentEditTextHelper.clearSavedText(mSiteUrlTextField, null);
    }

    /**
     * In case an error happened after the user creation steps, we don't want to show the sign up screen.
     * Show the log in screen with username and password prefilled, plus a toast message to explain what happened.
     *
     * Note: this should be called only if the user has been created.
     */
    private void finishAndShowSignInScreen() {
        if (!isAdded()) {
            return;
        }
        endProgress();
        Intent intent = new Intent();
        intent.putExtra("username", mUsername);
        intent.putExtra("password", mPassword);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        try {
            getFragmentManager().popBackStack();
        } catch (IllegalStateException e) {
            // Catch the ISE exception, because we can't check for the fragment state here
            // finishAndShowSignInScreen will be called in an Network onError callback so we can't guarantee, the
            // fragment transaction will be executed. In that case the user already is back on the Log In screen.
            AppLog.e(T.NUX, e);
        }
        ToastUtils.showToast(getActivity(), R.string.signup_succeed_signin_failed, Duration.LONG);
    }

    private void autocorrectEmail() {
        if (mEmailAutoCorrected) {
            return;
        }
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        String suggest = EmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mEmailTextField.setText(suggest);
            mEmailTextField.setSelection(suggest.length());
        }
    }

    private void initInfoButton(View rootView) {
        ImageView infoBUtton = (ImageView) rootView.findViewById(R.id.info_button);
        infoBUtton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newAccountIntent = new Intent(getActivity(), HelpActivity.class);
                newAccountIntent.putExtra(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.Tag.ORIGIN_SIGNUP_SCREEN);
                startActivity(newAccountIntent);
            }
        });
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.new_account_user_fragment_screen, container, false);

        WPTextView termsOfServiceTextView = (WPTextView) rootView.findViewById(R.id.l_agree_terms_of_service);
        termsOfServiceTextView.setText(Html.fromHtml(String.format(getString(R.string.agree_terms_of_service), "<u>",
                "</u>")));
        termsOfServiceTextView.setOnClickListener(new OnClickListener() {
                                                      @Override
                                                      public void onClick(View v) {
                                                          ActivityLauncher.openUrlExternal(getContext(),
                                                                  getString(R.string.wordpresscom_tos_url));
                                                      }
                                                  }
        );

        mSignupButton = (WPTextView) rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(mSignupClickListener);
        mSignupButton.setEnabled(false);

        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);

        mEmailTextField = (EditText) rootView.findViewById(R.id.email_address);
        mEmailTextField.setText(UserEmailUtils.getPrimaryEmail(getActivity()));
        mEmailTextField.setSelection(EditTextUtils.getText(mEmailTextField).length());
        mPasswordTextField = (EditText) rootView.findViewById(R.id.password);
        mUsernameTextField = (EditText) rootView.findViewById(R.id.username);
        mSiteUrlTextField = (EditText) rootView.findViewById(R.id.site_url);

        mEmailTextField.addTextChangedListener(this);
        mPasswordTextField.addTextChangedListener(this);
        mUsernameTextField.addTextChangedListener(this);
        mSiteUrlTextField.setOnKeyListener(mSiteUrlKeyListener);
        mSiteUrlTextField.setOnEditorActionListener(mEditorAction);

        mSiteUrlTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkIfFieldsFilled();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                lowerCaseEditable(editable);
            }
        });

        mUsernameTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // auto fill blog address
                mSiteUrlTextField.setError(null);
                if (mAutoCompleteUrl) {
                    mSiteUrlTextField.setText(EditTextUtils.getText(mUsernameTextField));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                lowerCaseEditable(editable);
            }
        });
        mUsernameTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean focusEh) {
                if (focusEh) {
                    mAutoCompleteUrl = EditTextUtils.getText(mUsernameTextField)
                            .equals(EditTextUtils.getText(mSiteUrlTextField))
                            || EditTextUtils.isEmpty(mSiteUrlTextField);
                }
            }
        });

        mEmailTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean focusEh) {
                if (!focusEh) {
                    autocorrectEmail();
                }
            }
        });
        initPasswordVisibilityButton(rootView, mPasswordTextField);
        initInfoButton(rootView);
        return rootView;
    }

    private void checkIfFieldsFilled() {
        if (fieldsFilled()) {
            mSignupButton.setEnabled(true);
        } else {
            mSignupButton.setEnabled(false);
        }
    }

    private final OnKeyListener mSiteUrlKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            mAutoCompleteUrl = EditTextUtils.isEmpty(mSiteUrlTextField);
            return false;
        }
    };

    private SmartLockHelper getSmartLockHelper() {
        if (getActivity() != null && getActivity() instanceof SignInActivity) {
            return ((SignInActivity) getActivity()).getSmartLockHelper();
        }
        return null;
    }

    private void fetchSiteAndAccount() {
        // User has been created. From this point, all errors should close this screen and display the
        // sign in screen
        AnalyticsUtils.refreshMetadataNewUser(mUsername, mEmail);
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_ACCOUNT);
        // Save credentials to smart lock
        SmartLockHelper smartLockHelper = getSmartLockHelper();
        if (smartLockHelper != null) {
            smartLockHelper.saveCredentialsInSmartLock(mUsername, mPassword, mUsername, null);
        }
        // Fetch user infos
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        // Fetch sites
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        AppLog.i(T.NUX, event.toString());
        if (event.errorEh()) {
            endProgress();
            finishAndShowSignInScreen();
            return;
        }
        if (mAccountStore.hasAccessToken()) {
            // Account created and user authenticated, now create the site
            updateProgress(getString(R.string.creating_your_site));
            mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(mNewSitePayload));

            // Get reader tags so they're available as soon as the Reader is accessed - done for
            // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
            // uses the application context since the activity is finished immediately below
            ReaderUpdateService.startService(getActivity().getApplicationContext(),
                    EnumSet.of(UpdateTask.TAGS));

            // Start the notification service
            NotificationsUpdateService.startService(getActivity().getApplicationContext());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewUserCreated(OnNewUserCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.errorEh()) {
            endProgress();
            AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_FAILED);
            showUserError(event.error.type, event.error.message);
            return;
        }
        if (event.dryRun) {
            // User Validated, now try to validate site creation
            mDispatcher.dispatch(SiteActionBuilder.newCreateNewSiteAction(mNewSitePayload));
            updateProgress(getString(R.string.validating_site_data));
            return;
        }
        // User created, now authenticate the newly created user
        AuthenticatePayload payload = new AuthenticatePayload(mNewAccountPayload.username, mNewAccountPayload.password);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewSiteCreated(OnNewSiteCreated event) {
        AppLog.i(T.NUX, event.toString());
        if (event.errorEh()) {
            endProgress();
            AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_FAILED);
            showSiteError(event.error.type, event.error.message);
            return;
        }
        if (event.dryRun) {
            // User and Site validated, dispatch the same actions with dryRun disabled
            updateProgress(getString(R.string.creating_your_account));
            mNewSitePayload.dryRun = false;
            mNewAccountPayload.dryRun = false;
            mDispatcher.dispatch(AccountActionBuilder.newCreateNewAccountAction(mNewAccountPayload));
            return;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_SITE);
        // Site created, time to wrap up
        fetchSiteAndAccount();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        AppLog.i(T.NUX, event.toString());
        mAccountSettingsFetched |= event.causeOfChange == AccountAction.FETCH_SETTINGS;
        mAccountFetched |= event.causeOfChange == AccountAction.FETCH_ACCOUNT;
        // Finish activity if sites have been fetched
        if (mSitesFetched && mAccountSettingsFetched && mAccountFetched) {
            finishCurrentActivity();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.NUX, event.toString());
        mSitesFetched = true;
        // Finish activity if account settings have been fetched
        if (mAccountSettingsFetched && mAccountFetched) {
            finishCurrentActivity();
        }
    }
}
