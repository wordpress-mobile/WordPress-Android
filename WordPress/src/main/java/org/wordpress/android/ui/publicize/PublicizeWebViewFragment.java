package org.wordpress.android.ui.publicize;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.util.WebViewUtils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PublicizeWebViewFragment extends PublicizeBaseFragment {
    private SiteModel mSite;
    private String mServiceId;
    private int mConnectionId;
    private WebView mWebView;
    private ProgressBar mProgress;

    @Inject AccountStore mAccountStore;

    /*
     * returns a new webView fragment to connect to a publicize service - if passed connection
     * is non-null then we're reconnecting a broken connection, otherwise we're creating a
     * new connection to the service
     */
    public static PublicizeWebViewFragment newInstance(@NonNull SiteModel site,
                                                       @NonNull PublicizeService service,
                                                       PublicizeConnection connection) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        args.putString(PublicizeConstants.ARG_SERVICE_ID, service.getId());
        if (connection != null) {
            args.putInt(PublicizeConstants.ARG_CONNECTION_ID, connection.connectionId);
        }

        PublicizeWebViewFragment fragment = new PublicizeWebViewFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            mSite = (SiteModel) args.getSerializable(WordPress.SITE);
            mServiceId = args.getString(PublicizeConstants.ARG_SERVICE_ID);
            mConnectionId = args.getInt(PublicizeConstants.ARG_CONNECTION_ID);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mServiceId = savedInstanceState.getString(PublicizeConstants.ARG_SERVICE_ID);
            mConnectionId = savedInstanceState.getInt(PublicizeConstants.ARG_CONNECTION_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putInt(PublicizeConstants.ARG_CONNECTION_ID, mConnectionId);
        mWebView.saveState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_webview_fragment, container, false);

        mProgress = (ProgressBar) rootView.findViewById(R.id.progress);
        mWebView = (WebView) rootView.findViewById(R.id.webView);

        mWebView.setWebViewClient(new PublicizeWebViewClient());
        mWebView.setWebChromeClient(new PublicizeWebChromeClient());
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            mProgress.setVisibility(View.VISIBLE);
            loadConnectUrl();
        } else {
            mWebView.restoreState(savedInstanceState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setNavigationIcon(R.drawable.ic_close_white_24dp);
    }

    /*
     * display the current connect URL for this service - this will ask the user to
     * authorize the connection via the external service
     */
    private void loadConnectUrl() {
        if (!isAdded()) return;

        // connect url depends on whether we're connecting or reconnecting
        String connectUrl;
        if (mConnectionId != 0) {
            connectUrl = PublicizeTable.getRefreshUrlForConnection(mConnectionId);
        } else {
            connectUrl = PublicizeTable.getConnectUrlForService(mServiceId);
        }

        // request must be authenticated with wp.com credentials
        String postData = WPWebViewActivity.getAuthenticationPostData(
                WPWebViewActivity.WPCOM_LOGIN_URL,
                connectUrl,
                mAccountStore.getAccount().getUserName(),
                "",
                mAccountStore.getAccessToken());

        mWebView.postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, postData.getBytes());
    }

    // ********************************************************************************************

    private class PublicizeWebViewClient extends WebViewClient {

        PublicizeWebViewClient() {
            super();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // does this url denotes that we made it past the auth stage?
            if (isAdded() && url != null) {
                Uri uri = Uri.parse(url);
                if (uri.getHost().equals("public-api.wordpress.com")
                        && uri.getPath().equals("/connect/")
                        && uri.getQueryParameter("action").equals("verify")) {
                    // "denied" param will appear on failure or cancellation
                    String denied = uri.getQueryParameter("denied");
                    if (!TextUtils.isEmpty(denied)) {
                        EventBus.getDefault().post(new PublicizeEvents.ActionCompleted(false, ConnectAction.CONNECT));
                        return;
                    }

                    long currentUserId = mAccountStore.getAccount().getUserId();
                    // call the endpoint to make the actual connection
                    PublicizeActions.connect(mSite.getSiteId(), mServiceId, currentUserId);
                    WebViewUtils.clearCookiesAsync();
                }
            }
        }
    }

    private class PublicizeWebChromeClient extends WebChromeClient {

        PublicizeWebChromeClient() {
            super();
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress == 100 && isAdded()) {
                mProgress.setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (title != null && !title.startsWith("http")) {
                setTitle(title);
            }
        }
    }
}
