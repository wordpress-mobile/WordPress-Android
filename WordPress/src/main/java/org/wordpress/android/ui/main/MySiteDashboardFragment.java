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
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppPrefs;

import java.security.InvalidParameterException;

/**
 * Dashboard fragments shows dual pane layout for tablets and single pane for smartphones.
 */

public class MySiteDashboardFragment extends Fragment implements MySiteFragment.OnMenuItemClickListener,
        MySiteFragment.OnSiteChangedListener {

    private final static int CONTAINER_ACTIVITY_ROOT_VIEW_ID = R.id.root_view_main;

    private final static String SIDEBAR_FRAGMENT_TAG = "my_sites";
    private final static String BACK_STACK_ID = "dashboard_back_stack";
    private final static String CONTENT_FRAGMENT_TAG_PARAMETER_KEY = "content_fragment_tag";
    private final static String CONTENT_FRAGMENT_STATE_PARAMETER_KEY = "single_pane_content_fragment_is_hidden";

    private View mEmptyView;
    private String mContentFragmentTag;

    private boolean mIsSinglePaneContentFragmentHidden = false;

    public static MySiteDashboardFragment newInstance() {
        return new MySiteDashboardFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mContentFragmentTag = savedInstanceState.getString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY);
            mIsSinglePaneContentFragmentHidden = savedInstanceState.getBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY);
        }

        attachSidebarFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_sites_dashboard_fragment, container, false);
        mEmptyView = view.findViewById(R.id.image_empty);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Content fragment is attached to activity's view hierarchy (not to this's one) in single pane mode
        //that's why we do it in onActivityCreated()
        attachContentFragment();
    }

    private void attachSidebarFragment() {
        //sidebar fragment is always part of a dashboard, so we only use child fragment manager
        FragmentTransaction childFragmentTransaction = getChildFragmentManager().beginTransaction();

        Fragment mySitesFragment = getSidebarFragment();
        if (mySitesFragment.isDetached()) {
            childFragmentTransaction.attach(mySitesFragment);
        } else {
            childFragmentTransaction.replace(R.id.my_site_fragment_container, mySitesFragment, SIDEBAR_FRAGMENT_TAG);
        }
        childFragmentTransaction.commit();
    }

    private Fragment getSidebarFragment() {
        Fragment sidebarFragment = getChildFragmentManager().findFragmentByTag(SIDEBAR_FRAGMENT_TAG);
        if (sidebarFragment == null) {
            sidebarFragment = MySiteFragment.newInstance();
        }
        return sidebarFragment;
    }

    private boolean isContentFragmentAttachedToActivity() {
        return getFragmentManager().findFragmentByTag(mContentFragmentTag) != null;
    }

    private boolean isContentFragmentAttachedToDashboardFragment() {
        return getChildFragmentManager().findFragmentByTag(mContentFragmentTag) != null;
    }

    /*
     * Remove's fragment from FragmentManger when necessary and return's reference to it (so it wont ne GC'ed)
     */
    private Fragment getDetachedContentFragment() {
        FragmentManager fragmentManager = getContentFragmentManager();
        Fragment fragment = getContentFragment();
        if (fragment != null) {
            //if number of panes did not changed after configuration change (fragment was simply recreated, etc)
            //we just return fragment as-is
            if (isContentFragmentAttachedToActivity() && isInDualPaneMode()) {
                fragmentManager.popBackStack(BACK_STACK_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentManager.beginTransaction().remove(fragment).commit();
                fragmentManager.executePendingTransactions();
            } else if (isContentFragmentAttachedToDashboardFragment() && !isInDualPaneMode()) {
                fragmentManager.beginTransaction().remove(fragment).commit();
                fragmentManager.executePendingTransactions();
            }
        }

        return fragment;
    }

    private Fragment getContentFragment() {
        Fragment contentFragment = null;

        //content fragment can belong to either activity's or dashboard's FragmentManger
        if (getFragmentManager().findFragmentByTag(mContentFragmentTag) != null) {
            contentFragment = getFragmentManager().findFragmentByTag(mContentFragmentTag);
        } else if (getChildFragmentManager().findFragmentByTag(mContentFragmentTag) != null) {
            contentFragment = getChildFragmentManager().findFragmentByTag(mContentFragmentTag);
        }

        return contentFragment;
    }

    /*
     *  There is no method to get "main" FragmentManger from fragment
     *  so we are going from opposite direction, and querying each FragmentManger if they have our fragment
     *  and returning them if they do
     */
    private FragmentManager getContentFragmentManager() {
        FragmentManager fragmentManager = null;
        if (isContentFragmentAttachedToActivity()) {
            fragmentManager = getFragmentManager();
        } else if (isContentFragmentAttachedToDashboardFragment()) {
            fragmentManager = getChildFragmentManager();
        }
        return fragmentManager;
    }

    private void attachContentFragment() {
        //we need to keep a reference to a wild Fragment removed from FragmentManger
        //otherwise it will be GC'ed
        Fragment contentFragment = getDetachedContentFragment();

        if (contentFragment != null) {
            FragmentTransaction fragmentTransaction;

            if (isInDualPaneMode()) {
                fragmentTransaction = getChildFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.content_fragment_container, contentFragment, mContentFragmentTag);
            } else {
                checkContainerActivityLayoutCompatibility();

                fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.do_nothing, R.anim.do_nothing,
                        R.anim.activity_slide_out_to_right, R.anim.activity_slide_out_to_right);

                //if configuration change occurred while we are not in dashboard tab and in single pane mod
                //the content fragment has to be detached (and kept like this, unless reopened)
                if (AppPrefs.getMainTabIndex() == WPMainTabAdapter.TAB_MY_SITE && !mIsSinglePaneContentFragmentHidden) {
                    fragmentTransaction.replace(CONTAINER_ACTIVITY_ROOT_VIEW_ID, contentFragment, mContentFragmentTag);

                    if (!contentFragment.isAdded()) {
                        fragmentTransaction.addToBackStack(BACK_STACK_ID);
                    }
                } else {
                    mIsSinglePaneContentFragmentHidden = true;

                    if (!contentFragment.isAdded()) {
                        fragmentTransaction.replace(CONTAINER_ACTIVITY_ROOT_VIEW_ID, contentFragment, mContentFragmentTag);
                        fragmentTransaction.detach(contentFragment);
                        fragmentTransaction.addToBackStack(BACK_STACK_ID);
                    }
                }
            }
            fragmentTransaction.commit();
        }
    }

    /*
     * Checks if the parent activity has a view where we can add content fragment, while single pane mode
     */
    private void checkContainerActivityLayoutCompatibility() {
        View activityRootView = getActivity().findViewById(CONTAINER_ACTIVITY_ROOT_VIEW_ID);

        if (activityRootView == null) {
            throw new InvalidParameterException("Cant find view with id specified in CONTAINER_ACTIVITY_ROOT_VIEW_ID " +
                    "within activity layout");
        }

        if (!((activityRootView instanceof FrameLayout) || (activityRootView instanceof RelativeLayout))) {
            throw new InvalidParameterException("Activity container view must be subclass of FrameLayout or RelativeLayout");
        }
    }

    @Override
    public void onSideBarMenuClicked(Class contentFragmentClass, Bundle parameters) {
        if (contentFragmentClass == null || !Fragment.class.isAssignableFrom(contentFragmentClass)) {
            throw new IllegalArgumentException("You need to pass a Fragment class as a parameter.");
        }

        FragmentTransaction fragmentTransaction;
        Fragment fragment;

        mContentFragmentTag = contentFragmentClass.getSimpleName();

        if (isInDualPaneMode()) {
            fragment = getChildFragmentManager().findFragmentByTag(mContentFragmentTag);
            fragmentTransaction = getChildFragmentManager().beginTransaction();

            if (fragment == null) {
                fragment = Fragment.instantiate(getActivity(), contentFragmentClass.getName(), parameters);
                fragmentTransaction.replace(R.id.content_fragment_container, fragment, mContentFragmentTag);
            } else if (fragment.isDetached()) {
                fragmentTransaction.attach(fragment);
            }
        } else {
            checkContainerActivityLayoutCompatibility();

            fragment = getFragmentManager().findFragmentByTag(mContentFragmentTag);
            fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.do_nothing,
                    R.anim.activity_slide_out_to_right, R.anim.activity_slide_out_to_right);

            if (fragment == null) {
                fragment = Fragment.instantiate(getActivity(), contentFragmentClass.getName(), parameters);
                fragmentTransaction.replace(CONTAINER_ACTIVITY_ROOT_VIEW_ID, fragment, mContentFragmentTag);
                fragmentTransaction.addToBackStack(BACK_STACK_ID);
            } else if (fragment.isDetached()) {
                fragmentTransaction.attach(fragment);
            }
        }

        fragmentTransaction.commit();

        mIsSinglePaneContentFragmentHidden = false;
        hideEmptyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //pass onActivityResult to fragments inside the dashboard.
        Fragment sidebarFragment = getSidebarFragment();
        sidebarFragment.onActivityResult(requestCode, resultCode, data);

        Fragment contentFragment = getContentFragment();
        if (contentFragment != null) {
            contentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean isInDualPaneMode() {
        return getResources().getBoolean(R.bool.dual_pane);
    }

    @Override
    public void onSiteChanged() {
        //when the site is changed/created - remove content fragment
        //clear back stack, reset state variables and show empty view
        Fragment contentFragment = getContentFragment();

        if (contentFragment != null) {
            FragmentManager fragmentManager = getContentFragmentManager();

            fragmentManager.popBackStack(BACK_STACK_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentManager.beginTransaction().remove(contentFragment).commit();
        }

        mContentFragmentTag = null;
        mIsSinglePaneContentFragmentHidden = false;
        showEmptyView();
    }

    private void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void showEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CONTENT_FRAGMENT_TAG_PARAMETER_KEY, mContentFragmentTag);
        outState.putBoolean(CONTENT_FRAGMENT_STATE_PARAMETER_KEY, mIsSinglePaneContentFragmentHidden);
    }
}