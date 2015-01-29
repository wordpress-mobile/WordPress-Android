package org.wordpress.android.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.ReaderTypes;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.SlidingTabLayout;
import org.wordpress.android.widgets.WPMainViewPager;

import javax.annotation.Nonnull;

/**
 * Main activity which hosts sites, reader, me and notifications tabs
 */
public class WPMainActivity extends ActionBarActivity
    implements ReaderInterfaces.OnReaderPostSelectedListener,
               ReaderInterfaces.OnReaderTagSelectedListener
{
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

        mTabAdapter = new WPTabAdapter(getFragmentManager());
        mViewPager.setAdapter(mTabAdapter);

        SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        tabs.setCustomTabView(R.layout.tab_text, R.id.text_tab);
        tabs.setSelectedIndicatorColors(getResources().getColor(R.color.tab_indicator));
        tabs.setDistributeEvenly(true);
        tabs.setViewPager(mViewPager);
    }

    @Override
    protected void onSaveInstanceState(@Nonnull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /***
     * START READER-RELATED ROUTINES
     */
    private ReaderPostListFragment getReaderListFragment() {
        Fragment fragment = mTabAdapter.getFragment(WPTabAdapter.TAB_READER);
        if (fragment != null && fragment instanceof ReaderPostListFragment) {
            return (ReaderPostListFragment)fragment;
        }
        return null;
    }

    /*
     * user tapped a post in the reader list fragment
     */
    @Override
    public void onReaderPostSelected(long blogId, long postId) {
        ReaderPostListFragment listFragment = getReaderListFragment();
        if (listFragment == null) {
            return;
        }

        switch (listFragment.getPostListType()) {
            case TAG_FOLLOWED:
            case TAG_PREVIEW:
                ReaderActivityLauncher.showReaderPostPagerForTag(
                        this,
                        listFragment.getCurrentTag(),
                        listFragment.getPostListType(),
                        blogId,
                        postId);
                break;
            case BLOG_PREVIEW:
                ReaderActivityLauncher.showReaderPostPagerForBlog(
                        this,
                        blogId,
                        postId);
                break;
        }
    }

    /*
     * user tapped a tag in the reader list fragment
     */
    @Override
    public void onReaderTagSelected(String tagName) {
        ReaderTag tag = new ReaderTag(tagName, ReaderTagType.FOLLOWED);
        ReaderPostListFragment listFragment = getReaderListFragment();
        if (listFragment != null && listFragment.getPostListType().equals(ReaderTypes.ReaderPostListType.TAG_PREVIEW)) {
            // user is already previewing a tag, so change current tag in existing preview
            listFragment.setCurrentTag(tag);
        } else {
            // user isn't previewing a tag, so open in tag preview
            ReaderActivityLauncher.showReaderTagPreview(this, tag);
        }
    }

    /*
     * END READER-RELATED ROUTINES
     ***/

    /**
     * pager adapter containing post detail fragments
     **/
    private class WPTabAdapter extends FragmentStatePagerAdapter {
        private static final int NUM_TABS = 4;
        private static final int TAB_READER = 0;
        private static final int TAB_SITES = 1;
        private static final int TAB_ME = 2;
        private static final int TAB_NOTIFS = 3;

        final SparseArray<Fragment> mFragments = new SparseArray<>(NUM_TABS);

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

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
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
            Fragment fragment = ReaderPostListFragment.newInstance(ReaderTag.getDefaultTag(), ReaderTypes.ReaderPostListType.TAG_FOLLOWED);
            return fragment;
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
            if (item instanceof Fragment) {
                mFragments.put(position, (Fragment) item);
            }
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragments.remove(position);
        }

        public Fragment getFragment(int position) {
            if (isValidPosition(position)) {
                return mFragments.get(position);
            } else {
                return null;
            }
        }
    }
}
