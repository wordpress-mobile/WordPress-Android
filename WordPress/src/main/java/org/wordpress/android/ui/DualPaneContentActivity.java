package org.wordpress.android.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.util.DualPaneHelper;

import de.greenrobot.event.EventBus;

/**
 * Abstract activity that serves as a container for content fragment in single pane mode.
 */
public abstract class DualPaneContentActivity extends AppCompatActivity {

    public final static String ARG_LAUNCHED_FROM_DUAL_PANE_HOST = "launched_from_dual_pane_dashboard";
    public final static String FRAGMENT_STATE_KEY = "fragment_state";
    public final static String RETAINED_FRAGMENT_STATE_KEY = "retained_fragment_state";

    private Fragment.SavedState mFragmentSavedState;

    /**
     * @return tag of fragment that this activity hosts. Used only internaly and can be anything you like.
     */
    protected abstract String getContentFragmentTag();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentSavedState = getIntent().getParcelableExtra(FRAGMENT_STATE_KEY);

        boolean isLaunchedFromDualPaneDashboard = getIntent().getBooleanExtra(ARG_LAUNCHED_FROM_DUAL_PANE_HOST, false);

        // When activity launched from dual pane host gets recreated in dual pane mode it calls finish(), but before that,
        // it broadcasts retained fragment state using EventBus. Dual pane host get's it and use's it to show fragment in
        // dual pane mode.
        if (DualPaneHelper.isInDualPaneConfiguration(this) && isLaunchedFromDualPaneDashboard) {

            if (savedInstanceState != null && getHostedFragment() != null) {
                Fragment.SavedState savedState = savedInstanceState.getParcelable(RETAINED_FRAGMENT_STATE_KEY);
                EventBus.getDefault().postSticky(new DualPaneContentState(getIntent(), getHostedFragment().getClass(),
                        savedState));
            }

            finish();
            //no transitions are allowed when the activity is switching to single pane mode
            overridePendingTransition(0, 0);
        }
    }

    private Fragment getHostedFragment() {
        return getSupportFragmentManager().findFragmentByTag(getContentFragmentTag());
    }

    protected Fragment.SavedState getFragmentSavedState() {
        return mFragmentSavedState;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Fragment hostedFragment = getHostedFragment();

        if (hostedFragment != null) {
            Fragment.SavedState savedState = getSupportFragmentManager().saveFragmentInstanceState(hostedFragment);
            outState.putParcelable(RETAINED_FRAGMENT_STATE_KEY, savedState);
        }
    }
}
