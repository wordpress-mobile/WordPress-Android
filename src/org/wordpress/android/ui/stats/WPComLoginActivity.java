package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gcm.GCMRegistrar;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.xmlrpc.android.WPComXMLRPCApi;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

/**
 * An activity to let the user specify their WordPress.com credentials.
 * Should be used to get WordPress.com credentials for JetPack integration in self-hosted sites.
 */
public class WPComLoginActivity extends SherlockFragmentActivity {

    public static final int REQUEST_CODE = 5000;
    private String mUsername;
    private String mPassword;
    private Button mSignInButon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wp_dot_com_login_activity);
        getSupportActionBar().hide();

        mSignInButon = (Button) findViewById(R.id.saveDotcom);
        mSignInButon.setOnClickListener(new Button.OnClickListener() {
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
                intent.setData(Uri.parse("http://jetpack.me/about"));
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
            mSignInButon.setText(getString(R.string.attempting_configure));
            mSignInButon.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            XMLRPCClient client = new XMLRPCClient(Constants.wpcomXMLRPCURL, "", "");
            Object[] signInParams = { mUsername, mPassword };

            try {
                client.call("wp.getUsersBlogs", signInParams);
                if (WordPress.hasValidWPComCredentials(WPComLoginActivity.this)) {
                    // Sign out current user from all services
                    new WPComXMLRPCApi().unregisterWPComToken(
                            WPComLoginActivity.this,
                            GCMRegistrar.getRegistrationId(WPComLoginActivity.this));
                    try {
                        GCMRegistrar.checkDevice(WPComLoginActivity.this);
                        GCMRegistrar.unregister(WPComLoginActivity.this);
                    } catch (Exception e) {
                        Log.v("WORDPRESS", "Could not unregister for GCM: " + e.getMessage());
                    }
                }
                WordPress.restClient.clearAccessToken();
                WordPress.currentBlog.setDotcom_username(mUsername);
                WordPress.currentBlog.setDotcom_password(mPassword);
                WordPress.currentBlog.save(WordPress.currentBlog.getUsername());

                Editor settings = PreferenceManager.getDefaultSharedPreferences(WPComLoginActivity.this).edit();
                settings.putString(WordPress.WPCOM_USERNAME_PREFERENCE, mUsername);
                settings.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, mPassword);
                settings.commit();
                return true;
            } catch (XMLRPCException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSignedIn) {
            if (isSignedIn && !isFinishing()) {
                WordPress.restClient.get("me", new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        WPComLoginActivity.this.setResult(RESULT_OK);
                        finish();
                    }
                }, null);
            } else {
                Toast.makeText(getBaseContext(), getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
                mSignInButon.setEnabled(true);
                mSignInButon.setText(R.string.sign_in);
            }
        }
    }
}
