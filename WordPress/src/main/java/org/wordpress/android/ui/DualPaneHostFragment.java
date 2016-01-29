package org.wordpress.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.util.DualPaneHelper;

import de.greenrobot.event.EventBus;

/**
 * Abstract fragment that provides core Dual Pane layout functionality
 */
public abstract class DualPaneHostFragment extends Fragment implements DualPaneHost {

    private final static String TAG = DualPaneHostFragment.class.getSimpleName();

    private final static int SIDEBAR_FRAGMENT_CONTAINER_ID = R.id.sidebar_fragment_container;
    private final static int CONTENT_FRAGMENT_CONTAINER_ID = R.id.content_fragment_container;

    private final static String SIDEBAR_FRAGMENT_TAG = "dual_pane_sidebar_fragment";
    private final static String DEFAULT_FRAGMENT_TAG = "dual_pane_default_fragment";

    private final static String CONTENT_FRAGMENT_TAG_PARAMETER_KEY = "content_fragment_tag";
    private final static String CONTENT_FRAGMENT_STATE_PARAMETER_KEY = "single_pane_content_fragment_is_hidden";

    private String mContentFragmentTag;
    private boolean mIsSinglePaneContentFragmentHidden = false;

    private DualPaneContentState mContentState;

    /**
     * <p>Gives you a chance to initialize a Sidebar fragment.</p>
     * This method will only be called if sidebar fragment does not exist in fragment manager
     */
    protected abstract Fragment initializeSidebarFragment();

    /**
     * <p>Gives you a chance to initialize default content fragment.</p>
     * Default content fragment would be displayed in dual pane mode, when there is no content fragment.<br>
     * This method will only be called if default content fragment does not exist in fragment manager.
     */
    protected abstract Fragment initializeDefaultFragment();

    /**
     * This method let's you implement custom logic that will decide, when content activity be started after switching
     * from dual to single pane mode.
     */
    protected boolean canOpenActivityAfterSwitchToSinglePaneMode() {
        return true;
    }

    protected String getSidebarFragmentTag() {
        return SIDEBAR_FRAGMENT_TAG;
    }

