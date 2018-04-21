package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;

/**
 * pager adapter containing fragments used by WPMainActivity
 */
class WPMainPageAdapter extends FragmentStatePagerAdapter {
    private static final int NUM_PAGES = 4;

    static final int PAGE_MY_SITE = 0;
    static final int PAGE_READER = 1;
    static final int PAGE_ME = 2;
    static final int PAGE_NOTIFS = 3;

    private final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_PAGES);

    WPMainPageAdapter(FragmentManager fm) {
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
