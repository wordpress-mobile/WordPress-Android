package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.SSLCertsViewActivity;
import org.wordpress.android.networking.SelfSignedSSLCertsManager;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService.UpdateTask;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker.EmailChecker;
import org.xmlrpc.android.ApiHelper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WelcomeFragmentSignIn extends NewAccountAbstractPageFragment implements TextWatcher {
    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;
    public static final String ENTERED_URL_KEY = "ENTERED_URL_KEY";
    public static final String ENTERED_USERNAME_KEY = "ENTERED_USERNAME_KEY";
    public static final String FROM_LOGIN_SCREEN_KEY = "FROM_LOGIN_SCREEN_KEY";
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mUrlEditText;
    private boolean mSelfHosted;
    private WPTextView mSignInButton;
    private WPTextView mCreateAccountButton;
    private WPTextView mAddSelfHostedButton;
    private WPTextView mProgressTextSignIn;
    private WPTextView mForgotPassword;
    private LinearLayout mBottomButtonsLayout;
    private RelativeLayout mProgressBarSignIn;
    private RelativeLayout mUrlButtonLayout;
    private ImageView mInfoButton;
    private ImageView mInfoButtonSecondary;
    private EmailChecker mEmailChecker;
    private boolean mEmailAutoCorrected;
    private int mWPComErroneousLogInCount;

    public WelcomeFragmentSignIn() {
        mEmailChecker = new EmailChecker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.nux_fragment_welcome, container, false);
        mUrlButtonLayout = (RelativeLayout) rootView.findViewById(R.id.url_button_layout);
        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (WPTextView) rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);
        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mCreateAccountButton = (WPTextView) rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);
        mAddSelfHostedButton = (WPTextView) rootView.findViewById(R.id.nux_add_selfhosted_button);
        mAddSelfHostedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUrlButtonLayout.getVisibility() == View.VISIBLE) {
                    mUrlButtonLayout.setVisibility(View.GONE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
                    mSelfHosted = false;
                } else {
                    mUrlButtonLayout.setVisibility(View.VISIBLE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_oops_not_selfhosted_blog));
                    mSelfHosted = true;
                }
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
        mBottomButtonsLayout = (LinearLayout) rootView.findViewById(R.id.nux_bottom_buttons);
        initPasswordVisibilityButton(rootView, mPasswordEditText);
        initInfoButtons(rootView);
        moveBottomButtons();
        return rootView;
    }

    /**
     * Hide toggle button "add self hosted / sign in with WordPress.com" and show self hosted URL
     * edit box
     */
    public void forceSelfHostedMode() {
        mUrlButtonLayout.setVisibility(View.VISIBLE);
        mAddSelfHostedButton.setVisibility(View.GONE);
        mCreateAccountButton.setVisibility(View.GONE);
        mSelfHosted = true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        moveBottomButtons();
    }

    private void initInfoButtons(View rootView) {
        OnClickListener infoButtonListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newAccountIntent = new Intent(getActivity(), NuxHelpActivity.class);
                // Used to pass data to an eventual support service
                newAccountIntent.putExtra(ENTERED_URL_KEY, EditTextUtils.getText(mUrlEditText));
                newAccountIntent.putExtra(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
                newAccountIntent.putExtra(FROM_LOGIN_SCREEN_KEY, true);
                startActivity(newAccountIntent);
            }
        };
        mInfoButton = (ImageView) rootView.findViewById(R.id.info_button);
        mInfoButtonSecondary = (ImageView) rootView.findViewById(R.id.info_button_secondary);
        mInfoButton.setOnClickListener(infoButtonListener);
        mInfoButtonSecondary.setOnClickListener(infoButtonListener);
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
        String suggest = mEmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mUsernameEditText.setText(suggest);
            mUsernameEditText.setSelection(suggest.length());
        }
    }

    private boolean isWPComLogin() {
        return !mSelfHosted || TextUtils.isEmpty(EditTextUtils.getText(mUrlEditText).trim());
    }

    private View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(getActivity(), NewAccountActivity.class);
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivityForResult(newAccountIntent, WelcomeActivity.CREATE_ACCOUNT_REQUEST);
            }
        }
    };

    private View.OnClickListener mForgotPasswordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String baseUrl = DOT_COM_BASE_URL;
            if (!isWPComLogin()) {
                baseUrl = EditTextUtils.getText(mUrlEditText).trim();
                String lowerCaseBaseUrl = baseUrl.toLowerCase(Locale.getDefault());
                if (!lowerCaseBaseUrl.startsWith("https://") && !lowerCaseBaseUrl.startsWith("http://")) {
                    baseUrl = "http://" + baseUrl;
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl + FORGOT_PASSWORD_RELATIVE_URL));
            startActivity(intent);
        }
    };

    protected void onDoneAction() {
        signin();
    }

    private TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
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

    private void signin() {
        if (!isUserDataValid()) {
            return;
        }
        new SetupBlogTask().execute();
    }

    private OnClickListener mSignInClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            signin();
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
    }

    private boolean fieldsFilled() {
        return EditTextUtils.getText(mUsernameEditText).trim().length() > 0
               && EditTextUtils.getText(mPasswordEditText).trim().length() > 0;
    }

    protected boolean isUserDataValid() {
        final String username = EditTextUtils.getText(mUsernameEditText).trim();
        final String password = EditTextUtils.getText(mPasswordEditText).trim();
        boolean retValue = true;

        if (username.equals("")) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            retValue = false;
        }

        if (password.equals("")) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }
        return retValue;
    }

    private boolean selfHostedFieldsFilled() {
        return fieldsFilled() && EditTextUtils.getText(mUrlEditText).trim().length() > 0;
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameEditText.setError(getString(messageId));
        mUsernameEditText.requestFocus();
    }

    private void showPasswordError(int messageId, String param) {
        mPasswordEditText.setError(getString(messageId, param));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId, String param) {
        mUsernameEditText.setError(getString(messageId, param));
        mUsernameEditText.requestFocus();
    }

    private void showUrlError(int messageId) {
        mUrlEditText.setError(getString(messageId));
        mUrlEditText.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case USERNAME:
            case PASSWORD:
                showUsernameError(messageId);
                showPasswordError(messageId);
                return true;
            default:
                return false;
        }
    }

    public void signInDotComUser() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                getActivity().getApplicationContext());
        String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
        if (username != null && password != null) {
            mUsernameEditText.setText(username);
            mPasswordEditText.setText(password);
            new SetupBlogTask().execute();
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
        mUrlEditText.setEnabled(true);
        mAddSelfHostedButton.setEnabled(true);
        mCreateAccountButton.setEnabled(true);
        mForgotPassword.setEnabled(true);
    }

    protected void askForSslTrust() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(getString(R.string.ssl_certificate_error));
        alert.setMessage(getString(R.string.ssl_certificate_ask_trust));
        alert.setPositiveButton(R.string.ssl_certificate_trust, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SetupBlogTask setupBlogTask = new SetupBlogTask();
                try {
                    SelfSignedSSLCertsManager selfSignedSSLCertsManager = SelfSignedSSLCertsManager.getInstance(
                            getActivity());
                    selfSignedSSLCertsManager.addCertificates(selfSignedSSLCertsManager.getLastFailureChain());
                } catch (IOException e) {
                    AppLog.e(T.NUX, e);
                } catch (GeneralSecurityException e) {
                    AppLog.e(T.NUX, e);
                }
                setupBlogTask.execute();
            }
        });
        alert.setNeutralButton(R.string.ssl_certificate_details, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getActivity(), SSLCertsViewActivity.class);
                try {
                    SelfSignedSSLCertsManager selfSignedSSLCertsManager = SelfSignedSSLCertsManager.getInstance(
                            getActivity());
                    String lastFailureChainDesc = "URL: " + EditTextUtils.getText(mUrlEditText).trim() + "<br/><br/>"
                                + selfSignedSSLCertsManager.getLastFailureChainDescription().replaceAll("\n", "<br/>");
                    intent.putExtra(SSLCertsViewActivity.CERT_DETAILS_KEYS, lastFailureChainDesc);
                    getActivity().startActivityForResult(intent, WelcomeActivity.SHOW_CERT_DETAILS);
                } catch (GeneralSecurityException e) {
                    AppLog.e(T.NUX, e);
                } catch (IOException e) {
                    AppLog.e(T.NUX, e);
                }
            }
        });
        alert.setNegativeButton(R.string.ssl_certificate_do_not_trust, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.show();
        endProgress();
    }

    private class SetupBlogTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
        private SetupBlog mSetupBlog;
        private int mErrorMsgId;

        private void setHttpCredentials(String username, String password) {
            if (mSetupBlog == null) {
                mSetupBlog = new SetupBlog();
            }
            mSetupBlog.setHttpUsername(username);
            mSetupBlog.setHttpPassword(password);
        }

        @Override
        protected void onPreExecute() {
            if (mSetupBlog == null) {
                mSetupBlog = new SetupBlog();
            }
            mSetupBlog.setUsername(EditTextUtils.getText(mUsernameEditText).trim());
            mSetupBlog.setPassword(EditTextUtils.getText(mPasswordEditText).trim());
            if (mSelfHosted) {
                mSetupBlog.setSelfHostedURL(EditTextUtils.getText(mUrlEditText).trim());
            } else {
                mSetupBlog.setSelfHostedURL(null);
            }
            startProgress(selfHostedFieldsFilled() ? getString(R.string.attempting_configure) : getString(
                    R.string.connecting_wpcom));
        }

        private void refreshBlogContent(Map<String, Object> blogMap) {
            String blogId = blogMap.get("blogid").toString();
            String xmlRpcUrl = blogMap.get("xmlrpc").toString();
            int intBlogId = StringUtils.stringToInt(blogId, -1);
            if (intBlogId == -1) {
                AppLog.e(T.NUX, "Can't refresh blog content - invalid blogId: " + blogId);
                return;
            }
            int blogLocalId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(intBlogId, xmlRpcUrl);
            Blog firstBlog = WordPress.wpDB.instantiateBlogByLocalId(blogLocalId);
            new ApiHelper.RefreshBlogContentTask(getActivity(), firstBlog, null).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, false);
        }

        /**
         * Get first blog and call RefreshBlogContentTask. First blog will be autoselected when user login.
         * Also when a user add a new self hosted blog, userBlogList contains only one element.
         * TODO: when user's default blog autoselection is implemented, we should refresh the default one and
         * not the first one.
         * We don't want to refresh the whole list because it can be huge and each blog is refreshed when
         * user selects it.
         */
        private void refreshFirstBlogContent(List<Map<String, Object>> userBlogList) {
            if (userBlogList != null && !userBlogList.isEmpty()) {
                Map<String, Object> firstBlogMap = userBlogList.get(0);
                refreshBlogContent(firstBlogMap);
            }
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Void... args) {
            List<Map<String, Object>> userBlogList = mSetupBlog.getBlogList();
            mErrorMsgId = mSetupBlog.getErrorMsgId();
            if (mErrorMsgId != 0) {
                return null;
            }
            if (userBlogList != null) {
                mSetupBlog.addBlogs(userBlogList);
            }
            mErrorMsgId = mSetupBlog.getErrorMsgId();
            if (mErrorMsgId != 0) {
                return null;
            }
            return userBlogList;
        }

        private void httpAuthRequired() {
            // Prompt for http credentials
            mSetupBlog.setHttpAuthRequired(false);
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            alert.setTitle(R.string.http_authorization_required);

            View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.alert_http_auth, null);
            final EditText usernameEditText = (EditText) httpAuth.findViewById(R.id.http_username);
            final EditText passwordEditText = (EditText) httpAuth.findViewById(R.id.http_password);
            alert.setView(httpAuth);
            alert.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    SetupBlogTask setupBlogTask = new SetupBlogTask();
                    setupBlogTask.setHttpCredentials(EditTextUtils.getText(usernameEditText), EditTextUtils.getText(
                            passwordEditText));
                    setupBlogTask.execute();
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

        private void handleInvalidUsernameOrPassword() {
            if (isWPComLogin()) {
                mWPComErroneousLogInCount += 1;
                if (mWPComErroneousLogInCount >= WPCOM_ERRONEOUS_LOGIN_THRESHOLD) {
                    mErrorMsgId = R.string.username_or_password_incorrect_selfhosted_hint;
                }
            }
            if (mErrorMsgId == R.string.username_or_password_incorrect_selfhosted_hint) {
                showUsernameError(mErrorMsgId, getString(R.string.nux_add_selfhosted_blog));
                showPasswordError(mErrorMsgId, getString(R.string.nux_add_selfhosted_blog));
            } else {
                showUsernameError(mErrorMsgId);
                showPasswordError(mErrorMsgId);
            }
            mErrorMsgId = 0;
            endProgress();
        }

        private void signInError() {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            NUXDialogFragment nuxAlert;
            if (mErrorMsgId == R.string.account_two_step_auth_enabled) {
                nuxAlert = NUXDialogFragment.newInstance(getString(R.string.nux_cannot_log_in), getString(
                                mErrorMsgId), getString(R.string.nux_tap_continue), R.drawable.nux_icon_alert, true,
                        getString(R.string.visit_security_settings), NUXDialogFragment.ACTION_OPEN_URL,
                        "https://wordpress.com/settings/security/?ssl=forced");
            } else {
                if (mErrorMsgId == R.string.username_or_password_incorrect) {
                    handleInvalidUsernameOrPassword();
                    return;
                } else if (mErrorMsgId == R.string.invalid_url_message) {
                    showUrlError(mErrorMsgId);
                    mErrorMsgId = 0;
                    endProgress();
                    return;
                } else {
                    nuxAlert = NUXDialogFragment.newInstance(getString(R.string.nux_cannot_log_in), getString(
                            mErrorMsgId), getString(R.string.nux_tap_continue), R.drawable.nux_icon_alert);
                }
            }
            ft.add(nuxAlert, "alert");
            ft.commitAllowingStateLoss();
            mErrorMsgId = 0;
            endProgress();
        }

        @Override
        protected void onPostExecute(final List<Map<String, Object>> userBlogList) {
            if (mSetupBlog.isErroneousSslCertificates() && isAdded()) {
                askForSslTrust();
                return;
            }

            if (mSetupBlog.isHttpAuthRequired() && isAdded()) {
                httpAuthRequired();
                return;
            }

            if (userBlogList == null && mErrorMsgId != 0 && isAdded()) {
                signInError();
                return;
            }

            Map<String, Boolean> properties = new HashMap<String, Boolean>();
            properties.put("dotcom_user", mSetupBlog.isDotComBlog());

            AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN, properties);

            refreshFirstBlogContent(userBlogList);

            if (mSelfHosted) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.ADDED_SELF_HOSTED_SITE);
            }

            // Update wp.com credentials
            if (mSetupBlog.getXmlrpcUrl() != null && mSetupBlog.getXmlrpcUrl().contains("wordpress.com")) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mSetupBlog.getUsername());
                editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, WordPressDB.encryptPassword(
                        mSetupBlog.getPassword()));
                editor.commit();
                // Fire off a request to get an access token
                WordPress.getRestClientUtils().get("me", new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        ReaderUserActions.setCurrentUser(jsonObject);
                    }
                }, null);
            }

            AnalyticsTracker.refreshMetadata();

            // get the user's reader tags so they're available as soon as the Reader is accessed
            if (!mSelfHosted) {
                ReaderUpdateService.startService(getActivity(), EnumSet.of(UpdateTask.TAGS));
            }

            if (userBlogList != null) {
                if (getActivity() != null) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            } else {
                endProgress();
            }
        }
    }
}
