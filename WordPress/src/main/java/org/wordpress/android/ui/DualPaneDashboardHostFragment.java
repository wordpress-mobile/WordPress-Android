package org.wordpress.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.util.DualPaneContentState;

import de.greenrobot.event.EventBus;

/**
 * Abstract fragment that implements core Dual Pane layout functionality
 */
public abstract class DualPaneDashboardHostFragment extends Fragment implements DualPaneDashboard {

    private final static String TAG = DualPaneDashboardHostFragment.class.getSimpleName();

    private final static int SIDEBAR_FRAGMENT_CONTAINER_ID = R.id.sidebar_fragment_container;
    private final static int CONTENT_FRAGMENT_CONTAINER_ID = R.id.content_fragment_container;

    private final static String SIDEBAR_FRAGMENT_TAG = "sidebar_fragment";
    private final static String DEFAULT_FRAGMENT_TAG = "default_fragment";

    private final static String CONTENT_FRAGMENT_TAG_PARAMETER_KEY = "content_fragment_tag";
    private final static String CONTENT_FRAGMENT_STATE_PARAMETER_KEY = "single_pane_content_fragment_is_hidden";

    private String mContentFragmentTag;
    private boolean mIsSinglePaneContentFragmentHidden = false;

    private DualPaneContentState mContentState;

    protected abstract Fragment initializeSidebarFragment();

    protected abstract Fragment initializeDefaultFragment();