    protected String getDefaultFragmentTag() {
        return DEFAULT_FRAGMENT_TAG;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mContentFragmentTag = savedInstanceState.getString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY);
            mIsSinglePaneContentFragmentHidden = savedInstanceState.getBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY);
            mContentState = savedInstanceState.getParcelable(DualPaneContentState.KEY);
            Log.v(TAG, "Host fragment is restored");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Reason we perform initial fragment transactions in onActivityCreated instead of onCreate, is because in some
        // cases (like when this fragment is a part of ViewPager) child FragmentManger might still not have restored it's
        // state in onCreate
        showSidebarFragment();
        showContent();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // we are using generic dual pane fragment layout
        return inflater.inflate(R.layout.dual_pane_host_fragment, container, false);
    }

    private void showSidebarFragment() {
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment sidebarFragment = getSidebarFragment();

        if (sidebarFragment == null) {
            throw new IllegalArgumentException("You need to provide a Fragment through initializeSidebarFragment()");
        }

        if (!sidebarFragment.isAdded()) {
            Log.v(TAG, "Adding sidebar fragment.");
            fragmentTransaction.replace(SIDEBAR_FRAGMENT_CONTAINER_ID, sidebarFragment, getSidebarFragmentTag());
        } else if (sidebarFragment.isDetached()) {
            Log.v(TAG, "Sidebar fragment already exist. Attaching to layout.");
            fragmentTransaction.attach(sidebarFragment);
        }

        fragmentTransaction.commit();
    }

    /**
     * Try to get sidebar from fragment manager. If it's not there return's fragment provided by concrete class.
     */
    protected Fragment getSidebarFragment() {
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(getSidebarFragmentTag());

        if (sidebarFragment == null) {
            sidebarFragment = initializeSidebarFragment();
            Log.v(TAG, "Sidebar fragment does not exist, initializing new one.");
        }

        return sidebarFragment;
    }

    /**
     * In single pane mode try to show available content in activity.
     * In dual pane mode try to show retained/default content fragment in content pane.
     */
    private void showContent() {
        if (DualPaneHelper.isInDualPaneConfiguration(getActivity())) {
            Log.v(TAG, "Host fragment is in dual pane mode.");
            if (hasRetainedDualPaneActivityState()) {
                removeDefaultContentFragment();
                showRestoredContentFragment();
            } else if (mContentState == null) {
                Log.v(TAG, "No saved content state.");
                showDefaultContentFragment(false);
            }

            mIsSinglePaneContentFragmentHidden = false; //content pane is always visible in dual pane mode
        } else {
            if (mContentState == null) return;
            Log.v(TAG, "Host fragment is in single pane mode.");

            //Do nothing if no Intent was provided when adding content.
            if (!mContentState.isActivityIntentAvailable()) {
                Log.v(TAG, "No intent to start a single pane activity.");
                return;
            }

            if (canOpenActivityAfterSwitchToSinglePaneMode() && !mIsSinglePaneContentFragmentHidden) {
                Log.v(TAG, "Starting single pane activity.");

                openContentActivity();
                onContentActivityStarted();
            } else {
                Log.v(TAG, "Cant start single pane activity yet.");
                mIsSinglePaneContentFragmentHidden = true;
            }
        }
    }

    /**
     * @return true if {@link DualPaneContentActivity} has passed us its fragment's state when going into dual pane mode
     */
    private boolean hasRetainedDualPaneActivityState() {
        return EventBus.getDefault().getStickyEvent(DualPaneContentState.class) != null;
    }

    /**
     * @return {@link DualPaneContentState} passed from {@link DualPaneContentActivity}
     */
    private DualPaneContentState getStickyContentState() {
        return EventBus.getDefault().getStickyEvent(DualPaneContentState.class);
    }

    private void removeDefaultContentFragment() {
        Fragment defaultFragment = getChildFragmentManager().findFragmentByTag(getDefaultFragmentTag());

        if (defaultFragment != null) {
            Log.v(TAG, "Removing default fragment.");
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.remove(defaultFragment);
            fragmentTransaction.commit();
        }
    }

    private void showDefaultContentFragment(boolean forceRecreation) {
        if (!DualPaneHelper.isInDualPaneConfiguration(getActivity())) return;

        Fragment defaultFragment = getChildFragmentManager().findFragmentByTag(getDefaultFragmentTag());

        if (defaultFragment == null || forceRecreation) {
            defaultFragment = initializeDefaultFragment();
        }

        //Do nothing if there is no default fragment.
        if (defaultFragment == null) return;

        Log.v(TAG, "Showing default fragment.");

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, defaultFragment, getDefaultFragmentTag());
        fragmentTransaction.commit();
    }

    /**
     * Show content fragment with initial state passed from {@link DualPaneContentActivity}
     */
    private void showRestoredContentFragment() {
        Log.v(TAG, "Showing restored content fragment.");

        mContentState = getStickyContentState();
        EventBus.getDefault().removeStickyEvent(DualPaneContentState.class);

        Fragment fragment = Fragment.instantiate(getActivity(), mContentState.getFragmentClass().getName(), mContentState
                .getOriginalIntent().getExtras());
        fragment.setInitialSavedState(mContentState.getFragmentState());

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
        fragmentTransaction.commit();
    }

    /**
     * Open content activity and pass content fragment state to it.
     * Setting initial state of fragment inside activity is up to that activity.
     */
    private void openContentActivity() {
        Intent intent = mContentState.getOriginalIntent();
        intent.setExtrasClassLoader(DualPaneContentState.class.getClassLoader());
        intent.putExtra(DualPaneContentActivity.FRAGMENT_STATE_KEY, mContentState.getFragmentState());
        startActivity(intent);
    }

    @Override
    public void showContent(Class contentFragmentClass, @Nullable String tag, @Nullable Bundle fragmentArgs) {
        showContent(contentFragmentClass, tag, fragmentArgs, null);
    }

    @Override
    public void showContent(Class contentFragmentClass, @Nullable String tag, @Nullable Intent intent) {
        showContent(contentFragmentClass, tag, null, intent);
    }

    private void showContent(Class contentFragmentClass, @Nullable String tag, @Nullable Bundle args, @Nullable Intent
            intent) {
        if (!isAdded() || !DualPaneHelper.isInDualPaneConfiguration(getActivity())) return;

        if (contentFragmentClass == null || !Fragment.class.isAssignableFrom(contentFragmentClass)) {
            throw new IllegalArgumentException("You need to pass a Fragment class to showContent() method!");
        }

        Log.v(TAG, "Showing fragment in content pane.");

        mContentState = new DualPaneContentState(intent, contentFragmentClass, null);
        mContentFragmentTag = tag;

        Bundle fragmentArgs;
        if (intent == null) {
            fragmentArgs = args;
        } else {
            fragmentArgs = intent.getExtras();
        }

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        Fragment fragment = Fragment.instantiate(getActivity(), contentFragmentClass.getName(), fragmentArgs);
        fragmentTransaction.replace(CONTENT_FRAGMENT_CONTAINER_ID, fragment, mContentFragmentTag);
        fragmentTransaction.commit();

        mIsSinglePaneContentFragmentHidden = false;
    }

    @Override
    public void onContentActivityStarted() {
        if (!isAdded()) return;

        Log.v(TAG, "Single pane content activity started.");
        removeContentPaneFragment();
        resetContentPaneState();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //pass onActivityResult to fragments inside the host fragment.
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(getSidebarFragmentTag());
        sidebarFragment.onActivityResult(requestCode, resultCode, data);

        Fragment contentFragment = getContentPaneFragment();
        if (contentFragment != null) {
            contentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void removeContentPaneFragment() {
        Fragment contentFragment = getContentPaneFragment();
        if (contentFragment != null) {
            Log.v(TAG, "Removing content pane fragment.");
            getChildFragmentManager().beginTransaction().remove(contentFragment).commit();
        }
    }

    @Override
    public void resetContentPane() {
        if (!isAdded()) return;

        removeContentPaneFragment();
        resetContentPaneState();
        showDefaultContentFragment(true);

        mContentFragmentTag = null;
    }

    private void resetContentPaneState() {
        mIsSinglePaneContentFragmentHidden = false;
        mContentState = null;
    }

    @Nullable
    @Override
    public Fragment getContentPaneFragment() {
        return getChildFragmentManager().findFragmentById(R.id.content_fragment_container);
    }

    @Override
    public boolean isFragmentAdded(String tag) {
        if (getContentPaneFragment() == null || TextUtils.isEmpty(tag)) return false;

        String contentFragmentTag = getContentPaneFragment().getTag();
        return !TextUtils.isEmpty(contentFragmentTag) && contentFragmentTag.equals(tag);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(TAG, "Saving dual pane host fragment's content state.");

        outState.putString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY, mContentFragmentTag);
        outState.putBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY, mIsSinglePaneContentFragmentHidden);

        Fragment contentFragment = getContentPaneFragment();
        if (contentFragment != null && mContentState != null && !isDefaultFragment(contentFragment)) {
            SavedState savedState = getChildFragmentManager().saveFragmentInstanceState(contentFragment);
            mContentState.setFragmentState(savedState);
        }

        outState.putParcelable(DualPaneContentState.KEY, mContentState);
    }

    private boolean isDefaultFragment(Fragment fragment) {
        if (fragment == null || TextUtils.isEmpty(fragment.getTag())) return false;
        return fragment.getTag().equals(getDefaultFragmentTag());
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

    @SuppressWarnings("unused")
    public void onEvent(DualPaneContentState event) {
        //This method wont be called. We will use EventBus.getDefault().getStickyEvent() instead
    }
}
