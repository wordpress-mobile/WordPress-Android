package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.main.WPMainActivity.OnScrollToTopListener;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.util.AppLog;

import static android.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE;

/*
 * Bottom navigation view and related fragment adapter used by the main activity
 * for the four primary views
 * TODO: notification badge
 */
public class WPMainNavigationView extends BottomNavigationView {
    private static final int NUM_PAGES = 4;

    static final int PAGE_MY_SITE = 0;
    static final int PAGE_READER = 1;
    static final int PAGE_ME = 2;
    static final int PAGE_NOTIFS = 3;

    private NavAdapter mNavAdapter;
    private FragmentManager mFragmentManager;

    public WPMainNavigationView(Context context) {
        super(context);
    }

    public WPMainNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPMainNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    void init(@NonNull FragmentManager fm) {
        mFragmentManager = fm;
        mNavAdapter = new NavAdapter(mFragmentManager);

        setOnNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int position = getPositionForBottomNavItemId(item.getItemId());
                setCurrentPage(position);
                return true;
            }
        });

        setOnNavigationItemReselectedListener(new OnNavigationItemReselectedListener() {
            @Override public void onNavigationItemReselected(@NonNull MenuItem item) {
                // scroll the active fragment's contents to the top when user re-taps the current item
                int position = getPositionForBottomNavItemId(item.getItemId());
                Fragment fragment = mNavAdapter.getFragment(position);
                if (fragment instanceof OnScrollToTopListener) {
                    ((OnScrollToTopListener) fragment).onScrollToTop();
                }
            }
        });
    }

    Fragment getActiveFragment() {
        return mNavAdapter.getFragment(getCurrentPosition());
    }

    private int getPositionForBottomNavItemId(int itemId) {
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

    private @IdRes int getBottomNavItemIdForPosition(int position) {
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
        return getPositionForBottomNavItemId(getSelectedItemId());
    }

    void setCurrentPosition(int position) {
        setSelectedItemId(getBottomNavItemIdForPosition(position));
    }

    void setCurrentPage(int position) {
        Fragment fragment = mNavAdapter.getFragment(position);
        if (fragment != null) {
            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setTransition(TRANSIT_FRAGMENT_FADE)
                    .commit();
        }

        AppPrefs.setMainTabIndex(position);
    }

    /*
     * re-create the fragment adapter so all its fragments are also re-created - used when
     * user signs in/out so the fragments reflect the active account
     * TODO: test this with the new nav
     */
    void resetFragments() {
        AppLog.i(AppLog.T.MAIN, "main activity > reset fragments");

        // reset the timestamp that determines when followed tags/blogs are updated so they're
        // updated when the fragment is recreated (necessary after signin/disconnect)
        ReaderPostListFragment.resetLastUpdateDate();

        // remember the current position, then recreate the adapter so new fragments are created
        int position = getCurrentPosition();
        mNavAdapter = new NavAdapter(mFragmentManager);

        // restore previous position
        setCurrentPage(position);
    }

    Fragment getFragment(int position) {
        return mNavAdapter.getFragment(position);
    }

    private class NavAdapter extends FragmentStatePagerAdapter {
        private final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_PAGES);

        NavAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragement no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                // nop
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        public boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        @Override
        public Fragment getItem(int position) {
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

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getFragment(int position) {
            if (isValidPosition(position)) {
                if (mFragments.get(position) == null) {
                    return getItem(position);
                }
                return mFragments.get(position);
            } else {
                return null;
            }
        }
    }
}
