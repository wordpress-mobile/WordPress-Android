package org.wordpress.android.ui.notifications;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;

@SuppressLint("SetJavaScriptEnabled")
public class NotificationsWebViewActivity extends AuthenticatedWebViewActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUserAgentString(Constants.USER_AGENT);
     
        // load URL if one was provided in the intent
        String url = getIntent().getStringExtra("external_url");
        if (url != null) {
            loadUrl(url);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.menu_signout).setVisible(false);
        menu.findItem(R.id.menu_settings).setVisible(false);
        return true;
    }
}
