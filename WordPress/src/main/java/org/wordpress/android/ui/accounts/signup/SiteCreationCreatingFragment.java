package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.OnSiteCreationStateUpdated;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground.ServiceEventConnection;
import org.wordpress.android.util.ToastUtils;

public class SiteCreationCreatingFragment extends SiteCreationBaseFormFragment<SiteCreationListener> {
    public static final String TAG = "site_creating_fragment_tag";

    private ServiceEventConnection mServiceEventConnection;

    private TextView mLabelFoundation;
    private TextView mLabelFetching;
    private TextView mLabelContent;
    private TextView mLabelStyle;
    private TextView mLabelFrontend;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.site_creation_creating_screen;
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mLabelFoundation = (TextView) rootView.findViewById(R.id.site_creation_creating_laying_foundation);
        mLabelFetching = (TextView) rootView.findViewById(R.id.site_creation_creating_fetching_info);
        mLabelContent = (TextView) rootView.findViewById(R.id.site_creation_creating_configuring_content);
        mLabelStyle = (TextView) rootView.findViewById(R.id.site_creation_creating_configuring_theme);
        mLabelFrontend = (TextView) rootView.findViewById(R.id.site_creation_creating_preparing_frontend);
    }

    @Override
    protected void onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpSiteCreatingScreen();
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED);
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteCreationPhaseUpdated(OnSiteCreationStateUpdated event) {
        AppLog.i(T.NUX, "Received state: " + event.getState().name());

        switch (event.getState()) {
            case IDLE:
                mLabelFoundation.setEnabled(true);
                break;
            case NEW_SITE:
                // nothing special to do here, just waiting for the site creation result...
                mLabelFoundation.setEnabled(true);
                break;
            case FETCHING_NEW_SITE:
                mLabelFoundation.setEnabled(true);
                mLabelFetching.setEnabled(true);
                break;
            case SET_TAGLINE:
                mLabelFoundation.setEnabled(true);
                mLabelFetching.setEnabled(true);
                mLabelContent.setEnabled(true);
                break;
            case SET_THEME:
                mLabelFoundation.setEnabled(true);
                mLabelFetching.setEnabled(true);
                mLabelContent.setEnabled(true);
                mLabelStyle.setEnabled(true);
                break;
            case FAILURE:
                ToastUtils.showToast(getContext(), R.string.site_creation_creating_failed);
                break;
            case SUCCESS:
                mLabelFoundation.setEnabled(true);
                mLabelFetching.setEnabled(true);
                mLabelContent.setEnabled(true);
                mLabelStyle.setEnabled(true);
                mLabelFrontend.setEnabled(true);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSiteCreationListener.creationSuccess();
                    }
                }, 4000);
                break;
        }
    }
}
