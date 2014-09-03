package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * An activity to let the user specify their WordPress.com credentials.
 * Should be used to get WordPress.com credentials for JetPack integration in self-hosted sites.
 */
public class WPComLoginActivity extends Activity implements TextWatcher {
    public static final int REQUEST_CODE = 5000;
    public static final String JETPACK_AUTH_REQUEST = "jetpackAuthRequest";
    private static final String NEED_HELP_URL = "http://android.wordpress.org/faq";
    private String mUsername;
    private String mPassword;
    private Button mSignInButton;
    private boolean mIsJetpackAuthRequest;
    private boolean mIsWpcomAccountWith2FA;
    private boolean mIsInvalidUsernameOrPassword;
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private boolean mPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wp_dot_com_login_activity);
        setTitle(getString(R.string.wpcom_signin_dialog_title));

        if (getIntent().hasExtra(JETPACK_AUTH_REQUEST)) {
            mIsJetpackAuthRequest = true;
        }

        mSignInButton = (Button) findViewById(R.id.saveDotcom);
        mSignInButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                signIn();
            }
        });

        TextView wpcomHelp = (TextView) findViewById(R.id.wpcomHelp);
        wpcomHelp.setOnClickListener(new TextView.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(NEED_HELP_URL));
                startActivity(intent);
            }
        });

        mUsernameEditText = (EditText) findViewById(R.id.dotcomUsername);
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText = (EditText) findViewById(R.id.dotcomPassword);
        mPasswordEditText.addTextChangedListener(this);
        initPasswordVisibilityButton((ImageView) findViewById(R.id.password_visibility), mPasswordEditText);
    }

    private void signIn() {
        mUsername = EditTextUtils.getText(mUsernameEditText);
        mPassword = EditTextUtils.getText(mPasswordEditText);
        boolean validUsernameAndPassword = true;

        if (mUsername.equals("")) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            validUsernameAndPassword = false;
        }
        if (mPassword.equals("")) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            validUsernameAndPassword = false;
        }
        if (validUsernameAndPassword) {
            new SignInTask().execute();
        }
    }

    protected void initPasswordVisibilityButton(final ImageView passwordVisibilityToggleView,
                                                final EditText passwordEditText) {
        passwordVisibilityToggleView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPasswordVisible = !mPasswordVisible;
                if (mPasswordVisible) {
                    passwordVisibilityToggleView.setImageResource(R.drawable.dashicon_eye_open);
                    passwordVisibilityToggleView.setColorFilter(v.getContext().getResources().getColor(R.color.nux_eye_icon_color_open));
                    passwordEditText.setTransformationMethod(null);
                } else {
                    passwordVisibilityToggleView.setImageResource(R.drawable.dashicon_eye_closed);
                    passwordVisibilityToggleView.setColorFilter(v.getContext().getResources().getColor(R.color.nux_eye_icon_color_closed));
                    passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                passwordEditText.setSelection(passwordEditText.length());
            }
        });
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mPasswordEditText.setError(null);
        mUsernameEditText.setError(null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
    }

    private void setEditTextAndButtonEnabled(boolean enable) {
        mUsernameEditText.setEnabled(enable);
        mPasswordEditText.setEnabled(enable);
        mSignInButton.setEnabled(enable);
    }

    private class SignInTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            mSignInButton.setText(getString(R.string.attempting_configure));
            setEditTextAndButtonEnabled(false);
            mIsWpcomAccountWith2FA = false;
            mIsInvalidUsernameOrPassword = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            URI uri;
            try {
                uri = new URI(Constants.wpcomXMLRPCURL);
            } catch (URISyntaxException e) {
                AppLog.e(T.API, "Invalid URI syntax: " + Constants.wpcomXMLRPCURL);
                return false;
            }
            XMLRPCClientInterface client = XMLRPCFactory.instantiate(uri, "", "");
            Object[] signInParams = {mUsername, mPassword};
            try {
                client.call("wp.getUsersBlogs", signInParams);
                Blog blog = WordPress.getCurrentBlog();
                if (blog != null) {
                    blog.setDotcom_username(mUsername);
                    blog.setDotcom_password(mPassword);
                }

                // Don't change global WP.com settings if this is Jetpack auth request from stats
                if (!mIsJetpackAuthRequest) {
                    // New wpcom credetials inserted here. Reset the app state: there is the possibility a different
                    // username/password is inserted here
                    WordPress.removeWpComUserRelatedData(WPComLoginActivity.this);
                    WordPress.sendLocalBroadcast(WPComLoginActivity.this, WordPress.BROADCAST_ACTION_SIGNOUT);

                    Editor settings = PreferenceManager.getDefaultSharedPreferences(WPComLoginActivity.this).edit();
                    settings.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
                    settings.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, WordPressDB.encryptPassword(mPassword));
                    settings.commit();

                    // Make sure to update credentials for .wpcom blog even if currentBlog is null
                    WordPress.wpDB.updateWPComCredentials(mUsername, mPassword);

                    // Update regular blog credentials for WP.com auth requests
                    if (blog != null) {
                        blog.setUsername(mUsername);
                        blog.setPassword(mPassword);
                    }
                }
                if (blog != null) {
                    WordPress.wpDB.saveBlog(blog);
                }
                return true;
            } catch (XMLRPCFault xmlRpcFault) {
                AppLog.e(T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
                if (xmlRpcFault.getFaultCode() == 403) {
                    mIsInvalidUsernameOrPassword = true;
                }
                if (xmlRpcFault.getFaultCode() == 425) {
                    mIsWpcomAccountWith2FA = true;
                    return false;
                }
            } catch (XMLRPCException e) {
                AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
            } catch (IOException e) {
                AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
            } catch (XmlPullParserException e) {
                AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
            }

            mIsWpcomAccountWith2FA = false;
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isSignedIn) {
            if (isSignedIn && !isFinishing()) {
                if (!mIsJetpackAuthRequest) {
                    WordPress.getRestClientUtils().get("me", new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            WPComLoginActivity.this.setResult(RESULT_OK);
                            // Register the device again for Push Notifications
                            WordPress.registerForCloudMessaging(WPComLoginActivity.this);
                            ReaderUserActions.setCurrentUser(jsonObject);
                            finish();
                        }
                    }, null);
                } else {
                    WPComLoginActivity.this.setResult(RESULT_OK);
                    finish();
                }
            } else {
                if (mIsInvalidUsernameOrPassword) {
                    mUsernameEditText.setError(getString(R.string.username_or_password_incorrect));
                    mPasswordEditText.setError(getString(R.string.username_or_password_incorrect));
                } else {
                    String errorMessage = mIsWpcomAccountWith2FA ? getString(R.string.account_two_step_auth_enabled)
                            : getString(R.string.nux_cannot_log_in);
                    Toast.makeText(getBaseContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
                setEditTextAndButtonEnabled(true);
                mSignInButton.setText(R.string.sign_in);
            }
        }
    }
}
