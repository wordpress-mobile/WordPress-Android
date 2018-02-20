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
import org.wordpress.android.ui.accounts.signup.SiteCreationService.SiteCreationState;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.SiteCreationStep;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground.ServiceEventConnection;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.URLFilteredWebViewClient;

import java.util.HashMap;

public class SiteCreationCreatingFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creating_fragment_tag";

    private static final String ARG_SITE_TITLE = "ARG_SITE_TITLE";
    private static final String ARG_SITE_TAGLINE = "ARG_SITE_TAGLINE";
    private static final String ARG_SITE_SLUG = "ARG_SITE_SLUG";
    private static final String ARG_SITE_THEME_ID = "ARG_SITE_THEME_ID";

    private static final String KEY_WEBVIEW_LOADED_IN_TIME = "KEY_WEBVIEW_LOADED_IN_TIME";
    private static final String KEY_TRACKED_SUCCESS = "KEY_TRACKED_SUCCESS";

    private ServiceEventConnection mServiceEventConnection;

    private ImageView mImageView;
    private View mProgressContainer;
    private View mErrorContainer;
    private Button mRetryButton;
    private View mCompletedContainer;
    private WebView mWebView;
    private View mTadaContainer;
    private TextView[] mLabels;

    private boolean mTrackedSuccess;
    private boolean mWebViewLoadedInTime;
    int mNewSiteLocalId;

    private PreviewWebViewClient mPreviewWebViewClient;

    public boolean isInModalMode() {
        SiteCreationState state = SiteCreationService.getState();
        return state != null && state.isInProgress();
    }

    public boolean isCreationSucceeded() {
        SiteCreationState state = SiteCreationService.getState();
        return state != null && SiteCreationService.getState().getStep() == SiteCreationStep.SUCCESS;
    }

    public boolean canGoBack() {
        SiteCreationState state = SiteCreationService.getState();
        if (state == null) {
            return true;
        }

        if (state.getStep() == SiteCreationStep.FAILURE) {
            state = (SiteCreationState) state.getPayload();
        }

        return !state.isAfterCreation();
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
        mRetryButton = rootView.findViewById(R.id.button_retry);
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
                    mSiteCreationListener.doConfigureSite(mNewSiteLocalId);
                }
            }
        });
        primaryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isAdded()) {
                    mSiteCreationListener.doWriteFirstPost(mNewSiteLocalId);
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
            // on first appearance start the Service to perform the site creation
            createSite();
        } else {
            mWebViewLoadedInTime = savedInstanceState.getBoolean(KEY_WEBVIEW_LOADED_IN_TIME, false);
            mTrackedSuccess = savedInstanceState.getBoolean(KEY_TRACKED_SUCCESS, false);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showHomeButton(!isInModalMode(), false);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CREATING_VIEWED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // connect to the Service. We'll receive updates via EventBus.
        mServiceEventConnection = new ServiceEventConnection(getContext(), SiteCreationService.class, this);
    }

    @Override
    public void onPause() {
        super.onPause();

        // disconnect from the Service
        mServiceEventConnection.disconnect(getContext(), this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_WEBVIEW_LOADED_IN_TIME, mWebViewLoadedInTime);
        outState.putBoolean(KEY_TRACKED_SUCCESS, mTrackedSuccess);
    }

    void createSite() {
        String siteTitle = getArguments().getString(ARG_SITE_TITLE);
        String siteTagline = getArguments().getString(ARG_SITE_TAGLINE);
        String siteSlug = getArguments().getString(ARG_SITE_SLUG);
        String themeId = getArguments().getString(ARG_SITE_THEME_ID);
        SiteCreationService.createSite(getContext(), siteTitle, siteTagline, siteSlug, themeId);
    }

    void retryFromState(SiteCreationState retryFromState, long newSiteRemoteId) {
        String siteTagline = getArguments().getString(ARG_SITE_TAGLINE);
        String themeId = getArguments().getString(ARG_SITE_THEME_ID);
        SiteCreationService.retryFromState(getContext(), retryFromState, newSiteRemoteId, siteTagline, themeId);
    }

    private void mutateToCompleted(boolean showWebView) {
        if (isAdded()) {
            hideActionbar();
            mProgressContainer.setVisibility(View.GONE);
            mCompletedContainer.setVisibility(View.VISIBLE);
            mWebView.setVisibility(showWebView ? View.VISIBLE : View.INVISIBLE);
            mTadaContainer.setVisibility(showWebView ? View.INVISIBLE : View.VISIBLE);
        }

        if (!mTrackedSuccess) {
            mTrackedSuccess = true;
            HashMap<String, Object> successProperties = new HashMap<>();
            successProperties.put("loaded_in_time", showWebView);
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_SUCCESS_VIEWED, successProperties);
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
        boolean mIsPageFinished;

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

    private void configureBackButton() {
        SiteCreationState currentState = SiteCreationService.getState();

        SiteCreationState failedOnState = null;
        if (currentState != null && currentState.getStep() == SiteCreationStep.FAILURE) {
            failedOnState = (SiteCreationState) currentState.getPayload();
        }

        boolean isInModal = currentState != null && currentState.isInProgress();
        boolean failedAfterCreation = failedOnState != null && failedOnState.isAfterCreation();
        showHomeButton(!isInModal, failedAfterCreation);
    }

    private void configureImage(boolean hasFailure) {
        mImageView.setImageResource(hasFailure ? R.drawable.img_site_error_camera_pencils_226dp
                : R.drawable.img_site_wordpress_camera_pencils_226dp);
    }

    private void handleFailure(final SiteCreationState failedState) {
        // update UI depending on which step the process failed so to properly offer retry options
        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (failedState == null) {
                    AppLog.d(T.NUX, "User retries site creation but failedState is null :(");
                    return;
                }

                AppLog.d(T.NUX, "User retries failed site creation on step: " + failedState.getStepName());
                if (failedState.isTerminal()) {
                    throw new RuntimeException("Internal inconsistency: Cannot resume site creation from "
                            + failedState.getStepName());
                } else if (failedState.getStep() == SiteCreationStep.IDLE
                        || failedState.getStep() == SiteCreationStep.NEW_SITE) {
                    createSite();
                } else {
                    retryFromState(failedState, (long) failedState.getPayload());
                }
            }
        });
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onSiteCreationStateUpdated(SiteCreationState event) {
        AppLog.i(T.NUX, "Received state: " + event.getStepName());

        mProgressContainer.setVisibility(View.VISIBLE);
        mErrorContainer.setVisibility(View.GONE);

        configureBackButton();

        switch (event.getStep()) {
            case IDLE:
                disableUntil(0);
                configureImage(false);
                break;
            case NEW_SITE:
                disableUntil(R.id.site_creation_creating_laying_foundation);
                configureImage(false);
                break;
            case FETCHING_NEW_SITE:
                disableUntil(R.id.site_creation_creating_fetching_info);
                configureImage(false);
                break;
            case SET_TAGLINE:
                disableUntil(R.id.site_creation_creating_configuring_content);
                configureImage(false);
                break;
            case SET_THEME:
                disableUntil(R.id.site_creation_creating_configuring_theme);
                configureImage(false);
                break;
            case FAILURE:
                configureImage(true);
                mProgressContainer.setVisibility(View.GONE);
                mErrorContainer.setVisibility(View.VISIBLE);
                handleFailure((SiteCreationState) event.getPayload());
                NetworkUtils.checkConnection(getContext());
                break;
            case PRELOAD:
                disableUntil(R.id.site_creation_creating_preparing_frontend);
                configureImage(false);
                mPreviewWebViewClient = loadWebview();
                break;
            case SUCCESS:
                mNewSiteLocalId = (Integer) event.getPayload();

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
