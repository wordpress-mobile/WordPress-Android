package org.wordpress.android.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.prefs.SettingsActivity;
import org.wordpress.android.ui.reader.ReaderPostListFragment;

/**
 * pager adapter containing tab fragments used by WPMainActivity
 */
class WPMainTabAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_TABS = 4;
    static final int TAB_SITES  = 0;
    static final int TAB_READER = 1;
    static final int TAB_ME     = 2;
    static final int TAB_NOTIFS = 3;

    private final SparseArray<WPMainTabFragment> mFragments = new SparseArray<>(NUM_TABS);

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
            case TAB_SITES:
                return new DummyFragment();
            case TAB_READER:
                return ReaderPostListFragment.newInstance();
            case TAB_ME:
                return new DummyFragment();
            case TAB_NOTIFS:
                return NotificationsListFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // TODO: use icons rather than text
        switch (position) {
            case TAB_SITES:
                return "Sites";
            case TAB_READER:
                return "Reader";
            case TAB_ME:
                return "Me";
            case TAB_NOTIFS:
                return "Notes";
            default:
                return super.getPageTitle(position);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);
        if (item instanceof WPMainTabFragment) {
            mFragments.put(position, (WPMainTabFragment) item);
        }
        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mFragments.remove(position);
    }

    public WPMainTabFragment getFragment(int position) {
        if (isValidPosition(position)) {
            return mFragments.get(position);
        } else {
            return null;
        }
    }

    public static class DummyFragment extends WPMainTabFragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dummy_fragment, container, false);
            Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
            toolbar.inflateMenu(R.menu.dummy_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_settings:
                            getActivity().startActivityForResult(new Intent(getActivity(), SettingsActivity.class), WPMainActivity.SETTINGS_REQUEST);
                            return true;
                        default:
                            return false;
                    }
                }
            });
            return view;
        }
    }
}
