package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * An activity to let the user specify their WordPress.com credentials.
 * Should be used to get WordPress.com credentials for JetPack integration in self-hosted sites.
 */
public class WPComLoginActivity extends SherlockFragmentActivity {

    public static final int REQUEST_CODE = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wp_dot_com_login_activity);
        getSupportActionBar().hide();

        Button saveStatsLogin = (Button) findViewById(R.id.saveDotcom);
        saveStatsLogin.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                EditText dotcomUsername = (EditText) findViewById(R.id.dotcomUsername);
                EditText dotcomPassword = (EditText) findViewById(R.id.dotcomPassword);

                String dcUsername = dotcomUsername.getText().toString();
                String dcPassword = dotcomPassword.getText().toString();

                if (dcUsername.equals("") || dcPassword.equals("")) {
                    dotcomUsername.setError(getString(R.string.username_password_required));
                    dotcomPassword.setError(getString(R.string.username_password_required));
                } else {

                    WordPress.currentBlog.setDotcom_username(dcUsername);
                    WordPress.currentBlog.setDotcom_password(dcPassword);
                    WordPress.currentBlog.save(WordPress.currentBlog.getUsername());
                    
                    Editor settings = PreferenceManager.getDefaultSharedPreferences(WPComLoginActivity.this).edit();
                    settings.putString(WordPress.WPCOM_USERNAME_PREFERENCE, dcUsername);
                    settings.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, dcPassword);
                    settings.commit();
                    
                    WPComLoginActivity.this.setResult(RESULT_OK);
                    finish();
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
    
}
