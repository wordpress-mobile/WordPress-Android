package org.wordpress.android.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

import de.greenrobot.event.EventBus;

public abstract class DualPaneDashboardHostFragment extends Fragment implements DualPaneDashboard {

    private final static int SIDEBAR_FRAGMENT_CONTAINER_ID = R.id.sidebar_fragment_container;
    private final static int CONTENT_FRAGMENT_CONTAINER_ID = R.id.content_fragment_container;

    private final static String SIDEBAR_FRAGMENT_TAG = "my_sites";
    private final static String DEFAULT_FRAGMENT_TAG = "default_fragment";

    private final static String CONTENT_FRAGMENT_TAG_PARAMETER_KEY = "content_fragment_tag";
    private final static String CONTENT_FRAGMENT_STATE_PARAMETER_KEY = "single_pane_content_fragment_is_hidden";

    private String mContentFragmentTag;
    private boolean mIsSinglePaneContentFragmentHidden = false;

    private DualPaneContentState mDashboardContentState;

    protected abstract Fragment initializeSidebarFragment();

    protected abstract Fragment initializeDefaultFragment();

    protected abstract boolean shouldOpenActivityAfterSwitchToSinglePane();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mContentFragmentTag = savedInstanceState.getString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY);
            mIsSinglePaneContentFragmentHidden = savedInstanceState.getBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY);
            mDashboardContentState = savedInstanceState.getParcelable(DualPaneContentState.KEY);
        }

        showSidebarFragment();

        if (isContentAvailable()) {
            showContent();
        } else {
            if (isInDualPaneMode()) {
                showDefaultContentFragment();
            }
        }
    }

    protected Fragment getSidebarFragment() {
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(SIDEBAR_FRAGMENT_TAG);

        if (sidebarFragment == null) {
            sidebarFragment = initializeSidebarFragment();
        }

        return sidebarFragment;
    }

    private void removeDefaultContentFragment() {
        Fragment defaultFragment = getChildFragmentManager().findFragmentByTag(DEFAULT_FRAGMENT_TAG);
        if (defaultFragment != null) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.remove(defaultFragment);
            fragmentTransaction.commit();
        }
    }

    private void showDefaultContentFragment() {
        Fragment defaultFragment = getChildFragmentManager().findFragmentByTag(DEFAULT_FRAGMENT_TAG);

        if (defaultFragment == null) {
            defaultFragment = initializeDefaultFragment();
        }

        //no default fragment after all
        if (defaultFragment == null) return;

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, defaultFragment, DEFAULT_FRAGMENT_TAG);
        fragmentTransaction.commit();
    }

    //check if we have some content to present (be it in content pane or fullscreen activity)
    private boolean isContentAvailable() {
        return pendingDualPaneActivityStateAvailable() || isSavedContentStateAvailable();
    }

    private void showContent() {
        //If we come into dual pane mode from activity we should update content state that activity provided us before
        //finishing
        if (pendingDualPaneActivityStateAvailable()) {
            mDashboardContentState = getDuaPaneActivityState();
            EventBus.getDefault().removeStickyEvent(DualPaneContentState.class);
        }

        //Let's check if we have some content to show and present it
        if (isSavedContentStateAvailable()) {

            if (isInDualPaneMode()) {  //if we are in dual pane mode we will show content in content pane
                removeDefaultContentFragment();
                showContentFragment();
                mIsSinglePaneContentFragmentHidden = false;
            } else {  //if we are in single pane mode we will show content in activity
                if (shouldOpenActivityAfterSwitchToSinglePane() && !mIsSinglePaneContentFragmentHidden) {
                    //if we are not in
                    openContentActivity();
                    notifyContentActivityStarted();
                } else {
                    mIsSinglePaneContentFragmentHidden = true;
                }
            }
        }
    }

    @Nullable
    private Fragment getContentFragment() {
        return getChildFragmentManager().findFragmentByTag(mContentFragmentTag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dual_pane_dashboard_fragment, container, false);
    }

    private void showSidebarFragment() {
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment sidebarFragment = getSidebarFragment();

        if (sidebarFragment == null) {
            throw new IllegalArgumentException("You need to provide a fragment through initializeSidebarFragment()");
        }

        if (sidebarFragment.isDetached()) {
            fragmentTransaction.attach(sidebarFragment);
        } else {
            fragmentTransaction.replace(SIDEBAR_FRAGMENT_CONTAINER_ID, sidebarFragment, SIDEBAR_FRAGMENT_TAG);
        }

        fragmentTransaction.commit();
    }

    private boolean isSavedContentStateAvailable() {
        return mDashboardContentState != null;
    }

    private void showContentFragment() {
        mContentFragmentTag = mDashboardContentState.getFragmentClass().getName();

        Fragment fragment = Fragment.instantiate(getActivity(), mContentFragmentTag, null);
        fragment.setInitialSavedState(mDashboardContentState.getFragmentState());

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
        fragmentTransaction.commit();
    }

    private void openContentActivity() {
        Intent intent = mDashboardContentState.getOriginalIntent();
        intent.putExtra(DualPaneContentActivity.FRAGMENT_STATE_KEY, mDashboardContentState.getFragmentState());
        startActivity(intent);
    }

    private boolean pendingDualPaneActivityStateAvailable() {
        return EventBus.getDefault().getStickyEvent(DualPaneContentState.class) != null;
    }

    private DualPaneContentState getDuaPaneActivityState() {
        return EventBus.getDefault().getStickyEvent(DualPaneContentState.class);
    }

    @Override
    public void showContentInDashboard(Class contentFragmentClass, Intent intent) {
        if (contentFragmentClass == null || !Fragment.class.isAssignableFrom(contentFragmentClass)) {
            throw new IllegalArgumentException("You need to pass a Fragment class as a parameter.");
        }

        mDashboardContentState = new DualPaneContentState(intent, contentFragmentClass, null);
        mContentFragmentTag = contentFragmentClass.getSimpleName();

        Bundle parameters = intent.getExtras();

        FragmentTransaction fragmentTransaction;
        DualPaneFragment fragment;

        if (isInDualPaneMode()) {
            fragment = (DualPaneFragment) getChildFragmentManager().findFragmentByTag(mContentFragmentTag);
            fragmentTransaction = getChildFragmentManager().beginTransaction();

            if (fragment == null) {
                fragment = (DualPaneFragment) Fragment.instantiate(getActivity(), contentFragmentClass.getName(),
                        parameters);
                fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
            } else if (fragment.isDetached()) {
                fragmentTransaction.attach(fragment);
            }
            fragmentTransaction.commit();
        }

        mIsSinglePaneContentFragmentHidden = false;
    }

    @Override
    public void notifyContentActivityStarted() {
        mIsSinglePaneContentFragmentHidden = false;
        removeContentFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //pass onActivityResult to fragments inside the dashboard.
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(SIDEBAR_FRAGMENT_TAG);
        sidebarFragment.onActivityResult(requestCode, resultCode, data);

        Fragment contentFragment = getContentFragment();
        if (contentFragment != null) {
            contentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean isInDualPaneMode() {
        return getResources().getBoolean(R.bool.dual_pane);
    }

    @Override
    public void removeContentFragment() {
        Fragment contentFragment = getContentFragment();

        if (contentFragment != null) {
            FragmentManager fragmentManager = getChildFragmentManager();
            fragmentManager.beginTransaction().remove(contentFragment).commit();
        }
        resetContentPaneState();
    }

    private void resetContentPaneState() {
        mContentFragmentTag = null;
        mIsSinglePaneContentFragmentHidden = false;
        mDashboardContentState = null;
    }

    @Override
    public boolean isContentFragmentAdded(Class contentFragmentClass) {
        if (contentFragmentClass == null) return false;

        String fragmentTag = contentFragmentClass.getSimpleName();
        Fragment fragment = getChildFragmentManager().findFragmentByTag(fragmentTag);
        return fragment != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY, mContentFragmentTag);
        outState.putBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY, mIsSinglePaneContentFragmentHidden);

        if (isInDualPaneMode()) {
            Fragment contentFragment = getContentFragment();
            if (contentFragment != null && mDashboardContentState != null) {
                SavedState savedState = getChildFragmentManager().saveFragmentInstanceState(contentFragment);
                mDashboardContentState.setFragmentState(savedState);
                outState.putParcelable(DualPaneContentState.KEY, mDashboardContentState);
            }
        } else {
            outState.putParcelable(DualPaneContentState.KEY, mDashboardContentState);
        }
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

    //Content activity is about to be closed
    public void onEventMainThread(DualPaneContentState event) {
        mDashboardContentState = event;
    }
}
