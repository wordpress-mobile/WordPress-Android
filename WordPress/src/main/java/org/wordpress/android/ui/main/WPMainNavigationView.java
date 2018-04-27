package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemReselectedListener;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AniUtils.Duration;
import org.wordpress.android.util.AppLog;

import static android.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE;

/*
 * Bottom navigation view and related fragment adapter used by the main activity
 * for the four primary views
 */
public class WPMainNavigationView extends BottomNavigationView
        implements OnNavigationItemSelectedListener, OnNavigationItemReselectedListener {
    private static final int NUM_PAGES = 4;

    static final int PAGE_MY_SITE = 0;
    static final int PAGE_READER = 1;
    static final int PAGE_ME = 2;
    static final int PAGE_NOTIFS = 3;

    private NavAdapter mNavAdapter;
    private FragmentManager mFragmentManager;
    private View mBadgeView;
    private OnPageListener mListener;

    interface OnPageListener {
        void onPageChanged(int position);
    }

    public WPMainNavigationView(Context context) {
        super(context);
    }

    public WPMainNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPMainNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    void init(@NonNull FragmentManager fm, @NonNull OnPageListener listener) {
        mFragmentManager = fm;
        mListener = listener;

        mNavAdapter = new NavAdapter();
        assignNavigationListeners(true);

        // add the notification badge to the notification menu item
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) getChildAt(0);
        View notifView = menuView.getChildAt(PAGE_NOTIFS);
        BottomNavigationItemView itemView = (BottomNavigationItemView) notifView;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mBadgeView = inflater.inflate(R.layout.badge_layout, menuView, false);
        itemView.addView(mBadgeView);
        mBadgeView.setVisibility(View.GONE);
    }

    private void assignNavigationListeners(boolean assign) {
        setOnNavigationItemSelectedListener(assign ? this : null);
        setOnNavigationItemReselectedListener(assign ? this : null);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int position = getPositionForItemId(item.getItemId());
        setCurrentPosition(position, false);
        mListener.onPageChanged(position);
        return true;
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem item) {
        // scroll the active fragment's contents to the top when user re-taps the current item
        int position = getPositionForItemId(item.getItemId());
        Fragment fragment = mNavAdapter.getFragment(position);
        if (fragment instanceof OnScrollToTopListener) {
            ((OnScrollToTopListener) fragment).onScrollToTop();
        }
    }

    Fragment getActiveFragment() {
        return mNavAdapter.getFragment(getCurrentPosition());
    }

    private int getPositionForItemId(int itemId) {
        switch (itemId) {
            case R.id.nav_sites:
                return PAGE_MY_SITE;
            case R.id.nav_reader:
                return PAGE_READER;
            case R.id.nav_me:
                return PAGE_ME;
            default:
                return PAGE_NOTIFS;
        }
    }

    private @IdRes int getItemIdForPosition(int position) {
        switch (position) {
            case PAGE_MY_SITE:
                return R.id.nav_sites;
            case PAGE_READER:
                return R.id.nav_reader;
            case PAGE_ME:
                return R.id.nav_me;
            default:
                return R.id.nav_notifications;
        }
    }

    int getCurrentPosition() {
        return getPositionForItemId(getSelectedItemId());
    }

    void setCurrentPosition(int position) {
        setCurrentPosition(position, true);
    }

    private void setCurrentPosition(int position, boolean ensureSelected) {
        if (ensureSelected) {
            // temporarily disable the nav listeners so they don't fire when we change the selected page
            assignNavigationListeners(false);
            try {
                setSelectedItemId(getItemIdForPosition(position));
            } finally {
                assignNavigationListeners(true);
            }
        }

        Fragment fragment = mNavAdapter.getFragment(position);
        if (fragment != null) {
            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setTransition(TRANSIT_FRAGMENT_FADE)
                    .commit();
        }
    }

    CharSequence getMenuTitleForPosition(int position) {
        int itemId = getItemIdForPosition(position);
        MenuItem item = getMenu().findItem(itemId);
        return item.getTitle();
    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     */
    void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // reset the timestamp that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        ReaderPostListFragment.resetLastUpdateDate();

        // remember the current position, reset the adapter so new fragments are created, then restore position
        int position = getCurrentPosition();
        mNavAdapter.reset();
        setCurrentPosition(position);
    }

    Fragment getFragment(int position) {
        return mNavAdapter.getFragment(position);
    }

    void showNoteBadge(boolean showBadge) {
        int currentVisibility = mBadgeView.getVisibility();
        int newVisibility = showBadge ? View.VISIBLE : View.GONE;
        if (currentVisibility == newVisibility) {
            return;
        }

        if (showBadge) {
            AniUtils.fadeIn(mBadgeView, Duration.MEDIUM);
        } else {
            AniUtils.fadeOut(mBadgeView, Duration.MEDIUM);
        }
    }

    private class NavAdapter {
        private final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_PAGES);

        boolean isValidPosition(int position) {
            return (position >= 0 && position < NUM_PAGES);
        }

        void reset() {
            mFragments.clear();
        }

        private Fragment createFragment(int position) {
            Fragment fragment;
            switch (position) {
                case PAGE_MY_SITE:
                    fragment = MySiteFragment.newInstance();
                    break;
                case PAGE_READER:
                    fragment = ReaderPostListFragment.newInstance();
                    break;
                case PAGE_ME:
                    fragment = MeFragment.newInstance();
                    break;
                case PAGE_NOTIFS:
                    fragment = NotificationsListFragment.newInstance();
                    break;
                default:
                    return null;
            }

            mFragments.put(position, fragment);
            return fragment;
        }

        Fragment getFragment(int position) {
            if (isValidPosition(position) && mFragments.get(position) != null) {
              return mFragments.get(position);
            } else {
                return createFragment(position);
            }
        }
    }
}
