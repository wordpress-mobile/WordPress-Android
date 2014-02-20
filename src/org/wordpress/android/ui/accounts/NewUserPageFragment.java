
package org.wordpress.android.ui.accounts;

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
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.UserEmail;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker.EmailChecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewUserPageFragment extends NewAccountAbstractPageFragment implements TextWatcher {
    private EditText mSiteUrlTextField;
    private EditText mEmailTextField;
    private EditText mPasswordTextField;
    private EditText mUsernameTextField;
    private WPTextView mSignupButton;
    private WPTextView mProgressTextSignIn;
    private RelativeLayout mProgressBarSignIn;

    private EmailChecker mEmailChecker;
    private boolean mEmailAutoCorrected;

    public NewUserPageFragment() {
        mEmailChecker = new EmailChecker();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (fieldsFilled()) {
            mSignupButton.setEnabled(true);
        } else {
            mSignupButton.setEnabled(false);
        }
    }

    private boolean fieldsFilled() {
        return mEmailTextField.getText().toString().trim().length() > 0
                && mPasswordTextField.getText().toString().trim().length() > 0
                && mUsernameTextField.getText().toString().trim().length() > 0
                && mSiteUrlTextField.getText().toString().trim().length() > 0;
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
        final String email = mEmailTextField.getText().toString().trim();
        final String password = mPasswordTextField.getText().toString().trim();
        final String username = mUsernameTextField.getText().toString().trim();
        final String siteUrl = mSiteUrlTextField.getText().toString().trim();
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

    private OnClickListener signupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            validateAndCreateUserAndBlog();
        }
    };

    private TextView.OnEditorActionListener mEditorAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return onDoneEvent(actionId, event);
        }
    };

    private String siteUrlToSiteName(String siteUrl) {
        return siteUrl;
    }

    private void finishThisStuff(String username) {
        final NewAccountActivity act = (NewAccountActivity) getActivity();
        Bundle bundle = new Bundle();
        bundle.putString("username", username);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        act.setResult(act.RESULT_OK, intent);
        act.finish();
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
            AlertUtil.showAlert(getActivity(), R.string.no_network_title,
                    R.string.no_network_message);
            return;
        }
        if (!isUserDataValid())
            return;

        // Prevent double tapping of the "done" btn in keyboard for those clients that don't dismiss the keyboard.
        // Samsung S4 for example
        if (View.VISIBLE == mProgressBarSignIn.getVisibility()) {
            return;
        }

        startProgress(getString(R.string.validating_user_data));

        final String siteUrl = mSiteUrlTextField.getText().toString().trim();
        final String email = mEmailTextField.getText().toString().trim();
        final String password = mPasswordTextField.getText().toString().trim();
        final String username = mUsernameTextField.getText().toString().trim();
        final String siteName = siteUrlToSiteName(siteUrl);
        final String language = CreateUserAndBlog.getDeviceLanguage(getActivity().getResources());

        CreateUserAndBlog createUserAndBlog = new CreateUserAndBlog(email, username, password,
                siteUrl, siteName, language, restClient, getActivity(), new ErrorListener(),
                new CreateUserAndBlog.Callback() {
                    @Override
                    public void onStepFinished(CreateUserAndBlog.Step step) {
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
                            case CREATE_SITE: // no messages
                            case AUTHENTICATE_USER:
                            default:
                                break;
                        }
                    }

                    @Override
                    public void onSuccess(JSONObject createSiteResponse) {
                        endProgress();
                        if (hasActivity()) {
                            finishThisStuff(username);
                        }
                    }

                    @Override
                    public void onError(int messageId) {
                        endProgress();
                        if (hasActivity()) {
                            showError(getString(messageId));
                        }
                    }
                });
        createUserAndBlog.startCreateUserAndBlogProcess();
    }

    private void autocorrectEmail() {
        if (mEmailAutoCorrected)
            return;
        final String email = mEmailTextField.getText().toString().trim();
        String suggest = mEmailChecker.suggestDomainCorrection(email);
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
                Intent newAccountIntent = new Intent(getActivity(), NuxHelpActivity.class);
                startActivity(newAccountIntent);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.new_account_user_fragment_screen, container, false);

        WPTextView termsOfServiceTextView = (WPTextView) rootView.findViewById(R.id.l_agree_terms_of_service);
        termsOfServiceTextView.setText(Html.fromHtml(String.format(getString(R.string.agree_terms_of_service), "<u>", "</u>")));
        termsOfServiceTextView.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri = Uri.parse(Constants.URL_TOS);
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                }
        );

        mSignupButton = (WPTextView) rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(signupClickListener);
        mSignupButton.setEnabled(false);

        mProgressTextSignIn = (WPTextView) rootView.findViewById(R.id.nux_sign_in_progress_text);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);

        mEmailTextField = (EditText) rootView.findViewById(R.id.email_address);
        mEmailTextField.setText(UserEmail.getPrimaryEmail(getActivity()));
        mEmailTextField.setSelection(mEmailTextField.getText().toString().length());
        mPasswordTextField = (EditText) rootView.findViewById(R.id.password);
        mUsernameTextField = (EditText) rootView.findViewById(R.id.username);
        mSiteUrlTextField = (EditText) rootView.findViewById(R.id.site_url);

        mEmailTextField.addTextChangedListener(this);
        mPasswordTextField.addTextChangedListener(this);
        mUsernameTextField.addTextChangedListener(this);
        mSiteUrlTextField.addTextChangedListener(this);
        mSiteUrlTextField.setOnEditorActionListener(mEditorAction);
        mUsernameTextField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // auto fill blog address
                mSiteUrlTextField.setError(null);
                mSiteUrlTextField.setText(mUsernameTextField.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
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
}