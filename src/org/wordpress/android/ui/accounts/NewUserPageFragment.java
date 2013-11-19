
package org.wordpress.android.ui.accounts;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.Config;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AlertUtil;
import org.wordpress.android.util.UserEmail;
import org.wordpress.android.widgets.WPTextView;
import org.wordpress.emailchecker.EmailChecker;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewUserPageFragment extends NewAccountAbstractPageFragment implements TextWatcher {

    private EditText mEmailTextField;
    private EditText mPasswordTextField;
    private EditText mUsernameTextField;
    private WPTextView mSignupButton;
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
                && mUsernameTextField.getText().toString().trim().length() > 0;
    }

    private boolean checkUserData() {
        // try to create the user
        final String email = mEmailTextField.getText().toString().trim();
        final String password = mPasswordTextField.getText().toString().trim();
        final String username = mUsernameTextField.getText().toString().trim();

        if (email.equals("")) {
            mEmailTextField.setError(getString(R.string.required_field));
            mEmailTextField.requestFocus();
            return false;
        }

        final String emailRegEx = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        final Pattern emailRegExPattern = Pattern.compile(emailRegEx,
                Pattern.DOTALL);
        Matcher matcher = emailRegExPattern.matcher(email);
        if (!matcher.find() || email.length() > 100) {
            mEmailTextField.setError(getString(R.string.invalid_email_message));
            mEmailTextField.requestFocus();
            return false;
        }

        if (username.equals("")) {
            mUsernameTextField.setError(getString(R.string.required_field));
            mUsernameTextField.requestFocus();
            return false;
        }

        if (username.length() < 4) {
            mUsernameTextField.setError(getString(R.string.invalid_username_too_short));
            mUsernameTextField.requestFocus();
            return false;
        }

        if (username.length() > 60) {
            mUsernameTextField.setError(getString(R.string.invalid_username_too_long));
            mUsernameTextField.requestFocus();
            return false;
        }

        if (password.equals("")) {
            mPasswordTextField.setError(getString(R.string.required_field));
            mPasswordTextField.requestFocus();
            return false;
        }

        if (password.length() < 4) {
            mPasswordTextField.setError(getString(R.string.invalid_password_message));
            mPasswordTextField.requestFocus();
            return false;
        }

        return true;
    }

    OnClickListener signupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        
            //TODO: The following lines ensure that no .com account are available in the app - change this!!!!
            WordPress.signOut(getActivity());
            
            //reset the data
            NewAccountActivity act = (NewAccountActivity)getActivity();
            act.validatedEmail = null;
            act.validatedPassword = null;
            act.validatedUsername = null;
            
            if (mSystemService.getActiveNetworkInfo() == null) {
                AlertUtil.showAlert(getActivity(), R.string.no_network_title, R.string.no_network_message);
                return;
            }
            
            // try to create the user
            final String email = mEmailTextField.getText().toString().trim();
            final String password = mPasswordTextField.getText().toString().trim();
            final String username = mUsernameTextField.getText().toString().trim();

            if (false == checkUserData())
                return;

            pd = ProgressDialog.show(NewUserPageFragment.this.getActivity(),
                    getString(R.string.account_setup),
                    getString(R.string.validating_user_data), true, false);
            restPostNewUser(username, password, email, pd);
        }
    };

    private void restPostNewUser(final String username, final String password, final String email,
                                 final ProgressDialog progressDialog) {
        String path = "users/new";
        Map<String, String> params = new HashMap<String, String>();
        params.put("username", username);
        params.put("password", password);
        params.put("email", email);
        params.put("validate", "1");
        params.put("client_id", Config.OAUTH_APP_ID);
        params.put("client_secret", Config.OAUTH_APP_SECRET);

        restClient.post(path, params, null,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (progressDialog != null)
                            progressDialog.dismiss();
                        Log.d("1. New User PAGE", String.format("OK %s", response.toString()));
                        try {
                            if (response.getBoolean("success")) {
                                NewAccountActivity act = (NewAccountActivity) getActivity();
                                act.validatedEmail = email;
                                act.validatedPassword = password;
                                act.validatedUsername = username;
                                act.showNextItem();
                            } else {
                                showError(getString(R.string.error_generic));
                            }
                        } catch (JSONException e) {
                            showError(getString(R.string.error_generic));
                        }
                    }
                },
                new ErrorListener()
        );
    }

    private void autocorrectEmail() {
        if (mEmailAutoCorrected)
            return ;
        final String email = mEmailTextField.getText().toString().trim();
        String suggest = mEmailChecker.suggestDomainCorrection(email);
        if (suggest.compareTo(email) != 0) {
            mEmailAutoCorrected = true;
            mEmailTextField.setText(suggest);
            mEmailTextField.setSelection(suggest.length());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.new_account_user_fragment_screen, container, false);

        WPTextView termsOfServiceTextView = (WPTextView)rootView.findViewById(R.id.l_agree_terms_of_service);
        termsOfServiceTextView.setText(Html.fromHtml(String.format(getString(R.string.agree_terms_of_service, "<u>", "</u>"))));
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

        mEmailTextField = (EditText) rootView.findViewById(R.id.email_address);
        mEmailTextField.setText(UserEmail.getPrimaryEmail(getActivity()));
        mEmailTextField.setSelection(mEmailTextField.getText().toString().length());
        mPasswordTextField = (EditText) rootView.findViewById(R.id.password);
        mUsernameTextField = (EditText) rootView.findViewById(R.id.username);

        mEmailTextField.addTextChangedListener(this);
        mPasswordTextField.addTextChangedListener(this);
        mUsernameTextField.addTextChangedListener(this);

        mEmailTextField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectEmail();
                }
            }
        });

        return rootView;
    }
}