package org.wordpress.android.ui.accounts.signup;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AutoForeground;
import org.wordpress.android.util.AutoForeground.ServiceClient;
import org.wordpress.android.util.WeakHandler.MessageListener;

public class SiteCreatingFragment extends Fragment implements MessageListener  {
    public static final String TAG = "site_creating_fragment_tag";

    private ServiceClient mServiceClient;

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
    public void onPause() {
        super.onPause();

        mServiceClient.disconnect(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        mServiceClient = new ServiceClient(getContext(), SiteCreationService.class, this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case AutoForeground.MSG_CURRENT_STATE:
                SiteCreationService.SiteCreationPhase state = (SiteCreationService.SiteCreationPhase) msg.obj;
                AppLog.i(T.NUX, "Received state: " + state.name());

                switch (state) {
                    case IDLE:
                        SiteCreationService.createSite(getActivity(),
                                WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_TITLE"),
                                WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_TAGLINE"),
                                WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_SLUG"),
                                WordPress.getBuildConfigString(getActivity(), "DEBUG_DOTCOM_NEW_SITE_THEME"));

                        mLabel.setText(R.string.site_creating_label);
                        break;
                    case NEW_SITE:
                        // nothing special to do here, just waiting for the site creation result...
                        break;
                    case FETCHING_NEW_SITE:
                        mLabel.setText(R.string.site_creating_fetching_info);
                        break;
                    case SET_TAGLINE:
                        mLabel.setText(R.string.site_creating_set_tagline);
                        break;
                    case SET_THEME:
                        mLabel.setText(R.string.site_creating_set_theme);
                        break;
                    case FAILURE:
                        mLabel.setText(R.string.site_creating_failed);
                        break;
                    case SUCCESS:
                        mLabel.setText(R.string.site_creating_success);
                        break;
                }

                return true;
        }

        return false;
    }
}
