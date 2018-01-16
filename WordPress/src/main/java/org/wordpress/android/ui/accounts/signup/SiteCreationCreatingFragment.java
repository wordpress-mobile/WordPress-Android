package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.signup.SiteCreationService.OnSiteCreationStateUpdated;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground.ServiceEventConnection;

public class SiteCreationCreatingFragment extends Fragment  {
    public static final String TAG = "site_creating_fragment_tag";

    private ServiceEventConnection mServiceEventConnection;

    private TextView mLabel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.site_creating_screen, container, false);

        mLabel = (TextView) view.findViewById(R.id.label);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
                SiteCreationService.createSite(getActivity(),
                        WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_TITLE"),
                        WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_TAGLINE"),
                        WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_SLUG"),
                        WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_THEME"));

                mLabel.setText(R.string.site_creation_creating_label);
                break;
            case NEW_SITE:
                // nothing special to do here, just waiting for the site creation result...
                break;
            case FETCHING_NEW_SITE:
                mLabel.setText(R.string.site_creation_creating_fetching_info);
                break;
            case SET_TAGLINE:
                mLabel.setText(R.string.site_creation_creating_set_tagline);
                break;
            case SET_THEME:
                mLabel.setText(R.string.site_creation_creating_set_theme);
                break;
            case FAILURE:
                mLabel.setText(R.string.site_creation_creating_failed);
                break;
            case SUCCESS:
                mLabel.setText(R.string.site_creation_creating_success);
                break;
        }
    }
}
