package org.wordpress.android.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.ReaderTypes;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.WPMainViewPager;

import javax.annotation.Nonnull;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public class WPMainActivity extends ActionBarActivity {

    private WPMainViewPager mViewPager;
    private WPTabAdapter mTabAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mViewPager = (WPMainViewPager) findViewById(R.id.viewpager_main);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.tabstrip_main);
        tabStrip.setTabIndicatorColorResource(R.color.tab_indicator);
        tabStrip.setTextColor(getResources().getColor(R.color.tab_text_selected));

        mTabAdapter = new WPTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);
    }

    @Override
    protected void onSaveInstanceState(@Nonnull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * pager adapter containing post detail fragments
     **/
    private class WPTabAdapter extends FragmentStatePagerAdapter {
        private static final int NUM_TABS = 4;
        private static final int TAB_SITES = 0;
        private static final int TAB_READER = 1;
        private static final int TAB_ME = 2;
        private static final int TAB_NOTIFS = 3;

        WPTabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragement no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                AppLog.v(AppLog.T.READER, "WPTabAdapter pager > adapter restoreState");
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        }

        @Override
        public Parcelable saveState() {
            AppLog.v(AppLog.T.READER, "WPTabAdapter pager > adapter saveState");
            return super.saveState();
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_SITES:
                    break;
                case TAB_READER:
                    break;
                case TAB_ME:
                    break;
                case TAB_NOTIFS:
                    break;
            }
            return ReaderPostListFragment.newInstance(null, ReaderTypes.ReaderPostListType.TAG_FOLLOWED);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // TODO: move hard-coded strings to resources
            switch (position) {
                case TAB_SITES:
                    return "Sites";
                case TAB_READER:
                    return "Reader";
                case TAB_ME:
                    return "Me";
                case TAB_NOTIFS:
                    return "Notifications";
                default:
                    return super.getPageTitle(position);
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);
            /*if (item instanceof Fragment) {
                mFragmentMap.put(position, (Fragment) item);
            }*/
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // mFragmentMap.remove(position);
            super.destroyItem(container, position, object);
        }
    }
}
