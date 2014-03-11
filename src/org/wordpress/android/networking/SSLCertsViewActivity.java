package org.wordpress.android.networking;


import android.os.Bundle;
import android.webkit.WebSettings;

import com.actionbarsherlock.app.ActionBar;

import org.wordpress.android.R;
import org.wordpress.android.ui.WebViewActivity;

/**
 * Display details of a SSL cert
 */
public class SSLCertsViewActivity extends WebViewActivity {

    public static final String CERT_DETAILS_KEYS = "CertDetails";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getResources().getText(R.string.ssl_certificate_details));
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        mWebView.getSettings().setBuiltInZoomControls(false);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(CERT_DETAILS_KEYS)) {
            String certDetails = extras.getString(CERT_DETAILS_KEYS);
            StringBuilder sb = new StringBuilder("<html><body>");
            sb.append(certDetails);
            sb.append("</body></html>");
            WebSettings settings = mWebView.getSettings();
            settings.setDefaultTextEncodingName("utf-8");
            mWebView.loadDataWithBaseURL(null, sb.toString(), "text/html", "utf-8", null);
        }
    }
}