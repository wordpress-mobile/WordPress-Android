package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ViewSwitcher;

import com.google.android.gms.auth.api.credentials.Credential;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountRestClient.IsAvailable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.SelfSignedSSLUtils.Callback;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.widgets.ContextMenuEditText;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker2.EmailChecker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import static android.content.Context.CLIPBOARD_SERVICE;

public class SignInFragment extends AbstractFragment implements TextWatcher {
    public static final String TAG = "sign_in_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;
    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;
    private static final String KEY_IS_SELF_HOSTED = "IS_SELF_HOSTED";
    private static final Pattern DOT_COM_RESERVED_NAMES =
            Pattern.compile("^(?:admin|administrator|invite|main|root|web|www|[^@]*wordpress[^@]*)$");
    private static final Pattern TWO_STEP_AUTH_CODE = Pattern.compile("^[0-9]{6}");
    private static final Pattern WPCOM_DOMAIN = Pattern.compile("[a-z0-9]+\\.wordpress\\.com");

    public static final String ENTERED_URL_KEY = "ENTERED_URL_KEY";
    public static final String ENTERED_USERNAME_KEY = "ENTERED_USERNAME_KEY";

    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION = "10";
    private static final String XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID = "102";

    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION = "10";
    private static final String MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID = "11";

    private static final String NO_SITE_HELPSHIFT_FAQ_SECTION = "10";
    private static final String NO_SITE_HELPSHIFT_FAQ_ID = "2"; //using the same as in INVALID URL

    protected EditText mUsernameEditText;
    protected EditText mPasswordEditText;
    protected EditText mUrlEditText;
    protected ContextMenuEditText mTwoStepEditText;

    protected ViewSwitcher mIconSwitcher;
    protected View mWpcomLogotype;
    protected LinearLayout mBottomButtonsLayout;
    protected RelativeLayout mUsernameLayout;
    protected RelativeLayout mPasswordLayout;
    protected RelativeLayout mProgressBarSignIn;
    protected RelativeLayout mUrlButtonLayout;
    protected RelativeLayout mTwoStepLayout;
    protected LinearLayout mTwoStepFooter;

    protected boolean mSelfHosted;
    protected boolean mIsSelfHostedForced;
    protected boolean mEmailAutoCorrected;
    protected boolean mShouldSendTwoStepSMS;
    protected int mErroneousLogInCount;
    protected String mUsername;
    protected String mPassword;
    protected String mTwoStepCode;
    protected String mHttpUsername;
    protected String mHttpPassword;
    protected SiteModel mJetpackSite;

    protected WPTextView mSignInButton;
    protected WPTextView mCreateAccountButton;
    protected WPTextView mAddSelfHostedButton;
    protected WPTextView mProgressTextSignIn;
    protected WPTextView mForgotPassword;
    protected WPTextView mJetpackAuthLabel;
    protected ImageView mInfoButton;
    protected ImageView mInfoButtonSecondary;

    protected @Inject SiteStore mSiteStore;
    protected @Inject AccountStore mAccountStore;
    protected @Inject Dispatcher mDispatcher;
    protected @Inject HTTPAuthManager mHTTPAuthManager;
    protected @Inject MemorizingTrustManager mMemorizingTrustManager;

    private final Matcher mReservedNameMatcher = DOT_COM_RESERVED_NAMES.matcher("");
    private final Matcher mTwoStepAuthCodeMatcher = TWO_STEP_AUTH_CODE.matcher("");

    private OnMagicLinkRequestInteraction mListener;
    private String mToken = "";
    private boolean mSmartLockEnabled = true;
    private boolean mIsActivityFinishing;
    private boolean mInhibitMagicLogin; // Prevent showing magic links as that is only applicable for initial sign in
    private boolean mIsMagicLinksEnabled = true;

    private JetpackCallbacks mJetpackCallbacks;

    public interface OnMagicLinkRequestInteraction {
        void onMagicLinkRequestSuccess(String email);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mSelfHosted = savedInstanceState.getBoolean(KEY_IS_SELF_HOSTED);
        }

