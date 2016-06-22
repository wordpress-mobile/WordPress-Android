package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.helpers.CreateUserAndBlog;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListAbstract.Callback;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListWPCom;
import org.wordpress.android.ui.accounts.helpers.LoginAbstract;
import org.wordpress.android.ui.accounts.helpers.LoginWPCom;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.util.AlertUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.UserEmailUtils;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker2.EmailChecker;
import org.wordpress.persistentedittext.PersistentEditTextHelper;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String mPassword;

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

    protected boolean isUserDataValid() {
        // try to create the user
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        final String password = EditTextUtils.getText(mPasswordTextField).trim();
        final String username = EditTextUtils.getText(mUsernameTextField).trim();
        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        boolean retValue = true;

        if (email.equals("")) {
            showEmailError(R.string.required_field);
            retValue = false;
        }

        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find() || email.length() > 100) {
            showEmailError(R.string.invalid_email_message);
            retValue = false;
        }

        if (username.equals("")) {
            showUsernameError(R.string.required_field);
            retValue = false;
        }

        if (username.length() < 4) {
            showUsernameError(R.string.invalid_username_too_short);
            retValue = false;
        }

        if (username.length() > 60) {
            showUsernameError(R.string.invalid_username_too_long);
            retValue = false;
        }

        if (username.contains(" ")) {
            showUsernameError(R.string.invalid_username_no_spaces);
            retValue = false;
        }

        if (siteUrl.contains(" ")) {
            showSiteUrlError(R.string.blog_name_no_spaced_allowed);
            retValue = false;
        }

        if (siteUrl.length() < 4) {
            showSiteUrlError(R.string.blog_name_must_be_at_least_four_characters);
            retValue = false;
        }

        if (password.equals("")) {
            showPasswordError(R.string.required_field);
            retValue = false;
        }

        if (password.length() < 4) {
            showPasswordError(R.string.invalid_password_message);
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

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case USERNAME:
                showUsernameError(messageId);
                return true;
            case PASSWORD:
                showPasswordError(messageId);
                return true;
            case EMAIL:
                showEmailError(messageId);
                return true;
            case SITE_URL:
                showSiteUrlError(messageId);
                return true;
        }
        return false;
    }

    private void showPasswordError(int messageId) {
        mPasswordTextField.setError(getString(messageId));
        mPasswordTextField.requestFocus();
    }

    private void showEmailError(int messageId) {
        mEmailTextField.setError(getString(messageId));
        mEmailTextField.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameTextField.setError(getString(messageId));
        mUsernameTextField.requestFocus();
    }

    private void showSiteUrlError(int messageId) {
        mSiteUrlTextField.setError(getString(messageId));
        mSiteUrlTextField.requestFocus();
    }

    private void validateAndCreateUserAndBlog() {
        if (mSystemService.getActiveNetworkInfo() == null) {
            AlertUtils.showAlert(getActivity(), R.string.no_network_title, R.string.no_network_message);
            return;
        }
        if (!isUserDataValid()) {
            return;
        }

        // Prevent double tapping of the "done" btn in keyboard for those clients that don't dismiss the keyboard.
        // Samsung S4 for example
        if (View.VISIBLE == mProgressBarSignIn.getVisibility()) {
            return;
        }

        startProgress(getString(R.string.validating_user_data));

        final String siteUrl = EditTextUtils.getText(mSiteUrlTextField).trim();
        final String email = EditTextUtils.getText(mEmailTextField).trim();
        mUsername = EditTextUtils.getText(mUsernameTextField).trim();
        mPassword = EditTextUtils.getText(mPasswordTextField).trim();
        final String siteName = siteUrlToSiteName(siteUrl);
        final String language = CreateUserAndBlog.getDeviceLanguage(getActivity());

        CreateUserAndBlog createUserAndBlog = new CreateUserAndBlog(email, mUsername, mPassword,
                siteUrl, siteName, language, getRestClientUtils(), getActivity(), new ErrorListener(),
                new CreateUserAndBlog.Callback() {
                    @Override
                    public void onStepFinished(CreateUserAndBlog.Step step) {
                        if (!isAdded()) {
                            return;
                        }
                        switch (step) {
                            case VALIDATE_USER:
                                updateProgress(getString(R.string.validating_site_data));
                                break;
                            case VALIDATE_SITE:
                                updateProgress(getString(R.string.creating_your_account));
                                break;
                            case CREATE_USER:
                                updateProgress(getString(R.string.creating_your_site));
                                break;
                            case CREATE_SITE:
                                // no messages
                            case AUTHENTICATE_USER:
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onSuccess(JSONObject createSiteResponse) {
                        // User has been created. From this point, all errors should close this screen and display the
                        // sign in screen
                        AnalyticsUtils.refreshMetadata(mUsername, email);
                        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_ACCOUNT);
                        // Save credentials to smart lock
                        SmartLockHelper smartLockHelper = getSmartLockHelper();
                        if (smartLockHelper != null) {
                            smartLockHelper.saveCredentialsInSmartLock(mUsername, mPassword, mUsername, null);
                        }
                        if (isAdded()) {
                            signInAndFetchBlogListWPCom();
                        }
                    }

                    @Override
                    public void onError(int messageId) {
                        endProgress();
                        if (isAdded()) {
                            showError(getString(messageId));
                        }
                    }
                });
        AppLog.i(T.NUX, "User tries to create a new account, username: " + mUsername + ", email: " + email
                + ", site name: " + siteName + ", site URL: " + siteUrl);
        createUserAndBlog.startCreateUserAndBlogProcess();
    }

    private void signInAndFetchBlogListWPCom() {
        updateProgress(getString(R.string.signing_in));
        LoginWPCom login = new LoginWPCom(mUsername, mPassword, null, false, null);
        login.execute(new LoginAbstract.Callback() {
            @Override
            public void onSuccess() {
                FetchBlogListWPCom fetchBlogListWPCom = new FetchBlogListWPCom(getActivity());
                fetchBlogListWPCom.execute(mFetchBlogListCallback);
            }

            @Override
            public void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired,
                                boolean erroneousSslCertificate) {
                // Should not happen (excepted for a timeout), go back to the sign in screen
                finishAndShowSignInScreen();
            }
        });
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
     * Show the sign in screen with username and password prefilled, plus a toast message to explain what happened.
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
        getFragmentManager().popBackStack();
        ToastUtils.showToast(getActivity(), R.string.signup_succeed_signin_failed, Duration.LONG);
    }

    protected final Callback mFetchBlogListCallback = new Callback() {
        @Override
        public void onSuccess(final List<Map<String, Object>> userBlogList) {
            if (!isAdded()) {
                return;
            }
            if (userBlogList != null) {
                BlogUtils.addBlogs(userBlogList, mUsername);
            }

            // get reader tags so they're available as soon as the Reader is accessed - done for
            // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
            // uses the application context since the activity is finished immediately below
            ReaderUpdateService.startService(getActivity().getApplicationContext(),
                    EnumSet.of(UpdateTask.TAGS));
            finishCurrentActivity();
        }

        @Override
        public void onError(final int messageId, final boolean twoStepCodeRequired, final boolean httpAuthRequired,
                            final boolean erroneousSslCertificate, final String clientResponse) {
            // Should not happen (excepted for a timeout), go back to the sign in screen
            finishAndShowSignInScreen();
        }
    };

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
                startActivity(newAccountIntent);
            }
        });
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
                                                          Uri uri = Uri.parse(Constants.URL_TOS);
                                                          startActivity(new Intent(Intent.ACTION_VIEW, uri));
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
            public void afterTextChanged(Editable s) {
                BlogUtils.convertToLowercase(s);
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
            public void afterTextChanged(Editable s) {
                BlogUtils.convertToLowercase(s);
            }
        });
        mUsernameTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mAutoCompleteUrl = EditTextUtils.getText(mUsernameTextField)
                            .equals(EditTextUtils.getText(mSiteUrlTextField))
                            || EditTextUtils.isEmpty(mSiteUrlTextField);
                }
            }
        });

        mEmailTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
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
}
