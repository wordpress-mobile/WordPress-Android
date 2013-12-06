
package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker.EmailChecker;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WelcomeFragmentSignIn extends NewAccountAbstractPageFragment implements TextWatcher {
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mUrlEditText;
    private boolean mSelfHosted;
    private WPTextView mSignInButton;
    private WPTextView mCreateAccountButton;
    private WPTextView mAddSelfHostedButton;
    private WPTextView mProgressTextSignIn;
    private ProgressBar mProgressBarSignIn;
    private RelativeLayout mUrlButtonLayout;
    private boolean mHttpAuthRequired;
    private String mHttpUsername = "";
    private String mHttpPassword = "";
    private EmailChecker mEmailChecker;
    private boolean mEmailAutoCorrected;

    public WelcomeFragmentSignIn() {
        mEmailChecker = new EmailChecker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.nux_fragment_welcome, container, false);

        ImageView statsIcon = (ImageView) rootView.findViewById(R.id.nux_fragment_icon);
        statsIcon.setImageResource(R.drawable.nux_icon_wp);

        mUrlButtonLayout = (RelativeLayout) rootView.findViewById(R.id.url_button_layout);
        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (WPTextView) rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mProgressBarSignIn = (ProgressBar) rootView.findViewById(R.id.nux_sign_in_progress_bar);
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

        mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectUsername();
                }
            }
        });
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

    private void autocorrectUsername() {
        if (mEmailAutoCorrected)
            return;
        final String email = mUsernameEditText.getText().toString().trim();
        // Check if the username looks like an email address
        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find()) {
            return ;
        }
        // It looks like an email address, then try to correct it
        String suggest = mEmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mUsernameEditText.setText(suggest);
            mUsernameEditText.setSelection(suggest.length());
        }
    }

    private View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(getActivity(), NewAccountActivity.class);
            startActivityForResult(newAccountIntent, WelcomeActivity.CREATE_ACCOUNT_REQUEST);
        }
    };

    private OnClickListener mSignInClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!wpcomFieldsFilled()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment
                        .newInstance(getString(R.string.required_fields));
                alert.show(ft, "alert");
                return;
            }
            new SetupBlogTask().execute();
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
        if (wpcomFieldsFilled()) {
            mSignInButton.setEnabled(true);
        } else {
            mSignInButton.setEnabled(false);
        }
        mPasswordEditText.setError(null);
        mUsernameEditText.setError(null);
    }

    private boolean wpcomFieldsFilled() {
        return mUsernameEditText.getText().toString().trim().length() > 0
                && mPasswordEditText.getText().toString().trim().length() > 0;
    }

    private boolean selfHostedFieldsFilled() {
        return wpcomFieldsFilled()
                && mUrlEditText.getText().toString().trim().length() > 0;
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameEditText.setError(getString(messageId));
        mUsernameEditText.requestFocus();
    }

    protected boolean specificShowError(int messageId) {
        switch (getErrorType(messageId)) {
            case USERNAME:
            case PASSWORD:
                showUsernameError(messageId);
                showPasswordError(messageId);
                return true;
        }
        return false;
    }

    public void signInDotComUser() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
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
    }

    protected void endProgress() {
        mProgressBarSignIn.setVisibility(View.GONE);
        mProgressTextSignIn.setVisibility(View.GONE);
        mSignInButton.setVisibility(View.VISIBLE);
    }

    private class SetupBlogTask extends AsyncTask<Void, Void, List<Object>> {
        private SetupBlog mSetupBlog;
        private int mErrorMsgId;

        @Override
        protected void onPreExecute() {
            mSetupBlog = new SetupBlog();
            mSetupBlog.setUsername(mUsernameEditText.getText().toString().trim());
            mSetupBlog.setPassword(mPasswordEditText.getText().toString().trim());
            if (mSelfHosted) {
                mSetupBlog.setSelfHostedURL(mUrlEditText.getText().toString().trim());
            } else {
                mSetupBlog.setSelfHostedURL(null);
            }
            startProgress(selfHostedFieldsFilled() ? getString(R.string.attempting_configure) :
                    getString(R.string.connecting_wpcom));
        }

        @Override
        protected List doInBackground(Void... args) {
            List userBlogList = mSetupBlog.getBlogList();
            mErrorMsgId = mSetupBlog.getErrorMsgId();
            if (userBlogList != null) {
                mSetupBlog.addBlogs(userBlogList);
            }
            return userBlogList;
        }

        @Override
        protected void onPostExecute(final List<Object> userBlogList) {
            if (mHttpAuthRequired) {
                if (getActivity() == null) {
                    return ;
                }
                // Prompt for http credentials
                mHttpAuthRequired = false;
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.http_authorization_required);

                View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.alert_http_auth, null);
                final EditText usernameEditText = (EditText) httpAuth.findViewById(R.id.http_username);
                final EditText passwordEditText = (EditText) httpAuth.findViewById(R.id.http_password);
                alert.setView(httpAuth);

                alert.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mSetupBlog.setHttpUsername(usernameEditText.getText().toString());
                        mSetupBlog.setHttpPassword(passwordEditText.getText().toString());
                        new SetupBlogTask().execute();
                    }
                });

                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
                endProgress();
                return;
            }

            if (userBlogList == null && mErrorMsgId != 0) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                NUXDialogFragment nuxAlert;
                if (mErrorMsgId == R.string.account_two_step_auth_enabled) {
                    nuxAlert = NUXDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                            getString(mErrorMsgId), getString(R.string.nux_tap_continue),
                            R.drawable.nux_icon_alert, true,
                            getString(R.string.visit_security_settings),
                            NUXDialogFragment.ACTION_OPEN_URL,
                            "https://wordpress.com/settings/security/?ssl=forced");
                } else {
                    if (mErrorMsgId == R.string.username_or_password_incorrect) {
                        showUsernameError(mErrorMsgId);
                        showPasswordError(mErrorMsgId);
                        mErrorMsgId = 0;
                        endProgress();
                        return ;
                    } else {
                        nuxAlert = NUXDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                                getString(mErrorMsgId), getString(R.string.nux_tap_continue),
                                R.drawable.nux_icon_alert);
                    }
                }
                nuxAlert.show(ft, "alert");
                mErrorMsgId = 0;
                endProgress();
                return;
            }

            // Update wp.com credentials
            if (mSetupBlog.getXmlrpcUrl().contains("wordpress.com")) {
                SharedPreferences settings = PreferenceManager.
                        getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mSetupBlog.getUsername());
                editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE,
                        WordPressDB.encryptPassword(mSetupBlog.getPassword()));
                editor.commit();
                // Fire off a request to get an access token
                WordPress.restClient.get("me", null, null);
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
