package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.reader.actions.ReaderUserActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;
import org.xmlrpc.android.XMLRPCFactory;
import org.xmlrpc.android.XMLRPCFault;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * An activity to let the user specify their WordPress.com credentials.
 * Should be used to get WordPress.com credentials for JetPack integration in self-hosted sites.
 */
public class WPComLoginActivity extends SherlockFragmentActivity {

    public static final int REQUEST_CODE = 5000;
    public static final String JETPACK_AUTH_REQUEST = "jetpackAuthRequest";
    private String mUsername;
    private String mPassword;
    private Button mSignInButton;
    private boolean mIsJetpackAuthRequest;
    private boolean mIsWpcomAccountWith2FA = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wp_dot_com_login_activity);
        getSupportActionBar().hide();

        if (getIntent().hasExtra(JETPACK_AUTH_REQUEST))
            mIsJetpackAuthRequest = true;

        mSignInButton = (Button) findViewById(R.id.saveDotcom);
        mSignInButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
                EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

                mUsername = dotcomUsername.getText().toString();
                mPassword = dotcomPassword.getText().toString();

                if (mUsername.equals("") || mPassword.equals("")) {
                    dotcomUsername.setError(getString(R.string.username_password_required));
                    dotcomPassword.setError(getString(R.string.username_password_required));
                } else {
                    new SignInTask().execute();
                }
            }
        });

        TextView wpcomHelp = (TextView) findViewById(R.id.wpcomHelp);
        wpcomHelp.setOnClickListener(new TextView.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://android.wordpress.org/faq"));
                startActivity(intent);

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
    }

    private class SignInTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            mSignInButton.setText(getString(R.string.attempting_configure));
            mSignInButton.setEnabled(false);
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
            Object[] signInParams = { mUsername, mPassword };
            try {
                client.call("wp.getUsersBlogs", signInParams);
                Blog blog = WordPress.getCurrentBlog();
                if (blog != null) {
                    blog.setDotcom_username(mUsername);
                    blog.setDotcom_password(mPassword);
                }

                // Don't change global WP.com settings if this is Jetpack auth request from stats
                if (!mIsJetpackAuthRequest) {
                    //New wpcom credetials inserted here. Reset the app state: there is the possibility a different username/password is inserted here
                    WordPress.removeWpComUserRelatedData(WPComLoginActivity.this);

                    Editor settings = PreferenceManager.getDefaultSharedPreferences(WPComLoginActivity.this).edit();
                    settings.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
                    settings.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, WordPressDB.encryptPassword(mPassword));
                    settings.commit();

                    //Make sure to update credentials for .wpcom blog even if currentBlog is null
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
            }
            catch (XMLRPCFault xmlRpcFault) {
                AppLog.e(T.NUX, "XMLRPCFault received from XMLRPC call wp.getUsersBlogs", xmlRpcFault);
                if (xmlRpcFault.getFaultCode() == 425) {
                    mIsWpcomAccountWith2FA = true;
                }
                return false;
            } catch (Exception e) {
                AppLog.e(T.NUX, "Exception received from XMLRPC call wp.getUsersBlogs", e);
                mIsWpcomAccountWith2FA = false;
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSignedIn) {
            if (isSignedIn && !isFinishing()) {
                if (!mIsJetpackAuthRequest) {
                    WordPress.getRestClientUtils().get("me", new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            WPComLoginActivity.this.setResult(RESULT_OK);
                            //Register the device again for Push Notifications
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
                String errorMessage = mIsWpcomAccountWith2FA ? getString(R.string.account_two_step_auth_enabled) : getString(R.string.nux_cannot_log_in);
                Toast.makeText(getBaseContext(),errorMessage, Toast.LENGTH_LONG).show();
                mSignInButton.setEnabled(true);
                mSignInButton.setText(R.string.sign_in);
            }
        }
    }
}
