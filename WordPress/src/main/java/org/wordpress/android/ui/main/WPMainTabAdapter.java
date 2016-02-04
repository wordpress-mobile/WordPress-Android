package org.wordpress.android.ui.main;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;

/**
 * pager adapter containing tab fragments used by WPMainActivity
 */
class WPMainTabAdapter extends FragmentStatePagerAdapter {

    static final int NUM_TABS = 4;

    static final int TAB_MY_SITE = 0;
    static final int TAB_READER  = 1;
    static final int TAB_ME      = 2;
    static final int TAB_NOTIFS  = 3;

    private final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_TABS);

    public WPMainTabAdapter(FragmentManager fm) {
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
        return NUM_TABS;
    }

    public boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_MY_SITE:
                return MySiteDualPaneFragment.newInstance();
            case TAB_READER:
                return ReaderPostListFragment.newInstance();
            case TAB_ME:
                return MeFragment.newInstance();
            case TAB_NOTIFS:
                return NotificationsListFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);
        if (item instanceof Fragment) {
            mFragments.put(position, (Fragment) item);
        }
        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getFragment(int position) {
        if (isValidPosition(position)) {
            return mFragments.get(position);
        } else {
            return null;
        }
    }

}
