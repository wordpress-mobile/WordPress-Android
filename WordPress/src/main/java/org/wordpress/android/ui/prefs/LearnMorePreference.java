package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.util.ToastUtils;

public class LearnMorePreference extends Preference implements View.OnClickListener {
    private static final String SUPPORT_CONTENT_JS =
            "javascript:(function(){"
            + "var mobileSupport = document.getElementById('mobile-only-usage');"
            + "mobileSupport.style.display = 'inline';"
            + "var newHtml = '<' + mobileSupport.tagName + '>'"
            + " + mobileSupport.innerHTML + '</' + mobileSupport.tagName + '>';"
            + "document.body.innerHTML = newHtml;"
            + "document.body.setAttribute('style', 'padding:24px 24px 0px 24px !important');"

            + "}) ();";
    private static final String CONTENT_PADDING_JS =
            "javascript:(function(){"
            + "document.body.setAttribute('style', 'padding:24px 24px 0px 24px !important');"
            + "document.getElementById('mobilenav-toggle').style.display = 'none';"
            + "document.getElementById('actionbar').style.display = 'none';"
            + "}) ();";

    private Dialog mDialog;
    private String mUrl;
    private String mCaption;
    private boolean mUseCustomJsFormatting;

    public LearnMorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.LearnMorePreference);
        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.LearnMorePreference_url) {
                mUrl = array.getString(index);
            } else if (index == R.styleable.LearnMorePreference_useCustomJsFormatting) {
                mUseCustomJsFormatting = array.getBoolean(index, false);
            } else if (index == R.styleable.LearnMorePreference_caption) {
                int id = array.getResourceId(index, -1);
                if (id != -1) {
                    mCaption = array.getResources().getString(id);
                }
            }
        }
        array.recycle();
    }

    @Override
    protected View onCreateView(@NonNull ViewGroup parent) {
        super.onCreateView(parent);
        View view = View.inflate(getContext(), R.layout.learn_more_pref, null);
        view.setOnClickListener(this);

        if (!TextUtils.isEmpty(mCaption)) {
            TextView captionView = (TextView) view.findViewById(R.id.learn_more_caption);
            captionView.setText(mCaption);
            captionView.setVisibility(View.VISIBLE);
        }

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
    public void onRestoreInstanceState(@NonNull Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
        } else {
            super.onRestoreInstanceState(((SavedState) state).getSuperState());
            showDialog();
        }
    }

    @Override
    public void onClick(View v) {
        if (mDialog != null) {
            return;
        }
        AnalyticsTracker.track(Stat.SITE_SETTINGS_LEARN_MORE_CLICKED);
        showDialog();
    }

    private void showDialog() {
        final WebView webView = loadSupportWebView();
        mDialog = new Dialog(getContext());
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                webView.stopLoading();
                mDialog = null;
            }
        });
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.learn_more_pref_screen);
        WindowManager.LayoutParams params = mDialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;
        params.x = 12;
        params.y = 12;
        mDialog.getWindow().setAttributes(params);
        mDialog.show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView loadSupportWebView() {
        WebView webView = new WebView(getContext());
        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new LearnMoreClient());
        webView.loadUrl(mUrl);
        return webView;
    }

    private static class SavedState extends BaseSavedState {
        SavedState(Parcel source) {
            super(source);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    private class LearnMoreClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            // prevent loading clicked links
            return !mUrl.equals(url) && !SUPPORT_CONTENT_JS.equals(url) && !CONTENT_PADDING_JS.equals(url);
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
                AnalyticsTracker.track(Stat.SITE_SETTINGS_LEARN_MORE_LOADED);
                if (mUseCustomJsFormatting) {
                    webView.loadUrl(SUPPORT_CONTENT_JS);
                } else {
                    webView.loadUrl(CONTENT_PADDING_JS);
                }
                mDialog.setContentView(webView);
                webView.scrollTo(0, 0);
            }
        }
    }
}
