package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;

public class LearnMorePreference extends Preference
        implements PreferenceHint, View.OnClickListener, DialogInterface.OnDismissListener {
    private static final String WP_SUPPORT_URL = "https://en.support.wordpress.com/settings/discussion-settings/#default-article-settings";

    private String mHint;
    private Dialog mDialog;

    public LearnMorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(@NonNull ViewGroup parent) {
        super.onCreateView(parent);

        View view = View.inflate(getContext(), R.layout.learn_more_pref, null);
        view.findViewById(R.id.learnMoreButton).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        if (mDialog != null) return;

        Context context = getContext();
        Dialog dialog = mDialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnDismissListener(this);
        WebView webView = new WebView(context);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl(WP_SUPPORT_URL);
        dialog.setContentView(webView);
        dialog.show();
        webView.requestFocus();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
    }

    @Override
    public boolean hasHint() {
        return !TextUtils.isEmpty(mHint);
    }

    @Override
    public String getHint() {
        return mHint;
    }

    @Override
    public void setHint(String hint) {
        mHint = hint;
    }
}
