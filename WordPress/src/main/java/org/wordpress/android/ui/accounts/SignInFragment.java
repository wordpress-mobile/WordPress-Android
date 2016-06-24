package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.credentials.Credential;
import com.optimizely.Optimizely;
import com.optimizely.Variable.LiveVariable;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListAbstract.Callback;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListWPCom;
import org.wordpress.android.ui.accounts.helpers.FetchBlogListWPOrg;
import org.wordpress.android.ui.accounts.helpers.LoginAbstract;
import org.wordpress.android.ui.accounts.helpers.LoginWPCom;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GenericCallback;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker2.EmailChecker;
import org.xmlrpc.android.ApiHelper;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignInFragment extends AbstractFragment implements TextWatcher {
    public static final String TAG = "sign_in_fragment_tag";
    private static LiveVariable<Boolean> isNotOnWordPressComVariable = Optimizely.booleanForKey("isNotOnWordPressCom", false);
    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;
    private static final String KEY_IS_SELF_HOSTED = "IS_SELF_HOSTED";

    public static final String ENTERED_URL_KEY = "ENTERED_URL_KEY";
    public static final String ENTERED_USERNAME_KEY = "ENTERED_USERNAME_KEY";

    protected EditText mUsernameEditText;
    protected EditText mPasswordEditText;
    protected EditText mUrlEditText;
    protected EditText mTwoStepEditText;

    protected LinearLayout mBottomButtonsLayout;
    protected RelativeLayout mUsernameLayout;
    protected RelativeLayout mPasswordLayout;
    protected RelativeLayout mProgressBarSignIn;
    protected RelativeLayout mUrlButtonLayout;
    protected RelativeLayout mTwoStepLayout;
    protected LinearLayout mTwoStepFooter;

    protected boolean mSelfHosted;
    protected boolean mEmailAutoCorrected;
    protected boolean mShouldSendTwoStepSMS;
    protected int mErroneousLogInCount;
    protected String mUsername;
    protected String mPassword;
    protected String mTwoStepCode;
    protected String mHttpUsername;
    protected String mHttpPassword;
    protected Blog mJetpackBlog;

    protected WPTextView mSignInButton;
    protected WPTextView mCreateAccountButton;
    protected WPTextView mAddSelfHostedButton;
    protected WPTextView mProgressTextSignIn;
    protected WPTextView mForgotPassword;
    protected WPTextView mJetpackAuthLabel;
    protected ImageView mInfoButton;
    protected ImageView mInfoButtonSecondary;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSelfHosted = savedInstanceState.getBoolean(KEY_IS_SELF_HOSTED);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.signin_fragment, container, false);
        mUrlButtonLayout = (RelativeLayout) rootView.findViewById(R.id.url_button_layout);
        mTwoStepLayout = (RelativeLayout) rootView.findViewById(R.id.two_factor_layout);
        mTwoStepFooter = (LinearLayout) rootView.findViewById(R.id.two_step_footer);
        mUsernameLayout = (RelativeLayout) rootView.findViewById(R.id.nux_username_layout);
        mUsernameLayout.setOnClickListener(mOnLoginFormClickListener);
        mPasswordLayout = (RelativeLayout) rootView.findViewById(R.id.nux_password_layout);
        mPasswordLayout.setOnClickListener(mOnLoginFormClickListener);

        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mUsernameEditText.setOnClickListener(mOnLoginFormClickListener);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mPasswordEditText.setOnClickListener(mOnLoginFormClickListener);
        mJetpackAuthLabel = (WPTextView) rootView.findViewById(R.id.nux_jetpack_auth_label);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (WPTextView) rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);
        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mCreateAccountButton = (WPTextView) rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);
        mAddSelfHostedButton = (WPTextView) rootView.findViewById(R.id.nux_add_selfhosted_button);
        setDotComAddSelfHostedButtonText();
        mAddSelfHostedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSignInMode();
            }
        });

        mForgotPassword = (WPTextView) rootView.findViewById(R.id.forgot_password);
        mForgotPassword.setOnClickListener(mForgotPasswordListener);
        mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectUsername();
                }
            }
        });

        mPasswordEditText.setOnEditorActionListener(mEditorAction);
        mUrlEditText.setOnEditorActionListener(mEditorAction);

        mTwoStepEditText = (EditText) rootView.findViewById(R.id.nux_two_step);
        mTwoStepEditText.addTextChangedListener(this);
        mTwoStepEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (keyCode == EditorInfo.IME_ACTION_DONE)) {
                    if (fieldsFilled()) {
                        signIn();
                    }
                }

                return false;
            }
        });

        WPTextView twoStepFooterButton = (WPTextView) rootView.findViewById(R.id.two_step_footer_button);
        twoStepFooterButton.setText(Html.fromHtml("<u>" + getString(R.string.two_step_footer_button) + "</u>"));
        twoStepFooterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSMSTwoStepCode();
            }
        });

        mBottomButtonsLayout = (LinearLayout) rootView.findViewById(R.id.nux_bottom_buttons);
        initPasswordVisibilityButton(rootView, mPasswordEditText);
        initInfoButtons(rootView);
        moveBottomButtons();

        if (mSelfHosted) {
            showSelfHostedSignInForm();
        }
        autofillFromBuildConfig();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Ensure two-step form is shown if needed
        if (!TextUtils.isEmpty(mTwoStepEditText.getText()) && mTwoStepLayout.getVisibility() == View.GONE) {
            setTwoStepAuthVisibility(true);
        }
    }

    private void setDotComAddSelfHostedButtonText() {
        if (isNotOnWordPressComVariable.get()) {
            mAddSelfHostedButton.setText(R.string.not_on_wordpress_com);
        } else {
            mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
        }
    }

    /**
     * Hide toggle button "add self hosted / sign in with WordPress.com" and show self hosted URL
     * edit box
     */
    public void forceSelfHostedMode(String prefillUrl) {
        mUrlButtonLayout.setVisibility(View.VISIBLE);
        mAddSelfHostedButton.setVisibility(View.GONE);
        mCreateAccountButton.setVisibility(View.GONE);
        mUrlEditText.setText(prefillUrl);
        mSelfHosted = true;
    }

    protected void toggleSignInMode(){
        if (mUrlButtonLayout.getVisibility() == View.VISIBLE) {
            showDotComSignInForm();
            mSelfHosted = false;
        } else {
            showSelfHostedSignInForm();
            mSelfHosted = true;
        }
    }

    protected void showDotComSignInForm(){
        mUrlButtonLayout.setVisibility(View.GONE);
        setDotComAddSelfHostedButtonText();
    }

    protected void showSelfHostedSignInForm(){
        mUrlButtonLayout.setVisibility(View.VISIBLE);
        mAddSelfHostedButton.setText(getString(R.string.nux_oops_not_selfhosted_blog));
    }

    protected void track(Stat stat, Map<String, Boolean> properties) {
        AnalyticsTracker.track(stat, properties);
    }

    protected void finishCurrentActivity(final List<Map<String, Object>> userBlogList) {
        if (!isAdded()) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (userBlogList != null) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
        });
    }

    private void initInfoButtons(View rootView) {
        OnClickListener infoButtonListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), HelpActivity.class);
                // Used to pass data to an eventual support service
                intent.putExtra(ENTERED_URL_KEY, EditTextUtils.getText(mUrlEditText));
                intent.putExtra(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
                intent.putExtra(HelpshiftHelper.ORIGIN_KEY, Tag.ORIGIN_LOGIN_SCREEN_HELP);
                startActivity(intent);
            }
        };
        mInfoButton = (ImageView) rootView.findViewById(R.id.info_button);
        mInfoButtonSecondary = (ImageView) rootView.findViewById(R.id.info_button_secondary);
        mInfoButton.setOnClickListener(infoButtonListener);
        mInfoButtonSecondary.setOnClickListener(infoButtonListener);
    }

    /*
     * autofill the username and password from BuildConfig/gradle.properties (developer feature,
     * only enabled for DEBUG releases)
     */
    private void autofillFromBuildConfig() {
        if (!BuildConfig.DEBUG) return;

        String userName = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_USERNAME");
        String password = (String) WordPress.getBuildConfigValue(getActivity().getApplication(),
                "DEBUG_DOTCOM_LOGIN_PASSWORD");
        if (!TextUtils.isEmpty(userName)) {
            mUsernameEditText.setText(userName);
            AppLog.d(T.NUX, "Autofilled username from build config");
        }
        if (!TextUtils.isEmpty(password)) {
            mPasswordEditText.setText(password);
            AppLog.d(T.NUX, "Autofilled password from build config");
        }
    }

    public boolean canAutofillUsernameAndPassword() {
        return EditTextUtils.getText(mUsernameEditText).isEmpty() && EditTextUtils.getText(mPasswordEditText).isEmpty();
    }

    public void onCredentialRetrieved(Credential credential) {
        AppLog.d(T.NUX, "Retrieved username from SmartLock: " + credential.getId());
        if (canAutofillUsernameAndPassword()) {
            track(Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED, null);
            mUsernameEditText.setText(credential.getId());
            mPasswordEditText.setText(credential.getPassword());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        moveBottomButtons();
    }

    private void setSecondaryButtonVisible(boolean visible) {
        mInfoButtonSecondary.setVisibility(visible ? View.VISIBLE : View.GONE);
        mInfoButton.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void moveBottomButtons() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mBottomButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
            if (getResources().getInteger(R.integer.isSW600DP) == 0) {
                setSecondaryButtonVisible(true);
            } else {
                setSecondaryButtonVisible(false);
            }
        } else {
            mBottomButtonsLayout.setOrientation(LinearLayout.VERTICAL);
            setSecondaryButtonVisible(false);
        }
    }

    private final OnClickListener mOnLoginFormClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // Don't change layout if we are performing a network operation
            if (mProgressBarSignIn.getVisibility() == View.VISIBLE) return;

            if (mTwoStepLayout.getVisibility() == View.VISIBLE) {
                setTwoStepAuthVisibility(false);
            }
        }
    };

    private void autocorrectUsername() {
        if (mEmailAutoCorrected) {
            return;
        }
        final String email = EditTextUtils.getText(mUsernameEditText).trim();
        // Check if the username looks like an email address
        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find()) {
            return;
        }
        // It looks like an email address, then try to correct it
        String suggest = EmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mUsernameEditText.setText(suggest);
            mUsernameEditText.setSelection(suggest.length());
        }
    }

    private boolean isWPComLogin() {
        String selfHostedUrl = EditTextUtils.getText(mUrlEditText).trim();
        return !mSelfHosted || TextUtils.isEmpty(selfHostedUrl) || WPUrlUtils.isWordPressCom(selfHostedUrl);
    }

    private boolean isJetpackAuth() {
        return mJetpackBlog != null;
    }

    // Set blog for Jetpack auth
    public void setBlogAndCustomMessageForJetpackAuth(Blog blog, String customAuthMessage) {
        mJetpackBlog = blog;
        if(customAuthMessage != null && mJetpackAuthLabel != null) {
            mJetpackAuthLabel.setText(customAuthMessage);
        }

        if (mAddSelfHostedButton != null) {
            mJetpackAuthLabel.setVisibility(View.VISIBLE);
            mAddSelfHostedButton.setVisibility(View.GONE);
            mCreateAccountButton.setVisibility(View.GONE);
            mUsernameEditText.setText("");
        }
    }

    private final View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            createUserFragment();
        }
    };

    private void createUserFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        NewUserFragment newUserFragment = NewUserFragment.newInstance();
        newUserFragment.setTargetFragment(this, NewUserFragment.NEW_USER);
        transaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        transaction.replace(R.id.fragment_container, newUserFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private String getForgotPasswordURL() {
        String baseUrl = DOT_COM_BASE_URL;
        if (!isWPComLogin()) {
            baseUrl = EditTextUtils.getText(mUrlEditText).trim();
            String lowerCaseBaseUrl = baseUrl.toLowerCase(Locale.getDefault());
            if (!lowerCaseBaseUrl.startsWith("https://") && !lowerCaseBaseUrl.startsWith("http://")) {
                baseUrl = "http://" + baseUrl;
            }
        }
        return baseUrl + FORGOT_PASSWORD_RELATIVE_URL;
    }

    private final View.OnClickListener mForgotPasswordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String forgotPasswordUrl = getForgotPasswordURL();
            AppLog.i(T.NUX, "User tapped forgot password link: " + forgotPasswordUrl);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(forgotPasswordUrl));
            startActivity(intent);
        }
    };

    protected void onDoneAction() {
        signIn();
    }

    private final TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (mPasswordEditText == v) {
                if (mSelfHosted) {
                    mUrlEditText.requestFocus();
                    return true;
                } else {
                    return onDoneEvent(actionId, event);
                }
            }
            return onDoneEvent(actionId, event);
        }
    };

    private void refreshAndSelectSite(Map<String, Object> site) {
        refreshBlogContent(site);
        WordPress.setCurrentBlog((Integer) site.get("id"));
    }

    private void setPrimaryBlog(JSONObject jsonObject) {
        try {
            String primarySiteId = jsonObject.getString("primary_blog");
            boolean hideJetpackWithoutCredentials = true;
            // Look for a visible site that is not a "non active" Jetpack site with this id in the DB
            // TODO: when we support Jetpack sites by wpcom login, we should change that
            List<Map<String, Object>> sites = WordPress.wpDB.getBlogsBy("isHidden = 0 AND blogId = " + primarySiteId,
                    null, 1, hideJetpackWithoutCredentials);
            if (sites != null && !sites.isEmpty()) {
                refreshAndSelectSite(sites.get(0));
            } else {
                // Primary blog not found or hidden (can happen if it's a "non active" Jetpack site)
                // Select the first visible site if it exists
                sites = WordPress.wpDB.getBlogsBy("isHidden = 0", null, 1, hideJetpackWithoutCredentials);
                if (sites != null && !sites.isEmpty()) {
                    refreshAndSelectSite(sites.get(0));
                }
            }
        } catch (JSONException e) {
            AppLog.e(T.NUX, e);
        }
    }

    private void trackAnalyticsSignIn() {
        AnalyticsUtils.refreshMetadata();
        Map<String, Boolean> properties = new HashMap<String, Boolean>();
        properties.put("dotcom_user", isWPComLogin());
        track(Stat.SIGNED_IN, properties);
        if (!isWPComLogin()) {
            track(Stat.ADDED_SELF_HOSTED_SITE, null);
        }
    }

    private SmartLockHelper getSmartLockHelper() {
        if (getActivity() != null && getActivity() instanceof SignInActivity) {
            return ((SignInActivity) getActivity()).getSmartLockHelper();
        }
        return null;
    }

    protected final Callback mFetchBlogListCallback = new Callback() {
        @Override
        public void onSuccess(final List<Map<String, Object>> userBlogList) {
            if (!isAdded()) {
                return;
            }
            if (userBlogList != null) {
                if (isWPComLogin()) {
                    BlogUtils.addBlogs(userBlogList, mUsername);
                } else {
                    BlogUtils.addBlogs(userBlogList, mUsername, mPassword, mHttpUsername, mHttpPassword);
                }

                // refresh the first 5 blogs
                refreshFirstFiveBlogsContent();
            }

            trackAnalyticsSignIn();
            Optimizely.trackEvent("Signed In");

            // get reader tags so they're available as soon as the Reader is accessed - done for
            // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
            // uses the application context since the activity is finished immediately below
            ReaderUpdateService.startService(getActivity().getApplicationContext(),
                    EnumSet.of(UpdateTask.TAGS));

            if (isWPComLogin()) {
                //Update previous stats widgets
                StatsWidgetProvider.updateWidgetsOnLogin(getActivity().getApplicationContext());

                // Fire off a synchronous request to get the primary blog
                WordPress.getRestClientUtils().get("me", new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        // Set primary blog
                        setPrimaryBlog(jsonObject);
                        finishCurrentActivity(userBlogList);
                        String displayName = JSONUtils.getStringDecoded(jsonObject, "display_name");
                        Uri profilePicture = Uri.parse(JSONUtils.getString(jsonObject, "avatar_URL"));
                        SmartLockHelper smartLockHelper = getSmartLockHelper();
                        // mUsername and mPassword are null when the user sign in with a magic link
                        if (smartLockHelper != null && mUsername != null && mPassword != null) {
                            smartLockHelper.saveCredentialsInSmartLock(mUsername, mPassword, displayName,
                                    profilePicture);
                        }
                    }
                }, null);
            } else {
                finishCurrentActivity(userBlogList);
            }
        }

        @Override
        public void onError(final int messageId, final boolean twoStepCodeRequired, final boolean httpAuthRequired,
                            final boolean erroneousSslCertificate, final String clientResponse) {
            if (!isAdded()) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (twoStepCodeRequired) {
                        setTwoStepAuthVisibility(true);
                        endProgress();
                        return;
                    }

                    if (erroneousSslCertificate) {
                        askForSslTrust();
                        return;
                    }
                    if (httpAuthRequired) {
                        askForHttpAuthCredentials();
                        return;
                    }
                    if (messageId != 0) {
                        signInError(messageId, clientResponse);
                        return;
                    }

                    endProgress();
                }
            });

            track(Stat.LOGIN_FAILED, null);
        }
    };

    public void showAuthErrorMessage() {
        if (mJetpackAuthLabel != null) {
            mJetpackAuthLabel.setVisibility(View.VISIBLE);
            mJetpackAuthLabel.setText(getResources().getString(R.string.auth_required));
        }
    }

    private void setTwoStepAuthVisibility(boolean isVisible) {
        mTwoStepLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mTwoStepFooter.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mSignInButton.setText(isVisible ? getString(R.string.verify) : getString(R.string.sign_in));
        mForgotPassword.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        mBottomButtonsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        mUsernameEditText.setFocusableInTouchMode(!isVisible);
        mUsernameLayout.setAlpha(isVisible ? 0.6f : 1.0f);
        mPasswordEditText.setFocusableInTouchMode(!isVisible);
        mPasswordLayout.setAlpha(isVisible ? 0.6f : 1.0f);

        if (isVisible) {
            mTwoStepEditText.requestFocus();
            mTwoStepEditText.setText("");
            showSoftKeyboard();
        } else {
            mTwoStepEditText.setText("");
            mTwoStepEditText.clearFocus();
        }
    }

    private void showSoftKeyboard() {
        if (isAdded() && !hasHardwareKeyboard()) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private boolean hasHardwareKeyboard() {
        return (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS);
    }

    private void signInAndFetchBlogListWPCom() {
        LoginWPCom login = new LoginWPCom(mUsername, mPassword, mTwoStepCode, mShouldSendTwoStepSMS, mJetpackBlog);
        login.execute(new LoginAbstract.Callback() {
            @Override
            public void onSuccess() {
                configureAccountAfterSuccessfulSignIn();
            }

            @Override
            public void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate) {
                mFetchBlogListCallback.onError(errorMessageId, twoStepCodeRequired, httpAuthRequired, erroneousSslCertificate, "");
                mShouldSendTwoStepSMS = false;
                // Delete credentials only if login failed with an incorrect username/password error
                if (errorMessageId == R.string.username_or_password_incorrect) {
                    SmartLockHelper smartLockHelper = getSmartLockHelper();
                    if (smartLockHelper != null) {
                        smartLockHelper.deleteCredentialsInSmartLock(mUsername, mPassword);
                    }
                }
            }
        });
    }

    protected void configureAccountAfterSuccessfulSignIn() {
        mShouldSendTwoStepSMS = false;

        // Finish this activity if we've authenticated to a Jetpack site
        if (isJetpackAuth() && getActivity() != null) {
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
            return;
        }

        FetchBlogListWPCom fetchBlogListWPCom = new FetchBlogListWPCom(getActivity());
        fetchBlogListWPCom.execute(mFetchBlogListCallback);
    }

    private void signInAndFetchBlogListWPOrg() {
        String url = EditTextUtils.getText(mUrlEditText).trim();
        FetchBlogListWPOrg fetchBlogListWPOrg = new FetchBlogListWPOrg(mUsername, mPassword, url);
        if (mHttpUsername != null && mHttpPassword != null) {
            fetchBlogListWPOrg.setHttpCredentials(mHttpUsername, mHttpPassword);
        }
        fetchBlogListWPOrg.execute(mFetchBlogListCallback);
    }

    private boolean checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            SignInDialogFragment nuxAlert;
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.no_network_title),
                    getString(R.string.no_network_message),
                    R.drawable.noticon_alert_big,
                    getString(R.string.cancel));
            ft.add(nuxAlert, "alert");
            ft.commitAllowingStateLoss();
            return false;
        }
        return true;
    }

    protected void signIn() {
        if (!isUserDataValid()) {
            return;
        }

        if (!checkNetworkConnectivity()) {
            return;
        }

        mUsername = EditTextUtils.getText(mUsernameEditText).trim();
        mPassword = EditTextUtils.getText(mPasswordEditText).trim();
        mTwoStepCode = EditTextUtils.getText(mTwoStepEditText).trim();
        if (isWPComLogin()) {
            AppLog.i(T.NUX, "User tries to sign in on WordPress.com with username: " + mUsername);
            startProgress(getString(R.string.connecting_wpcom));
            signInAndFetchBlogListWPCom();
        } else {
            String selfHostedUrl = EditTextUtils.getText(mUrlEditText).trim();
            AppLog.i(T.NUX, "User tries to sign in on Self Hosted: " + selfHostedUrl + " with username: " + mUsername);
            startProgress(getString(R.string.signing_in));
            signInAndFetchBlogListWPOrg();
        }
    }

    private void requestSMSTwoStepCode() {
        if (!isAdded()) return;

        ToastUtils.showToast(getActivity(), R.string.two_step_sms_sent);
        mTwoStepEditText.setText("");
        mShouldSendTwoStepSMS = true;

        signIn();
    }

    private final OnClickListener mSignInClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            signIn();
        }
    };

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mSignInButton.setEnabled(true);
        } else {
            mSignInButton.setEnabled(false);
        }
        mPasswordEditText.setError(null);
        mUsernameEditText.setError(null);
        mTwoStepEditText.setError(null);
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mUsernameEditText).trim().length() > 0
               && (mPasswordLayout.getVisibility() == View.GONE || EditTextUtils.getText(mPasswordEditText).trim().length() > 0)
               && (mTwoStepLayout.getVisibility() == View.GONE || EditTextUtils.getText(mTwoStepEditText).trim().length() > 0);
    }

    protected boolean isUserDataValid() {
        final String username = EditTextUtils.getText(mUsernameEditText).trim();
        final String password = EditTextUtils.getText(mPasswordEditText).trim();
        boolean retValue = true;

        if (password.equals("")) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }

        if (username.equals("")) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            retValue = false;
        }

        return retValue;
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameEditText.setError(getString(messageId));
        mUsernameEditText.requestFocus();
    }

    private void showUrlError(int messageId) {
        mUrlEditText.setError(getString(messageId));
        mUrlEditText.requestFocus();
    }

    private void showTwoStepCodeError(int messageId) {
        mTwoStepEditText.setError(getString(messageId));
        mTwoStepEditText.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case USERNAME:
            case PASSWORD:
                showPasswordError(messageId);
                showUsernameError(messageId);
                return true;
            default:
                return false;
        }
    }

    protected void startProgress(String message) {
        mProgressBarSignIn.setVisibility(View.VISIBLE);
        mProgressTextSignIn.setVisibility(View.VISIBLE);
        mSignInButton.setVisibility(View.GONE);
        mProgressBarSignIn.setEnabled(false);
        mProgressTextSignIn.setText(message);
        mUsernameEditText.setEnabled(false);
        mPasswordEditText.setEnabled(false);
        mTwoStepEditText.setEnabled(false);
        mUrlEditText.setEnabled(false);
        mAddSelfHostedButton.setEnabled(false);
        mCreateAccountButton.setEnabled(false);
        mForgotPassword.setEnabled(false);
    }

    protected void endProgress() {
        mProgressBarSignIn.setVisibility(View.GONE);
        mProgressTextSignIn.setVisibility(View.GONE);
        mSignInButton.setVisibility(View.VISIBLE);
        mUsernameEditText.setEnabled(true);
        mPasswordEditText.setEnabled(true);
        mTwoStepEditText.setEnabled(true);
        mUrlEditText.setEnabled(true);
        mAddSelfHostedButton.setEnabled(true);
        mCreateAccountButton.setEnabled(true);
        mForgotPassword.setEnabled(true);
    }

    public void askForSslTrust() {
        SelfSignedSSLCertsManager.askForSslTrust(getActivity(), new GenericCallback<Void>() {
            @Override
            public void callback(Void aVoid) {
                // Try to signin again
                signIn();
            }
        });
        endProgress();
    }

    private void askForHttpAuthCredentials() {
        // Prompt for http credentials
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.http_authorization_required);

        View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.alert_http_auth, null);
        final EditText usernameEditText = (EditText) httpAuth.findViewById(R.id.http_username);
        final EditText passwordEditText = (EditText) httpAuth.findViewById(R.id.http_password);
        alert.setView(httpAuth);
        alert.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mHttpUsername = EditTextUtils.getText(usernameEditText);
                mHttpPassword = EditTextUtils.getText(passwordEditText);
                signIn();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
        endProgress();
    }

    protected void showInvalidUsernameOrPasswordDialog() {
        // Show a dialog
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;
        // create a 3 buttons dialog ("Contact us", "Forget your password?" and "Cancel")
        nuxAlert = SignInDialogFragment.newInstance(getString(org.wordpress.android.R.string.nux_cannot_log_in),
                getString(org.wordpress.android.R.string.username_or_password_incorrect),
                org.wordpress.android.R.drawable.noticon_alert_big, 3, getString(
                        org.wordpress.android.R.string.cancel), getString(
                        org.wordpress.android.R.string.forgot_password), getString(
                        org.wordpress.android.R.string.contact_us), SignInDialogFragment.ACTION_OPEN_URL,
                SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT);

        // Put entered url and entered username args, that could help our support team
        Bundle bundle = nuxAlert.getArguments();
        bundle.putString(SignInDialogFragment.ARG_OPEN_URL_PARAM, getForgotPasswordURL());
        bundle.putString(ENTERED_URL_KEY, EditTextUtils.getText(mUrlEditText));
        bundle.putString(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    protected void handleInvalidUsernameOrPassword(int messageId) {
        mErroneousLogInCount += 1;
        if (mErroneousLogInCount >= WPCOM_ERRONEOUS_LOGIN_THRESHOLD) {
            // Clear previous errors
            mPasswordEditText.setError(null);
            mUsernameEditText.setError(null);
            showInvalidUsernameOrPasswordDialog();
        } else {
            showPasswordError(messageId);
            showUsernameError(messageId);
        }
        endProgress();
    }

    protected void signInError(int messageId, String clientResponse) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;
        if (messageId == org.wordpress.android.R.string.username_or_password_incorrect) {
            handleInvalidUsernameOrPassword(messageId);
            return;
        } else if (messageId == R.string.invalid_verification_code) {
            endProgress();
            showTwoStepCodeError(messageId);
            return;
        } else if (messageId == org.wordpress.android.R.string.invalid_site_url_message) {
            showUrlError(messageId);
            endProgress();
            return;
        } else {
            AppLog.e(T.NUX, "Server response: " + clientResponse);
            nuxAlert = SignInDialogFragment.newInstance(getString(org.wordpress.android.R.string.nux_cannot_log_in),
                    getString(messageId), R.drawable.noticon_alert_big, 3,
                    getString(R.string.cancel), getString(R.string.contact_us), getString(R.string.reader_title_applog),
                    SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT,
                    SignInDialogFragment.ACTION_OPEN_APPLICATION_LOG);
        }
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
        endProgress();
    }

    private void refreshBlogContent(Map<String, Object> blogMap) {
        String blogId = blogMap.get("blogId").toString();
        String xmlRpcUrl = blogMap.get("url").toString();
        int intBlogId = StringUtils.stringToInt(blogId, -1);
        if (intBlogId == -1) {
            AppLog.e(T.NUX, "Can't refresh blog content - invalid blogId: " + blogId);
            return;
        }
        int blogLocalId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(intBlogId, xmlRpcUrl);
        Blog firstBlog = WordPress.wpDB.instantiateBlogByLocalId(blogLocalId);
        new ApiHelper.RefreshBlogContentTask(firstBlog, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
    }

    /**
     * Get the first five blogs and call RefreshBlogContentTask. First blog will be autoselected when user login.
     * Also when a user add a new self hosted blog, userBlogList contains only one element.
     * We don't want to refresh the whole list because it can be huge and each blog is refreshed when
     * user selects it.
     */
    private void refreshFirstFiveBlogsContent() {
        List<Map<String, Object>> visibleBlogs = WordPress.wpDB.getBlogsBy("isHidden = 0", null, 5, true);
        if (visibleBlogs != null && !visibleBlogs.isEmpty()) {
            int numberOfBlogsBeingRefreshed = Math.min(5, visibleBlogs.size());
            for (int i = 0; i < numberOfBlogsBeingRefreshed; i++) {
                Map<String, Object> currentBlog = visibleBlogs.get(i);
                refreshBlogContent(currentBlog);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_SELF_HOSTED, mSelfHosted);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Autofill username / password if string fields are set (only usefull after an error in sign up).
        // This can't be done in onCreateView
        if (mUsername != null) {
            mUsernameEditText.setText(mUsername);
        }
        if (mPassword != null) {
            mPasswordEditText.setText(mPassword);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NewUserFragment.NEW_USER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Text views will be populated by username/password if these fields are set
                mUsername = data.getStringExtra("username");
                mPassword = data.getStringExtra("password");
            }
        }
    }
}
