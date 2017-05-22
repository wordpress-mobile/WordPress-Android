package org.wordpress.android.networking;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

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
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    protected void loadContent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(CERT_DETAILS_KEYS)) {
            String certDetails = extras.getString(CERT_DETAILS_KEYS);
            StringBuilder sb = new StringBuilder("<html><body>");
            sb.append(certDetails);
            sb.append("</body></html>");
            mWebView.loadDataWithBaseURL(null, sb.toString(), "text/html", "utf-8", null);
        }
    }

    @Override
    protected void configureWebView() {
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");
    }
}
