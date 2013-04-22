package org.wordpress.android.ui.reader;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.wordpress.android.R;

public class ReaderWebPageFragment extends ReaderBaseFragment {

    // private String cachedPage = null;
    public WebView wv;

    public static ReaderWebPageFragment newInstance() {
        ReaderWebPageFragment f = new ReaderWebPageFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.reader_web_page, container, false);
        wv = (WebView) v.findViewById(R.id.webView);
        this.setDefaultWebViewSettings(wv);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        wv.addJavascriptInterface(new JavaScriptInterface(getActivity()
                .getApplicationContext()), interfaceNameForJS);
        wv.setWebViewClient(new DetailWebViewClient());
        wv.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (getActivity() != null)
                    ((ReaderActivity)getActivity()).setSupportProgress(progress * 100);
            }
        });

        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (wv != null) {
            wv.stopLoading();
            wv.clearCache(true);
        }
    }

    protected class DetailWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            wv.requestLayout();
            super.onPageFinished(view, url);
        }
    }
}
