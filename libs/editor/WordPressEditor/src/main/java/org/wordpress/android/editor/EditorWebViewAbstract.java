package org.wordpress.android.editor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A text editor WebView with support for JavaScript execution.
 */
public abstract class EditorWebViewAbstract extends WebView {
    public abstract void execJavaScriptFromString(String javaScript);

    public static final int REQUEST_TIMEOUT_MS = 30000;

    private OnImeBackListener mOnImeBackListener;

    private Map<String, String> mHeaderMap = new HashMap<>();

    public EditorWebViewAbstract(Context context, AttributeSet attrs) {
        super(context, attrs);
        configureWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        this.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                AppLog.e(AppLog.T.EDITOR, description);
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (mHeaderMap.size() > 0) {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                        conn.setReadTimeout(REQUEST_TIMEOUT_MS);
                        conn.setConnectTimeout(REQUEST_TIMEOUT_MS);

                        // Keep any existing request headers from the WebResourceRequest
                        for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }

                        for (Map.Entry<String, String> entry : mHeaderMap.entrySet()) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                        return new WebResourceResponse(conn.getContentType(), conn.getContentEncoding(),
                                conn.getInputStream());
                    } catch (IOException e) {
                        AppLog.e(AppLog.T.EDITOR, e);
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }

            /**
             * Compatibility method for API < 21
             */
            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                // Intercept requests for private images and add the WP.com authorization header
                if (mHeaderMap.size() > 0) {
                    try {
                        // Force HTTPS usage if an authorization header was specified
                        if (mHeaderMap.keySet().contains("Authorization")) {
                            url = UrlUtils.makeHttps(url);
                        }
                        URL imageUrl = new URL(url);

                        HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                        conn.setReadTimeout(REQUEST_TIMEOUT_MS);
                        conn.setConnectTimeout(REQUEST_TIMEOUT_MS);

                        for (Map.Entry<String, String> entry : mHeaderMap.entrySet()) {
                            conn.setRequestProperty(entry.getKey(), entry.getValue());
                        }
                        return new WebResourceResponse(conn.getContentType(), conn.getContentEncoding(),
                                conn.getInputStream());
                    } catch (IOException e) {
                        AppLog.e(AppLog.T.EDITOR, e);
                    }
                }

                return super.shouldInterceptRequest(view, url);
            }
        });

        this.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(@NonNull ConsoleMessage cm) {
                AppLog.d(AppLog.T.EDITOR, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                AppLog.d(AppLog.T.EDITOR, message);
                return true;
            }
        });
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    public void setOnImeBackListener(OnImeBackListener listener) {
        mOnImeBackListener = listener;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mOnImeBackListener != null) {
                mOnImeBackListener.onImeBack();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setCustomHeader(String name, String value) {
        mHeaderMap.put(name, value);
    }
}
