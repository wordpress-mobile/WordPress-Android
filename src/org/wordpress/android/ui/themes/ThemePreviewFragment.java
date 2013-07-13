package org.wordpress.android.ui.themes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;

public class ThemePreviewFragment extends Fragment {

    private static final String ARGS_THEME_ID = "theme_id";
    private static final String ARGS_PREVIEW_URL = "preview_url";
    
    // sample desktop user-agent to force desktop view of site 
    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";

    
    private ThemePreviewFragmentCallback mCallback;
    private WebView mWebView;
    private Blog mBlog;
    
    public interface ThemePreviewFragmentCallback {
        public void onResumeThemePreviewFragment();
        public void onPauseThemePreviewFragment();
    }
    
    public static ThemePreviewFragment newInstance(String themeId, String previewURL) {
        ThemePreviewFragment fragment = new ThemePreviewFragment();
        
        Bundle args = new Bundle();
        args.putString(ARGS_THEME_ID, themeId);
        args.putString(ARGS_PREVIEW_URL, previewURL);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (ThemePreviewFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ThemePreviewFragmentCallback");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mCallback.onResumeThemePreviewFragment();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPauseThemePreviewFragment();
    }
    
    public String getThemeId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_THEME_ID);
        else
            return null;
    }
    
    private String getPreviewURL() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_PREVIEW_URL);
        else
            return null;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mBlog = WordPress.getCurrentBlog();
        String previewURL = getPreviewURL();
        
        if (previewURL == null || mBlog == null)
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        
        View view = inflater.inflate(R.layout.webview, container, false);
        
        mWebView = (WebView) view.findViewById(R.id.webView);
        mWebView.getSettings().setUserAgentString(DESKTOP_UA);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.loadUrl(previewURL);
        
        mWebView.setWebViewClient(new WordPressWebViewClient(mBlog));

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setSavePassword(false);
        
        loadAuthenticatedUrl(previewURL);
        
        return view;
    }

    /**
     * Login to the WordPress blog and load the specified URL.
     *
     * @param url URL to be loaded in the webview.
     */
    protected void loadAuthenticatedUrl(String url) {

        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    mBlog.getUsername(), mBlog.getPassword(),
                    URLEncoder.encode(url, "UTF-8"));
            mWebView.postUrl(getLoginUrl(), postData.getBytes());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Get the URL of the WordPress login page.
     *
     * @return URL of the login page.
     */
    protected String getLoginUrl() {
        if (mBlog.getUrl().lastIndexOf("/") != -1) {
            return mBlog.getUrl().substring(0, mBlog.getUrl().lastIndexOf("/"))
                    + "/wp-login.php";
        } else {
            return mBlog.getUrl().replace("xmlrpc.php", "wp-login.php");
        }
    }
    
    /**
     * WebViewClient that is capable of handling HTTP authentication requests using the HTTP
     * username and password of the blog configured for this activity.
     */
    private class WordPressWebViewClient extends WebViewClient {
        private Blog blog;

        WordPressWebViewClient(Blog blog) {
            super();
            this.blog = blog;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            handler.proceed(blog.getHttpuser(), blog.getHttppassword());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {

            super.onReceivedError(view, errorCode, description, failingUrl);
        }

    }

}