        mInhibitMagicLogin = getActivity() != null
                && (getActivity().getIntent().getBooleanExtra(SignInActivity.EXTRA_INHIBIT_MAGIC_LOGIN, false)
                || !WPActivityUtils.isEmailClientAvailable(getActivity()));

        AnalyticsTracker.track(Stat.LOGIN_ACCESSED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.signin_fragment, container, false);
        mIconSwitcher = (ViewSwitcher) rootView.findViewById(R.id.icon_switcher);
        mWpcomLogotype = rootView.findViewById(R.id.nux_wordpress_logotype);
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
        mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
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

                    // Show the self-hosted sign-in form if the user entered a username that can't be for WordPress.com
                    if (!mSelfHosted) {
                        mReservedNameMatcher.reset(mUsernameEditText.getText().toString());
                        if (mReservedNameMatcher.matches()) {
                            showSelfHostedSignInForm();
                        }
                    }
                }
            }
        });

        mPasswordEditText.setOnEditorActionListener(mEditorAction);
        mUrlEditText.setOnEditorActionListener(mEditorAction);

        mTwoStepEditText = (ContextMenuEditText) rootView.findViewById(R.id.nux_two_step);
        mTwoStepEditText.addTextChangedListener(this);
        mTwoStepEditText.setOnKeyListener(
            new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (keyCode == EditorInfo.IME_ACTION_DONE)) {
                        if (fieldsFilled()) {
                            signIn();
                        }
                    }

                    return false;
                }
            }
        );
        mTwoStepEditText.setOnContextMenuListener(
            new ContextMenuEditText.OnContextMenuListener() {
                @Override
                public void onCut() {
                }

                @Override
                public void onCopy() {
                }

                @Override
                public void onPaste() {
                    mTwoStepEditText.setText(getAuthCodeFromClipboard());

                    if (TextUtils.isEmpty(mTwoStepEditText.getText().toString())) {
                        showTwoStepCodeError(R.string.invalid_verification_code_format);
                    }
                }
            }
        );

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

        if (savedInstanceState == null) {
            configureMagicLinkUI();
        }
        mUsernameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((didPressNextKey(actionId, event) || didPressEnterKey(actionId, event)) && mIsMagicLinksEnabled) {
                    signIn();
                    return true;
                } else {
                    return false;
                }
            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SignInFragment.OnMagicLinkRequestInteraction) {
            mListener = (SignInFragment.OnMagicLinkRequestInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnMagicLinkRequestInteraction");
        }
        if (context instanceof JetpackCallbacks) {
            mJetpackCallbacks = (JetpackCallbacks) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement JetpackCallbacks");
        }
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
        // Insert authentication code if copied to clipboard
        } else if (TextUtils.isEmpty(mTwoStepEditText.getText()) && mTwoStepLayout.getVisibility() == View.VISIBLE) {
            mTwoStepEditText.setText(getAuthCodeFromClipboard());
        }

        if (!mToken.isEmpty() && !mInhibitMagicLogin) {
            mSmartLockEnabled = false;
            attemptLoginWithMagicLink();
        } else {
            mSmartLockEnabled = true;
        }
        if (!mIsMagicLinksEnabled) {
            showPasswordFieldAndFocus();
        } else if (mInhibitMagicLogin) {
            showPasswordField(false);
        }
    }

    /**
     * Hide toggle button "add self hosted / log in with WordPress.com" and show self hosted URL
     * edit box
     */
    public void forceSelfHostedMode(@NonNull String prefillUrl) {
        mUrlButtonLayout.setVisibility(View.VISIBLE);
        mPasswordLayout.setVisibility(View.VISIBLE);
        mAddSelfHostedButton.setVisibility(View.GONE);
        mCreateAccountButton.setVisibility(View.GONE);
        switchToDotOrgIcon(true);
        switchBackgroundToDotOrg(true, true);
        if (!prefillUrl.isEmpty()) {
            mUrlEditText.setText(prefillUrl);
        }
        mSelfHosted = true;
        mIsSelfHostedForced = true;
    }

    private void showPasswordFieldAndFocus() {
        showPasswordField(true);
    }

    private void showPasswordField(boolean doFocus) {
        if (isAdded()) {
            endProgress();
            showPasswordField();
            if (doFocus) {
                mPasswordEditText.requestFocus();
            }
            mSignInButton.setText(getString(R.string.sign_in));
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mPasswordEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void showPasswordField() {
        if (isAdded()) {
            mIsMagicLinksEnabled = false;
            mPasswordLayout.setVisibility(View.VISIBLE);
            mForgotPassword.setVisibility(View.VISIBLE);
            if (!mSelfHosted) {
                mPasswordEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                showWPComLogoType(true);
            }
            mSignInButton.setText(R.string.sign_in);
        }
    }

    protected void toggleSignInMode(){
        if (mUrlButtonLayout.getVisibility() == View.VISIBLE) {
            if (mInhibitMagicLogin) {
                showDotComSignInForm();
            } else {
                mIsMagicLinksEnabled = true;
                configureMagicLinkUI();
            }
        } else {
            showSelfHostedSignInForm();
        }
        if (fieldsFilled()) {
            mSignInButton.setEnabled(true);
        } else {
            mSignInButton.setEnabled(false);
        }
    }

    protected void showDotComSignInForm(){
        mUrlButtonLayout.setVisibility(View.GONE);
        mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
        switchToDotOrgIcon(false);
        switchBackgroundToDotOrg(false, false);
        showWPComLogoType(false);
    }

    protected void showSelfHostedSignInForm(){
        endProgress();
        mSelfHosted = true;
        mUrlButtonLayout.setVisibility(View.VISIBLE);
        mAddSelfHostedButton.setText(getString(R.string.nux_oops_not_selfhosted_blog));
        showPasswordField();
        switchToDotOrgIcon(true);
        switchBackgroundToDotOrg(true, false);
    }

    private void switchToDotOrgIcon(boolean showDotOrg) {
        if (mIconSwitcher.getDisplayedChild() == 0) {
            if (showDotOrg) {
                mIconSwitcher.showNext();
            }
            showWPComLogoType(false);
        } else {
            if (!showDotOrg) {
                mIconSwitcher.showPrevious();
            }
        }
    }

    private void showWPComLogoType(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        mWpcomLogotype.setVisibility(visibility);
    }

    private void switchBackgroundToDotOrg(boolean useDotOrg, boolean noFading) {
        if (getView() == null) {
            return;
        }

        TransitionDrawable transition = (TransitionDrawable) getView().getBackground();
        if (useDotOrg) {
            transition.startTransition(noFading ? 0 : 500);
        } else {
            transition.reverseTransition(noFading ? 0 : 500);
        }
    }

    public void setToken(String token) {
        mToken = token;
    }

    public void setIsMagicLinkEnabled(boolean isMagicLinksEnabled) {
        mIsMagicLinksEnabled = isMagicLinksEnabled;
    }

    private void initInfoButtons(View rootView) {
        OnClickListener infoButtonListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), HelpActivity.class);
                // Used to pass data to an eventual support service
                intent.putExtra(ENTERED_URL_KEY, EditTextUtils.getText(mUrlEditText));
                intent.putExtra(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
                intent.putExtra(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag
                        (mJetpackCallbacks.isJetpackAuth(), isWPComLogin() && !mSelfHosted));
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
        return EditTextUtils.getText(mUsernameEditText).isEmpty()
               && EditTextUtils.getText(mPasswordEditText).isEmpty()
               && mUsernameEditText != null
               && mPasswordEditText != null
               && mSmartLockEnabled
               && !mSelfHosted;
    }

    public void onCredentialRetrieved(Credential credential) {
        AppLog.d(T.NUX, "Retrieved username from SmartLock: " + credential.getId());
        if (isAdded() && canAutofillUsernameAndPassword()) {
            AnalyticsTracker.track(Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED);
            mUsernameEditText.setText(credential.getId());
            mPasswordEditText.setText(credential.getPassword());
        }
        showPasswordField();
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

            // make the top padding match the bottom padding of the logo so the logo doesn't touch the screen top
            mIconSwitcher.setPadding(mIconSwitcher.getPaddingLeft(), mIconSwitcher.getPaddingBottom(), mIconSwitcher
                    .getPaddingRight(), mIconSwitcher.getPaddingBottom());
        } else {
            mBottomButtonsLayout.setOrientation(LinearLayout.VERTICAL);
            setSecondaryButtonVisible(false);

            // revert the top padding to zero when in portrait
            mIconSwitcher.setPadding(mIconSwitcher.getPaddingLeft(), 0, mIconSwitcher.getPaddingRight(), mIconSwitcher
                    .getPaddingBottom());
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
        return !mSelfHosted || TextUtils.isEmpty(selfHostedUrl) ||
                WPUrlUtils.isWordPressCom(UrlUtils.addUrlSchemeIfNeeded(selfHostedUrl, false));
    }

    private void configureMagicLinkUI() {
        showDotComSignInForm();
        mSelfHosted = false;
        mPasswordLayout.setVisibility(View.GONE);
        mForgotPassword.setVisibility(View.GONE);
        mSignInButton.setText(getString(R.string.button_next));
    }

    private boolean isJetpackAuth() {
        return mJetpackSite != null;
    }

    // Set blog for Jetpack auth
    public void setBlogAndCustomMessageForJetpackAuth(SiteModel site, String customAuthMessage) {
        mJetpackSite = site;
        if (customAuthMessage != null && mJetpackAuthLabel != null) {
            mJetpackAuthLabel.setText(customAuthMessage);
        }

        if (mAddSelfHostedButton != null) {
            mJetpackAuthLabel.setVisibility(View.VISIBLE);
            mAddSelfHostedButton.setVisibility(View.GONE);
            mCreateAccountButton.setVisibility(View.GONE);
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

        AnalyticsTracker.track(Stat.CREATE_ACCOUNT_INITIATED);
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
            ActivityLauncher.openUrlExternal(getContext(), forgotPasswordUrl);
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

    private void trackAnalyticsSignIn() {
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);
        Map<String, Boolean> properties = new HashMap<>();
        properties.put("dotcom_user", isWPComLogin());
        AnalyticsTracker.track(Stat.SIGNED_IN, properties);
        if (!isWPComLogin()) {
            AnalyticsTracker.track(Stat.ADDED_SELF_HOSTED_SITE);
        }
    }

    private void saveCredentialsInSmartLock() {
        SmartLockHelper smartLockHelper = getSmartLockHelper();
        // mUsername and mPassword are null when the user log in with a magic link
        if (smartLockHelper != null && mUsername != null && mPassword != null) {
            smartLockHelper.saveCredentialsInSmartLock(mUsername, mPassword,
                    HtmlUtils.fastUnescapeHtml(mAccountStore.getAccount().getDisplayName()),
                    Uri.parse(mAccountStore.getAccount().getAvatarUrl()));
        }
    }

    private void finishCurrentActivity() {
        if (mIsActivityFinishing) {
            return;
        }

        // Clear persisted text from in the URL field
        mUrlEditText.setText("");

        mIsActivityFinishing = true;
        saveCredentialsInSmartLock();
        if (getActivity() == null) {
            return;
        }

        if (mInhibitMagicLogin) {
            // just finish the login activity and return to the its "caller"
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        } else {
            // move on the the main activity
            Intent intent = new Intent(getActivity(), WPMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(SignInActivity.MAGIC_LOGIN, mIsMagicLinksEnabled);
            getActivity().startActivity(intent);
        }
    }

    public void attemptLoginWithMagicLink() {
        // Save Token to the AccountStore, this will a onAuthenticationChanged (that will pull account setting and
        // sites)
        AccountStore.UpdateTokenPayload payload = new AccountStore.UpdateTokenPayload(mToken);
        mDispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(payload));
    }

    private SmartLockHelper getSmartLockHelper() {
        if (getActivity() != null && getActivity() instanceof SignInActivity) {
            return ((SignInActivity) getActivity()).getSmartLockHelper();
        }
        return null;
    }

    public void showAuthErrorMessage() {
        if (mJetpackAuthLabel != null) {
            mJetpackAuthLabel.setVisibility(View.VISIBLE);
            mJetpackAuthLabel.setText(getResources().getString(R.string.auth_required));
        }
    }

    private String getAuthCodeFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);

        if (clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemAt(0) != null
            && clipboard.getPrimaryClip().getItemAt(0).getText() != null) {
            String code = clipboard.getPrimaryClip().getItemAt(0).getText().toString();

            mTwoStepAuthCodeMatcher.reset(code);

            if (!code.isEmpty() && mTwoStepAuthCodeMatcher.matches()) {
                return code;
            }
        }

        return "";
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
        startProgress(getString(R.string.connecting_wpcom));
        AuthenticatePayload payload = new AuthenticatePayload(mUsername, mPassword);
        payload.twoStepCode = mTwoStepCode;
        payload.shouldSendTwoStepSms = mShouldSendTwoStepSMS;
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    private void signInAndFetchBlogListWPOrg() {
        startProgress(getString(R.string.signing_in));
        String url = EditTextUtils.getText(mUrlEditText).trim();
        url = url.replaceAll("\r|\n", "");
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(url));
    }

    private boolean checkNetworkConnectivity() {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            SignInDialogFragment nuxAlert;
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.no_network_title),
                    getString(R.string.no_network_message),
                    R.drawable.ic_notice_white_64dp,
                    getString(R.string.cancel));
            ft.add(nuxAlert, "alert");
            ft.commitAllowingStateLoss();
            return false;
        }
        return true;
    }

    private boolean checkIfUserIsAlreadyLoggedIn() {
        if (mAccountStore.hasAccessToken()) {
            String currentUsername = mAccountStore.getAccount().getUserName();
            AppLog.e(T.NUX, "User is already logged in WordPress.com: " + currentUsername
                            + " - but tries to sign in again: " + mUsername);
            if (getActivity() != null) {
                if (currentUsername.equals(mUsername)) {
                    ToastUtils.showToast(getActivity(), R.string.already_logged_in_wpcom_same_username, Duration.LONG);
                } else {
                    ToastUtils.showToast(getActivity(), R.string.already_logged_in_wpcom, Duration.LONG);
                }
            }
            return true;
        }
        return false;
    }

    protected void signIn() {
        if (mSelfHosted || !mIsMagicLinksEnabled) {
            if (!isUserDataValid()) {
                return;
            }

            if (!checkNetworkConnectivity()) {
                return;
            }

            mUsername = EditTextUtils.getText(mUsernameEditText).trim().toLowerCase();
            mPassword = EditTextUtils.getText(mPasswordEditText).trim();
            mTwoStepCode = EditTextUtils.getText(mTwoStepEditText).trim();
            if (isWPComLogin()) {
                // If the user is already logged in a wordpress.com account, bail out
                if (checkIfUserIsAlreadyLoggedIn()) {
                    return;
                }
                AppLog.i(T.NUX, "User tries to sign in on WordPress.com with username: " + mUsername);
                signInAndFetchBlogListWPCom();
            } else {
                String selfHostedUrl = EditTextUtils.getText(mUrlEditText).trim();
                AppLog.i(T.NUX, "User tries to sign in on Self Hosted: " + selfHostedUrl
                                + " with username: " + mUsername);
                signInAndFetchBlogListWPOrg();
            }
        } else {
            if (isUsernameEmail()) {
                startProgress(getActivity().getString(R.string.checking_email));
                mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(mUsername));
            } else if (isWPComDomain(mUsername)) {
                // If a wpcom domain was entered, check if the subdomain matches an existing username.
                String maybeUsername = UrlUtils.extractSubDomain(mUsername);
                if (maybeUsername.length() > 0) {
                    // See if the username exists.
                    startProgress(getActivity().getString(R.string.checking_username));
                    mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction(maybeUsername));

                } else {
                    // The text that was entered was .wordpress.com or the like.
                    // Its invalid so just show an error.
                    showUsernameError(R.string.username_invalid);
                }
            } else {
                showPasswordFieldAndFocus();
            }
        }
    }

    /**
     * Tests the specified string to see if it contains a wpcom subdomain.
     *
     * @param string The string to check
     * @return True if the string contains a wpcom subdomain, else false.
     */
    private boolean isWPComDomain(String string) {
        Matcher matcher = WPCOM_DOMAIN.matcher(string);
        return matcher.find();
    }

    private boolean isUsernameEmail() {
        mUsername = EditTextUtils.getText(mUsernameEditText).trim();
        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(mUsername);

        return matcher.find() && mUsername.length() <= MAX_EMAIL_LENGTH;
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
        final String url = EditTextUtils.getText(mUrlEditText).trim();
        boolean retValue = true;

        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }

        if (TextUtils.isEmpty(username)) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            retValue = false;
        }

        if (mIsSelfHostedForced && TextUtils.isEmpty(url)) {
            mUrlEditText.setError(getString(R.string.required_field));
            mUrlEditText.requestFocus();
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

    private void askForHttpAuthCredentials(@NonNull final String url) {
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
                mHTTPAuthManager.addHTTPAuthCredentials(mHttpUsername, mHttpPassword, url, null);
                signInAndFetchBlogListWPOrg();
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
                org.wordpress.android.R.drawable.ic_notice_white_64dp, 3, getString(
                        org.wordpress.android.R.string.cancel), getString(
                        org.wordpress.android.R.string.forgot_password), getString(
                        org.wordpress.android.R.string.contact_us), SignInDialogFragment.ACTION_OPEN_URL,
                SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT);

        // Put entered url and entered username args, that could help our support team
        Bundle bundle = nuxAlert.getArguments();
        bundle.putString(SignInDialogFragment.ARG_OPEN_URL_PARAM, getForgotPasswordURL());
        bundle.putString(ENTERED_URL_KEY, EditTextUtils.getText(mUrlEditText));
        bundle.putString(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
        bundle.putSerializable(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag
                (mJetpackCallbacks.isJetpackAuth(), isWPComLogin() && !mSelfHosted));
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

    private void showGenericErrorDialog(String errorMessage) {
        showGenericErrorDialog(errorMessage, null, null);
    }

    private void showGenericErrorDialog(String errorMessage, String faqId, String faqSection) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;

        int faqAction = SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT;
        String thirdButtonLabel = getString(R.string.contact_us);
        if (!TextUtils.isEmpty(faqId) || !TextUtils.isEmpty(faqSection)) {
            faqAction = SignInDialogFragment.ACTION_OPEN_FAQ_PAGE;
            thirdButtonLabel =  getString(R.string.tell_me_more);
        }
        nuxAlert = SignInDialogFragment.newInstance(getString(org.wordpress.android.R.string.nux_cannot_log_in),
                errorMessage, R.drawable.ic_notice_white_64dp, 3,
                getString(R.string.cancel), getString(R.string.reader_title_applog), thirdButtonLabel,
                SignInDialogFragment.ACTION_OPEN_APPLICATION_LOG,
                faqAction, faqId, faqSection);
        Bundle bundle = nuxAlert.getArguments();
        bundle.putSerializable(HelpshiftHelper.ORIGIN_KEY, HelpshiftHelper.chooseHelpshiftLoginTag
                (mJetpackCallbacks.isJetpackAuth(), isWPComLogin() && !mSelfHosted));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_SELF_HOSTED, mSelfHosted);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Autofill username / password if string fields are set (only useful after an error in sign up).
        // This can't be done in onCreateView
        if (mUsername != null) {
            mUsernameEditText.setText(mUsername);
        }
        if (mPassword != null) {
            mPasswordEditText.setText(mPassword);
        }
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
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

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAccountChanged has error: " + event.error.type + " - " + event.error.message);
            showAccountError(event.error.type, event.error.message);
            endProgress();
            return;
        }

        AppLog.i(T.NUX, "onAccountChanged: " + event.toString());

        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            // The user's account info has been fetched and stored - next, fetch the user's settings
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // The user's account settings have also been fetched and stored - now we can fetch the user's sites
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            AnalyticsTracker.track(Stat.LOGIN_FAILED, event.getClass().getSimpleName(), event.error.type.toString(),
                    event.error.message);
            showAuthError(event.error.type, event.error.message);
            endProgress();
            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        if (mAccountStore.hasAccessToken()) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            NotificationsUpdateService.startService(getActivity().getApplicationContext());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            AppLog.e(T.API, "onSiteChanged has error: " + event.error.type + " - " + event.error.toString());
            endProgress();
            if (!isAdded()) {
                return;
            }
            if (event.error.type == SiteErrorType.DUPLICATE_SITE) {
                if (event.rowsAffected == 0) {
                    // If there is a duplicate site and not any site has been added, show an error and
                    // stop the sign in process
                    ToastUtils.showToast(getContext(), R.string.cannot_add_duplicate_site);
                    return;
                } else {
                    // If there is a duplicate site, notify the user something could be wrong,
                    // but continue the sign in process
                    ToastUtils.showToast(getContext(), R.string.duplicate_site_detected);
                }
            } else {
                return;
            }
        }

        // Login Successful
        trackAnalyticsSignIn();

        // Fetching the sites is the last step of both WP.com and self-hosted sign in - we can now finish the activity
        finishCurrentActivity();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoverySucceeded(OnDiscoveryResponse event) {
        if (event.isError()) {
            AppLog.e(T.API, "onDiscoveryResponse has error: " + event.error.name() + " - " + event.error.toString());
            handleDiscoveryError(event.error, event.failedEndpoint);
            AnalyticsTracker.track(Stat.LOGIN_FAILED, event.getClass().getSimpleName(), event.error.name(),
                    event.error.toString());
            return;
        }
        AppLog.i(T.NUX, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
        RefreshSitesXMLRPCPayload selfhostedPayload = new RefreshSitesXMLRPCPayload();
        selfhostedPayload.username = mUsername;
        selfhostedPayload.password = mPassword;
        selfhostedPayload.url = event.xmlRpcEndpoint;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(selfhostedPayload));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (event.isError()) {
            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
        }

        switch(event.type) {
            case EMAIL:
                handleEmailAvailabilityEvent(event);
                break;
            case USERNAME:
                handleUsernameAvailabilityEvent(event);
                break;
            default:
                // Failsafe
                showSelfHostedSignInForm();
        }
    }

    /**
     * Handler for an email availability event. If a user enters an email address for their
     * username an API checks to see if it belongs to a wpcom account.  If it exists the magic links
     * flow is followed. Otherwise the self-hosted sign in form is shown.
     * @param event
     */
    private void handleEmailAvailabilityEvent(OnAvailabilityChecked event) {
        if (event.type != IsAvailable.EMAIL) {
            return;
        }
        if (!event.isAvailable) {
            // Email address exists in WordPress.com
            if (mListener != null) {
                mListener.onMagicLinkRequestSuccess(mUsername);
            }
        } else {
            // Email address doesn't exist in WordPress.com, show the self hosted sign in form
            showSelfHostedSignInForm();
        }
    }

    /**
     * Handler for a username availability event. If a user enters a wpcom domain as their username
     * an API call is made to check if the subdomain is a valid username. This method handles
     * the result of that API call, showing an error if the subdomain was not a valid username,
     * or showing the password field if it was a valid username.
     *
     * @param event
     */
    private void handleUsernameAvailabilityEvent(OnAvailabilityChecked event) {
        endProgress();
        if (event.type != IsAvailable.USERNAME ) {
            return;
        }
        if (event.isAvailable) {
            // Username doesn't exist in WordPress.com, show just show an error.
            showUsernameError(R.string.username_invalid);
            return;
        }
        // Username exists in WordPress.com. Update the form and show the password field.
        mUsername = event.value;
        mUsernameEditText.setText(event.value);
        showPasswordFieldAndFocus();
    }

    public void handleDiscoveryError(DiscoveryError error, final String failedEndpoint) {
        AppLog.e(T.API, "Discover error: " + error);
        endProgress();
        if (!isAdded()) {
            return;
        }
        switch (error) {
            case ERRONEOUS_SSL_CERTIFICATE:
                SelfSignedSSLUtils.showSSLWarningDialog(getActivity(), mMemorizingTrustManager,
                        new Callback() {
                            @Override
                            public void certificateTrusted() {
                                if (failedEndpoint == null) {
                                    return;
                                }
                                // retry login with the same parameters
                                startProgress(getString(R.string.signing_in));
                                mDispatcher.dispatch(
                                        AuthenticationActionBuilder.newDiscoverEndpointAction(failedEndpoint));
                            }
                        });
                break;
            case HTTP_AUTH_REQUIRED:
                askForHttpAuthCredentials(failedEndpoint);
                break;
            case WORDPRESS_COM_SITE:
                signInAndFetchBlogListWPCom();
                break;
            case NO_SITE_ERROR:
                showGenericErrorDialog(getResources().getString(R.string.no_site_error),
                        NO_SITE_HELPSHIFT_FAQ_ID,
                        NO_SITE_HELPSHIFT_FAQ_SECTION);
                break;
            case INVALID_URL:
                showUrlError(R.string.invalid_site_url_message);
                AnalyticsTracker.track(Stat.LOGIN_INSERTED_INVALID_URL);
                break;
            case MISSING_XMLRPC_METHOD:
                showGenericErrorDialog(getResources().getString(R.string.xmlrpc_missing_method_error),
                        MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_ID,
                        MISSING_XMLRPC_METHOD_HELPSHIFT_FAQ_SECTION);
                break;
            case XMLRPC_BLOCKED:
                // use this to help the user a bit:  pass the Helpshift page ID or section ID
                // on the rest of the error cases in this switch
                showGenericErrorDialog(getResources().getString(R.string.xmlrpc_post_blocked_error),
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID,
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION);
                break;
            case XMLRPC_FORBIDDEN:
                showGenericErrorDialog(getResources().getString(R.string.xmlrpc_endpoint_forbidden_error),
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_ID,
                        XMLRPC_BLOCKED_HELPSHIFT_FAQ_SECTION);
                break;
            case GENERIC_ERROR:
            default:
                showGenericErrorDialog(getResources().getString(R.string.nux_cannot_log_in));
                break;
        }
    }

    private void showAccountError(AccountStore.AccountErrorType error, String errorMessage) {
        switch (error) {
            case ACCOUNT_FETCH_ERROR:
                showError(R.string.error_fetch_my_profile);
                break;
            case SETTINGS_FETCH_ERROR:
                showError(R.string.error_fetch_account_settings);
                break;
            case SETTINGS_POST_ERROR:
                showError(R.string.error_post_account_settings);
                break;
            case GENERIC_ERROR:
            default:
                showError(errorMessage);
                break;
        }
    }

    private void showAuthError(AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
                handleInvalidUsernameOrPassword(R.string.username_or_password_incorrect);
                break;
            case INVALID_OTP:
                showTwoStepCodeError(R.string.invalid_verification_code);
                break;
            case NEEDS_2FA:
                setTwoStepAuthVisibility(true);
                mTwoStepEditText.setText(getAuthCodeFromClipboard());
                break;
            case INVALID_REQUEST:
                // TODO: FluxC: could be specific?
            default:
                // For all other kind of error, show a dialog with API Response error message
                AppLog.e(T.NUX, "Server response: " + errorMessage);
                showGenericErrorDialog(errorMessage);
                break;
        }
    }
}