    protected abstract boolean shouldOpenActivityAfterSwitchToSinglePane();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mContentFragmentTag = savedInstanceState.getString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY);
            mIsSinglePaneContentFragmentHidden = savedInstanceState.getBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY);
            mContentState = savedInstanceState.getParcelable(DualPaneContentState.KEY);
            Log.v(TAG, "Dashboard state was restored");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        showSidebarFragment();
        showContent();
    }

    private void showSidebarFragment() {
        Log.v(TAG, "Showing sidebar fragment");
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment sidebarFragment = getSidebarFragment();

        if (sidebarFragment == null) {
            throw new IllegalArgumentException("You need to provide a Fragment through initializeSidebarFragment()");
        }

        if (!sidebarFragment.isAdded()) {
            Log.v(TAG, "Adding sidebar fragment.");
            fragmentTransaction.replace(SIDEBAR_FRAGMENT_CONTAINER_ID, sidebarFragment, SIDEBAR_FRAGMENT_TAG);
        } else if (sidebarFragment.isDetached()) {
            Log.v(TAG, "Sidebar fragment already exist. Attaching to layout.");
            fragmentTransaction.attach(sidebarFragment);
        }

        fragmentTransaction.commit();
    }

    protected Fragment getSidebarFragment() {
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(SIDEBAR_FRAGMENT_TAG);

        if (sidebarFragment == null) {
            sidebarFragment = initializeSidebarFragment();
            Log.v(TAG, "Sidebar fragment does not exist, initializing new one.");
        }

        return sidebarFragment;
    }

    private void showContent() {
        //If we came into dual pane mode from activity, we would use fresh content state that activity has provided
        if (stickyDualPaneActivityStateAvailable()) {
            Log.v(TAG, "Pending content state exist.");
            mContentState = getStickyContentState();
            EventBus.getDefault().removeStickyEvent(DualPaneContentState.class);
        }

        if (mContentState != null) {
            if (isInDualPaneMode()) {
                Log.v(TAG, "Dashboard is in dual pane mode.");
                removeDefaultContentFragment();
                showContentFragment();
                mIsSinglePaneContentFragmentHidden = false;
            } else {
                Log.v(TAG, "Dashboard is in single pane mode.");
                //Do nothing if no Intent was provided.
                if (!mContentState.isActivityIntentAvailable()) {
                    Log.v(TAG, "No intent to start single pane activity.");
                    return;
                }
                if (shouldOpenActivityAfterSwitchToSinglePane() && !mIsSinglePaneContentFragmentHidden) {
                    Log.v(TAG, "Starting single pane activity.");
                    openContentActivity();
                    onContentActivityStarted();
                } else {
                    Log.v(TAG, "Cant start single pane activity yet.");
                    mIsSinglePaneContentFragmentHidden = true;
                }
            }
        } else {
            Log.v(TAG, "No saved content state.");
            showDefaultContentFragment();
        }
    }

    private boolean stickyDualPaneActivityStateAvailable() {
        return EventBus.getDefault().getStickyEvent(DualPaneContentState.class) != null;
    }

    private DualPaneContentState getStickyContentState() {
        return EventBus.getDefault().getStickyEvent(DualPaneContentState.class);
    }

    private void removeDefaultContentFragment() {
        Fragment defaultFragment = getChildFragmentManager().findFragmentByTag(DEFAULT_FRAGMENT_TAG);
        if (defaultFragment != null) {
            Log.v(TAG, "Removing deafault fragment.");
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.remove(defaultFragment);
            fragmentTransaction.commit();
        }
    }

    private void showDefaultContentFragment() {
        if (!isInDualPaneMode()) return;

        Fragment defaultFragment = getChildFragmentManager().findFragmentByTag(DEFAULT_FRAGMENT_TAG);

        if (defaultFragment == null) {
            defaultFragment = initializeDefaultFragment();
        }

        //Do nothing if there is no default fragment.
        if (defaultFragment == null) return;

        Log.v(TAG, "Showing default fragment.");

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, defaultFragment, DEFAULT_FRAGMENT_TAG);
        fragmentTransaction.commit();
    }

    @Nullable
    private Fragment getAttachedContentFragment() {
        return getChildFragmentManager().findFragmentByTag(mContentFragmentTag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // we are using generic dual pane fragment layout, later if the need will arise we could pass custom layout along
        // with sidebar/container ID's
        return inflater.inflate(R.layout.dual_pane_dashboard_fragment, container, false);
    }

    private void showContentFragment() {
        Log.v(TAG, "Showing restored content fragment.");
        mContentFragmentTag = mContentState.getFragmentClass().getSimpleName();

        Fragment fragment = Fragment.instantiate(getActivity(), mContentState.getFragmentClass().getName(), null);

        fragment.setInitialSavedState(mContentState.getFragmentState());

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
        fragmentTransaction.commit();
    }

    private void openContentActivity() {
        Intent intent = mContentState.getOriginalIntent();
        intent.putExtra(DualPaneContentActivity.FRAGMENT_STATE_KEY, mContentState.getFragmentState());
        startActivity(intent);
    }

    @Override
    public void showContentInDashboard(Class contentFragmentClass, Intent intent) {
        if (!isInDualPaneMode()) return;

        if (contentFragmentClass == null || !Fragment.class.isAssignableFrom(contentFragmentClass)) {
            throw new IllegalArgumentException("You need to pass a Fragment class as a parameter.");
        }

        Log.v(TAG, "Showing content in single pane.");

        mContentState = new DualPaneContentState(intent, contentFragmentClass, null);
        mContentFragmentTag = contentFragmentClass.getSimpleName();

        Bundle parameters = intent.getExtras();

        Fragment fragment = getChildFragmentManager().findFragmentByTag(mContentFragmentTag);

        if (fragment == null) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragment = Fragment.instantiate(getActivity(), contentFragmentClass.getName(), parameters);
            fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
            // Remember that commit() is an async method! Fragment might not be immediately available after this call.
            fragmentTransaction.commit();
        }

        mIsSinglePaneContentFragmentHidden = false;
    }

    @Override
    public void onContentActivityStarted() {
        Log.v(TAG, "Single pane content activity started.");
        mIsSinglePaneContentFragmentHidden = false;
        removeContentFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //pass onActivityResult to fragments inside the dashboard.
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(SIDEBAR_FRAGMENT_TAG);
        sidebarFragment.onActivityResult(requestCode, resultCode, data);

        Fragment contentFragment = getAttachedContentFragment();
        if (contentFragment != null) {
            contentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean isInDualPaneMode() {
        return getResources().getBoolean(R.bool.dual_pane);
    }

    @Override
    public void removeContentFragment() {
        Fragment contentFragment = getAttachedContentFragment();

        if (contentFragment != null) {
            Log.v(TAG, "Removing content fragment.");
            FragmentManager fragmentManager = getChildFragmentManager();
            fragmentManager.beginTransaction().remove(contentFragment).commit();
        }
        resetContentPaneState();
    }

    private void resetContentPaneState() {
        mContentFragmentTag = null;
        mIsSinglePaneContentFragmentHidden = false;
        mContentState = null;
    }

    @Nullable
    @Override
    public Fragment getContentPaneFragment() {
        return getAttachedContentFragment();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "Saving dashboard content state.");
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY, mContentFragmentTag);
        outState.putBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY, mIsSinglePaneContentFragmentHidden);

        Fragment contentFragment = getAttachedContentFragment();
        if (contentFragment != null && mContentState != null) {
            SavedState savedState = getChildFragmentManager().saveFragmentInstanceState(contentFragment);
            mContentState.setFragmentState(savedState);
            outState.putParcelable(DualPaneContentState.KEY, mContentState);
        }
        outState.putParcelable(DualPaneContentState.KEY, mContentState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    //Content activity is about to be closed and and passing it's fragment state
    public void onEvent(DualPaneContentState event) {
        mContentState = event;
    }
}
