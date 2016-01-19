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

/**
 * Abstract fragment that implements core Dual Pane layout functionality
 */
public abstract class DualPaneDashboardHostFragment extends Fragment implements DualPaneDashboard {

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
        }

        showSidebarFragment();
        showContent();
    }

    private void showSidebarFragment() {
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment sidebarFragment = getSidebarFragment();

        if (sidebarFragment == null) {
            throw new IllegalArgumentException("You need to provide a Fragment through initializeSidebarFragment()");
        }

        if (sidebarFragment.isDetached()) {
            fragmentTransaction.attach(sidebarFragment);
        } else {
            fragmentTransaction.replace(SIDEBAR_FRAGMENT_CONTAINER_ID, sidebarFragment, SIDEBAR_FRAGMENT_TAG);
        }

        fragmentTransaction.commit();
    }

    protected Fragment getSidebarFragment() {
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(SIDEBAR_FRAGMENT_TAG);

        if (sidebarFragment == null) {
            sidebarFragment = initializeSidebarFragment();
        }

        return sidebarFragment;
    }

    private void showContent() {
        //If we came into dual pane mode from activity, we would use fresh content state that activity has provided
        if (stickyDualPaneActivityStateAvailable()) {
            mContentState = getStickyContentState();
            EventBus.getDefault().removeStickyEvent(DualPaneContentState.class);
        }

        if (mContentState != null) {
            if (isInDualPaneMode()) {
                removeDefaultContentFragment();
                showContentFragment();
                mIsSinglePaneContentFragmentHidden = false;
            } else {
                //Do nothing if no Intent was provided.
                if (!mContentState.isActivityIntentAvailable()) return;

                if (shouldOpenActivityAfterSwitchToSinglePane() && !mIsSinglePaneContentFragmentHidden) {
                    openContentActivity();
                    onContentActivityStarted();
                } else {
                    mIsSinglePaneContentFragmentHidden = true;
                }
            }
        } else {
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
        mContentFragmentTag = mContentState.getFragmentClass().getName();

        Fragment fragment = Fragment.instantiate(getActivity(), mContentFragmentTag, null);
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

        mContentState = new DualPaneContentState(intent, contentFragmentClass, null);
        mContentFragmentTag = contentFragmentClass.getSimpleName();

        Bundle parameters = intent.getExtras();

        FragmentTransaction fragmentTransaction;
        DualPaneFragment fragment;

        fragment = (DualPaneFragment) getChildFragmentManager().findFragmentByTag(mContentFragmentTag);
        fragmentTransaction = getChildFragmentManager().beginTransaction();

        if (fragment == null) {
            fragment = (DualPaneFragment) Fragment.instantiate(getActivity(), contentFragmentClass.getName(),
                    parameters);
            fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
        } else if (fragment.isDetached()) {
            fragmentTransaction.attach(fragment);
        }
        // Remember that commit() is an async method! Fragment might not be immediately available after this call.
        fragmentTransaction.commit();

        mIsSinglePaneContentFragmentHidden = false;
    }

    @Override
    public void onContentActivityStarted() {
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
