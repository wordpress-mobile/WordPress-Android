package org.wordpress.android.ui.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.PostsListFragment;

import de.greenrobot.event.EventBus;

public abstract class DualPaneContentActivity extends AppCompatActivity {

    public final static String ARG_LAUNCHED_FROM_DUAL_PANE_DASHBOARD = "launched_from_dual_pane_dashboard";

    public final static String FRAGMENT_STATE_KEY = "fragment_state";

    protected abstract String getContentFragmentTag();

    private Fragment.SavedState mFragmentSavedState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentSavedState = getIntent().getParcelableExtra(FRAGMENT_STATE_KEY);
        boolean isLaunchedFromDualPaneDashboard = getIntent().getBooleanExtra(ARG_LAUNCHED_FROM_DUAL_PANE_DASHBOARD, false);

        if (isInDualPaneMode() && isLaunchedFromDualPaneDashboard) {
            if (savedInstanceState != null) {
                Fragment.SavedState fragmentState = savedInstanceState.getParcelable(FRAGMENT_STATE_KEY);

                if (fragmentState != null) {
                    EventBus.getDefault().postSticky(new DualPaneContentState(getIntent(), PostsListFragment
                            .class, fragmentState));
                }
            }

            finish();
        }
    }

    protected Fragment.SavedState getFragmentSavedState() {
        return mFragmentSavedState;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();

        Fragment contentFragment = fm.findFragmentByTag(getContentFragmentTag());
        if (contentFragment != null) {
            Fragment.SavedState savedState = fm.saveFragmentInstanceState(contentFragment);

            outState.putParcelable(FRAGMENT_STATE_KEY, savedState);
        }
    }

    protected boolean isInDualPaneMode() {
        return getResources().getBoolean(R.bool.dual_pane);
    }
}
