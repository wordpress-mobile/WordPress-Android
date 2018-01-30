package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.OnSiteCreationStateUpdated;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground.ServiceEventConnection;
import org.wordpress.android.util.URLFilteredWebViewClient;

public class SiteCreationCreatingFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creating_fragment_tag";

    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_TAGLINE = "ARG_SITE_TAGLINE";
    private static final String ARG_SITE_SLUG = "ARG_SITE_SLUG";
    private static final String ARG_SITE_THEME_ID = "ARG_SITE_THEME_ID";

    private static final String KEY_IN_MODAL_MODE = "KEY_IN_MODAL_MODE";
    private static final String KEY_CREATION_FINISHED = "KEY_CREATION_FINISHED";
    private static final String KEY_WEBVIEW_LOADED_IN_TIME = "KEY_WEBVIEW_LOADED_IN_TIME";

    private ServiceEventConnection mServiceEventConnection;

    private ImageView mImageView;
    private View mProgressContainer;
    private View mErrorContainer;
    private View mCompletedContainer;
    private WebView mWebView;
    private View mTadaContainer;
    private TextView[] mLabels;

    private boolean mInModalMode;
    private boolean mCreationSucceeded;
    private boolean mWebViewLoadedInTime;

    private PreviewWebViewClient mPreviewWebViewClient;

    public boolean isInModalMode() {
        return mInModalMode;
    }

    public boolean isCreationSucceeded() {
        return mCreationSucceeded;
    }

    public static SiteCreationCreatingFragment newInstance(String siteTitle, String siteTagline, String siteSlug,
            String themeId) {
        SiteCreationCreatingFragment fragment = new SiteCreationCreatingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SITE_TITLE, siteTitle);
        args.putString(ARG_SITE_TAGLINE, siteTagline);
        args.putString(ARG_SITE_SLUG, siteSlug);
        args.putString(ARG_SITE_THEME_ID, themeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_creating_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mImageView = rootView.findViewById(R.id.image);
        mProgressContainer = rootView.findViewById(R.id.progress_container);
        mErrorContainer = rootView.findViewById(R.id.error_container);
        mCompletedContainer = rootView.findViewById(R.id.completed_container);
        mWebView = rootView.findViewById(R.id.webview);
        mTadaContainer = rootView.findViewById(R.id.tada_container);

        // construct an array with the labels in reverse order
        mLabels = new TextView[] {
                rootView.findViewById(R.id.site_creation_creating_preparing_frontend),
                rootView.findViewById(R.id.site_creation_creating_configuring_theme),
                rootView.findViewById(R.id.site_creation_creating_configuring_content),
                rootView.findViewById(R.id.site_creation_creating_fetching_info),
                rootView.findViewById(R.id.site_creation_creating_laying_foundation)};
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isAdded()) {
                    mSiteCreationListener.doConfigureSite();
                }
            }
        });
        primaryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isAdded()) {
                    mSiteCreationListener.doWriteFirstPost();
                }
            }
        });
    }

    @Override
    protected void onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpSiteCreatingScreen();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            String siteTitle = getArguments().getString(ARG_SITE_TITLE);
            String siteTagline = getArguments().getString(ARG_SITE_TAGLINE);
            String siteSlug = getArguments().getString(ARG_SITE_SLUG);
            String themeId = getArguments().getString(ARG_SITE_THEME_ID);

            // on first appearance start the Service to perform the site creation
            mInModalMode = true;
            SiteCreationService.createSite(getContext(), siteTitle, siteTagline, siteSlug, themeId);
        } else {
            mInModalMode = savedInstanceState.getBoolean(KEY_IN_MODAL_MODE, false);
            mCreationSucceeded = savedInstanceState.getBoolean(KEY_CREATION_FINISHED, false);
            mWebViewLoadedInTime = savedInstanceState.getBoolean(KEY_WEBVIEW_LOADED_IN_TIME, false);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showHomeButton(!mInModalMode);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CREATING_VIEWED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mCreationSucceeded) {
            // connect to the Service. We'll receive updates via EventBus.
            mServiceEventConnection = new ServiceEventConnection(getContext(), SiteCreationService.class, this);
        } else {
            disableUntil(R.id.site_creation_creating_preparing_frontend);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mServiceEventConnection != null) {
            // disconnect from the Service
            mServiceEventConnection.disconnect(getContext(), this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IN_MODAL_MODE, mInModalMode);
        outState.putBoolean(KEY_CREATION_FINISHED, mCreationSucceeded);
        outState.putBoolean(KEY_WEBVIEW_LOADED_IN_TIME, mWebViewLoadedInTime);
    }

    private void mutateToCompleted(boolean showWebView) {
        if (isAdded()) {
            hideActionbar();
            mProgressContainer.setVisibility(View.GONE);
            mCompletedContainer.setVisibility(View.VISIBLE);
            mWebView.setVisibility(showWebView ? View.VISIBLE : View.INVISIBLE);
            mTadaContainer.setVisibility(showWebView ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private PreviewWebViewClient loadWebview() {
        String siteAddress = "https://" + getArguments().getString(ARG_SITE_SLUG) + ".wordpress.com";
        PreviewWebViewClient client = new PreviewWebViewClient(siteAddress);
        mWebView.setWebViewClient(client);
        mWebView.loadUrl(siteAddress);
        return client;
    }

    private static class PreviewWebViewClient extends URLFilteredWebViewClient {
        private boolean mIsPageFinished;

        PreviewWebViewClient(String siteAddress) {
            super(siteAddress);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            mIsPageFinished = true;
        }
    }

    private void disableUntil(@IdRes int textViewId) {
        boolean enabled = false;

        // traverse the array (elements are in "reverse" order already) and disable them until the provided on is reach.
        //  From that point on, enable the labels found
        for(TextView tv : mLabels) {
            if (tv.getId() == textViewId) {
                enabled = true;
            }

            tv.setEnabled(enabled);
        }
    }

    private void setModalMode(boolean inModalMode) {
        mInModalMode = inModalMode;
        showHomeButton(!mInModalMode);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onSiteCreationPhaseUpdated(OnSiteCreationStateUpdated event) {
        AppLog.i(T.NUX, "Received state: " + event.getState().name());

        mProgressContainer.setVisibility(View.VISIBLE);
        mErrorContainer.setVisibility(View.GONE);

        switch (event.getState()) {
            case IDLE:
                disableUntil(0);
                break;
            case NEW_SITE:
                disableUntil(R.id.site_creation_creating_laying_foundation);
                break;
            case FETCHING_NEW_SITE:
                disableUntil(R.id.site_creation_creating_fetching_info);
                break;
            case SET_TAGLINE:
                disableUntil(R.id.site_creation_creating_configuring_content);
                break;
            case SET_THEME:
                disableUntil(R.id.site_creation_creating_configuring_theme);
                break;
            case FAILURE:
                setModalMode(false);
                mImageView.setImageResource(R.drawable.img_site_error_camera_pencils_226dp);
                mProgressContainer.setVisibility(View.GONE);
                mErrorContainer.setVisibility(View.VISIBLE);
                break;
            case PRELOAD:
                disableUntil(R.id.site_creation_creating_preparing_frontend);
                mPreviewWebViewClient = loadWebview();
                break;
            case SUCCESS:
                mCreationSucceeded = true;
                setModalMode(false);

                if (mPreviewWebViewClient == null) {
                    // Apparently view got rotated while at the final so, just reconfigure the WebView.
                    loadWebview();
                } else {
                    mWebViewLoadedInTime = mPreviewWebViewClient.mIsPageFinished;
                }

                mutateToCompleted(mWebViewLoadedInTime);
                break;
        }
    }
}
