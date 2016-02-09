package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.ToastUtils;

public class LearnMorePreference extends Preference
        implements PreferenceHint, View.OnClickListener {
    private static final String WP_SUPPORT_URL = "https://en.support.wordpress.com/settings/discussion-settings/#default-article-settings";
    private static final String SUPPORT_MOBILE_ID = "mobile-only-usage";
    private static final String SUPPORT_CONTENT_JS = "javascript:(function(){" +
            "var mobileSupport = document.getElementById('" + SUPPORT_MOBILE_ID + "');" +
            "mobileSupport.style.display = 'inline';" +
            "var newHtml = '<body><h2>Discussion Settings</h2><' + " +
            "mobileSupport.tagName + ' style=\"font-size:medium;\">' + mobileSupport.innerHTML + '</' + mobileSupport.tagName + '></body>';" +
            "document.body.innerHTML = newHtml;" +
            "document.body.style.paddingLeft='32px';" +
            "document.body.style.marginLeft='32px';" +
            "document.body.style.paddingTop='0px';" +
            "document.body.style.paddingRight='32px';" +
            "document.body.style.paddingBottom='24px';" +
            "})();";

    private String mHint;
    private Dialog mDialog;

    public LearnMorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(@NonNull ViewGroup parent) {
        super.onCreateView(parent);

        View view = View.inflate(getContext(), R.layout.learn_more_pref, null);
        view.findViewById(R.id.learn_more_button).setOnClickListener(this);

        return view;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            return new SavedState(super.onSaveInstanceState());
        }
        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {// per documentation, state is always non-null
            super.onRestoreInstanceState(state);
        } else {
            super.onRestoreInstanceState(((SavedState) state).getSuperState());
            showDialog();
        }
    }

    @Override
    public void onClick(View v) {
        if (mDialog != null) return;

        AnalyticsUtils.trackWithCurrentBlogDetails(
                AnalyticsTracker.Stat.SITE_SETTINGS_LEARN_MORE_CLICKED);
        showDialog();
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

    @SuppressLint("SetJavaScriptEnabled")
    private void showDialog() {
        Context context = getContext();
        final WebView webView = new WebView(context);
        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new LearnMoreClient());
        webView.loadUrl(WP_SUPPORT_URL);
        mDialog = new Dialog(context);
        mDialog.setTitle(R.string.site_settings_learn_more_dialog_title);
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                webView.stopLoading();
                mDialog = null;
            }
        });
        mDialog.setContentView(R.layout.learn_more_pref_screen);
        mDialog.show();
    }

    private static class SavedState extends BaseSavedState {
        public SavedState(Parcel source) { super(source); }

        public SavedState(Parcelable superState) { super(superState); }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) { return new SavedState(in); }

                    public SavedState[] newArray(int size) { return new SavedState[size]; }
                };
    }

    private class LearnMoreClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            return !WP_SUPPORT_URL.equals(url) && !SUPPORT_CONTENT_JS.equals(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);

            if (mDialog != null && mDialog.isShowing()) {
                ToastUtils.showToast(getContext(), R.string.could_not_load_page);
                mDialog.dismiss();
            }
        }

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);
            if (mDialog != null) {
                AnalyticsUtils.trackWithCurrentBlogDetails(
                        AnalyticsTracker.Stat.SITE_SETTINGS_LEARN_MORE_LOADED);
                webView.loadUrl(SUPPORT_CONTENT_JS);
                mDialog.setContentView(webView);
            }
        }
    }
}
