package org.wordpress.android.ui.reader;

import java.util.List;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;

public class ReaderImplFragment extends ReaderBaseFragment {
    /** Called when the activity is first created. */
    public WebView wv;
    public String topicsID;
    private PostSelectedListener onPostSelectedListener;
    private LoadDetailListener loadDetailListener;
    public TextView topicTV;

    public static ReaderImplFragment newInstance() {
        ReaderImplFragment f = new ReaderImplFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Make sure WP.com auth cookie is cleared out
        CookieSyncManager.createInstance(this.getActivity().getApplicationContext());
        CookieManager.getInstance().removeAllCookie();
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.reader_wpcom, container, false);

        wv = (WebView) v.findViewById(R.id.webView);
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wv.addJavascriptInterface(new JavaScriptInterface(getActivity()
                .getApplicationContext()), interfaceNameForJS);

        wv.setWebViewClient(new WebViewClient() {
            
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

                if(isAdded()){ // check if the fragment is currently added to its activity
                    if (url.equalsIgnoreCase(Constants.readerDetailURL)) {
                        view.stopLoading();
                        wv.loadUrl("javascript:Reader2.get_loaded_items();");
                        wv.loadUrl("javascript:Reader2.get_last_selected_item();");
                        onPostSelectedListener.onPostSelected(url);
                    } else {
                        ((ReaderActivity) getActivity()).startAnimatingButton();
                    }

                    if (url.contains("chrome=no")) {
                        loadDetailListener.onLoadDetail();
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (getActivity() != null && !getActivity().isFinishing())
                    ((ReaderActivity) getActivity()).stopAnimatingButton();
                wv.requestLayout();
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (getActivity() != null && !getActivity().isFinishing())
                    ((ReaderActivity) getActivity()).stopAnimatingButton();
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                /*
                 * if (url.equalsIgnoreCase(Constants.readerDetailURL)) {
                 * onPostSelectedListener.onPostSelected(url,
                 * WPCOMReaderImpl.this.cachedDetailPage); return true; }
                 */
                return false;
            }
        });

        this.setDefaultWebViewSettings(wv);
        
        WebSettings webSettings = wv.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCacheMaxSize(1024*1024*10);
        webSettings.setAppCachePath(getActivity().getApplicationContext().getCacheDir().getAbsolutePath());
        
        reloadReader();
        return v;
    }

    public void reloadReader() {
        new loadReaderTask().execute(null, null, null, null);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            onPostSelectedListener = (PostSelectedListener) activity;
            loadDetailListener = (LoadDetailListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
        if (wv != null) {
            wv.stopLoading();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }

    public void refreshReader() {
        wv.reload();
        new Thread(new Runnable() {
            public void run() {
                // refresh stat
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpProtocolParams.setUserAgent(
                            httpclient.getParams(), "wp-android-native");
                    String readerURL = Constants.readerURL
                            + "/?template=stats&stats_name=home_page_refresh";
                    if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
                        readerURL += "&per_page=20";
                    }

                    httpclient.execute(new HttpGet(readerURL));
                } catch (Exception e) {
                    // oh well
                }
            }
        }).start();
    }

    private class loadReaderTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            ((ReaderActivity) getActivity()).startAnimatingButton();
        }

        protected void onPostExecute(Void result) {

            if (getActivity() != null)
                ((ReaderActivity) getActivity()).stopAnimatingButton();
            // Read the WordPress.com cookies from the wv and pass them to the
            // connections below!
            CookieManager cookieManager = CookieManager.getInstance();
            final String cookie = cookieManager.getCookie("wordpress.com");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpProtocolParams.setUserAgent(httpclient.getParams(),
                                "wp-android-native");

                        String readerURL = Constants.readerURL
                                + "/?template=stats&stats_name=home_page";
                        HttpGet httpGet = new HttpGet(readerURL);
                        httpGet.setHeader("Cookie", cookie);
                        httpclient.execute(httpGet);

                    } catch (Exception e) {
                        // oh well
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        protected Void doInBackground(Void... args) {

            if (getActivity() == null || !WordPress.hasValidWPComCredentials(getActivity().getApplicationContext()) ){
                return null;
            }
            
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
            
            String readerURL = Constants.readerURL_v3;

            try {
                if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4) {
                    if (readerURL.contains("?"))
                        readerURL += "&per_page=20";
                    else
                        readerURL += "?per_page=20";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                final String responseContent = "<head>"
                        + "<script type=\"text/javascript\">"
                        + "function submitform(){document.loginform.submit();} </script>"
                        + "</head>"
                        + "<body onload=\"submitform()\">"
                        + "<form style=\"visibility:hidden;\" name=\"loginform\" id=\"loginform\" action=\""
                        + Constants.wpcomLoginURL
                        + "\" method=\"post\">"
                        + "<input type=\"text\" name=\"log\" id=\"user_login\" value=\""
                        + username
                        + "\"/></label>"
                        + "<input type=\"password\" name=\"pwd\" id=\"user_pass\" value=\""
                        + password
                        + "\" /></label>"
                        + "<input type=\"submit\" name=\"wp-submit\" id=\"wp-submit\" value=\"Log In\" />"
                        + "<input type=\"hidden\" name=\"redirect_to\" value=\""
                        + readerURL + "\" />" + "</form>" + "</body>";

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        wv.loadData(Uri.encode(responseContent), "text/html",
                                HTTP.UTF_8);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

    public interface ChangePageListener {
        public void onChangePage(int position);
    }

    public interface PostSelectedListener {
        public void onPostSelected(String requestedURL);
    }

    public interface ShowTopicsListener {
        public void showTopics();
    }

    public interface LoadDetailListener {
        public void onLoadDetail();
    }

}
