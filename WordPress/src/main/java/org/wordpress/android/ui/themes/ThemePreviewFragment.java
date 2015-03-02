package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.WPWebChromeClient;
import org.wordpress.android.util.WPWebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A fragment to display a preview of the theme being applied on a blog.
 *
 */
public class ThemePreviewFragment extends Fragment {
    public static final String TAG = ThemePreviewFragment.class.getName();
    private static final String ARGS_THEME_ID = "theme_id";
    private static final String ARGS_PREVIEW_URL = "preview_url";

    // sample desktop user-agent to force desktop view of site
    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";


    private ThemePreviewFragmentCallback mCallback;
    private WebView mWebView;
    private Blog mBlog;
    private String mThemeId;
    private String mPreviewURL;

    public interface ThemePreviewFragmentCallback {
        public void onResume(Fragment fragment);
        public void onPause(Fragment fragment);
        public void onActivateThemeClicked(String themeId, Fragment fragment);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        mCallback.onResume(this);
        setHasOptionsMenu(true);
        setMenuVisibility(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPause(this);
    }

    public String getThemeId() {
        if (mThemeId != null) {
            return mThemeId;
        } else if (getArguments() != null) {
            mThemeId = getArguments().getString(ARGS_THEME_ID);
            return mThemeId;
        } else {
            return null;
        }
    }

    private String getPreviewURL() {
        if (mPreviewURL != null) {
            return mPreviewURL;
        } else if (getArguments() != null) {
            mPreviewURL = getArguments().getString(ARGS_PREVIEW_URL);
            return mPreviewURL;
        } else {
            return null;
        }
    }

    public void load(String themeId, String previewURL) {
        mThemeId = themeId;
        mPreviewURL = previewURL;
        refreshViews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBlog = WordPress.getCurrentBlog();
        String previewURL = getPreviewURL();

        if (previewURL == null || mBlog == null)
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();

        View view = inflater.inflate(R.layout.webview, container, false);

        mWebView = (WebView) view.findViewById(R.id.webView);
        mWebView.getSettings().setUserAgentString(DESKTOP_UA);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        mWebView.setWebChromeClient(
                new WPWebChromeClient(
                        getActivity(),
                        (ProgressBar) view.findViewById(R.id.progress_bar),
                        false));

        mWebView.setWebViewClient(new WPWebViewClient(mBlog));

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setJavaScriptEnabled(true);

        loadAuthenticatedUrl(previewURL);

        return view;
    }

    private void refreshViews() {
        loadAuthenticatedUrl(getPreviewURL());
    }


    /**
     * Login to the WordPress blog and load the specified URL.
     *
     * @param url URL to be loaded in the webview.
     */
    protected void loadAuthenticatedUrl(String url) {
        if (!isAdded() || mBlog == null || url == null) {
            return;
        }

        String authenticationUrl = WordPress.getLoginUrl(mBlog);
        String postData = WPWebViewActivity.getAuthenticationPostData(authenticationUrl, url,
                mBlog.getUsername(), mBlog.getPassword(), WordPress.getDotComToken(getActivity())
        );

        mWebView.postUrl(authenticationUrl, postData.getBytes());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.theme_preview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_activate:
                mCallback.onActivateThemeClicked(getThemeId(), ThemePreviewFragment.this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.menu_search);
    }
}
