package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;

import java.io.Serializable;
import java.util.ArrayList;

public class ReaderPostPagerActivity extends Activity
                                     implements ReaderUtils.FullScreenListener {
    protected static final String ARG_BLOG_POST_ID_LIST = "blog_post_id_list";
    protected static final String ARG_POSITION = "position";
    protected static final String ARG_TITLE = "title";

    private ViewPager mViewPager;
    private PostPagerAdapter mPageAdapter;
    private boolean mIsFullScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (isFullScreenSupported()) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_pager);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final int position;
        final String title;
        final Serializable serializedList;
        if (savedInstanceState != null) {
            position = savedInstanceState.getInt(ARG_POSITION, 0);
            title = savedInstanceState.getString(ARG_TITLE);
            serializedList = savedInstanceState.getSerializable(ARG_BLOG_POST_ID_LIST);
        } else {
            position = getIntent().getIntExtra(ARG_POSITION, 0);
            title = getIntent().getStringExtra(ARG_TITLE);
            serializedList = getIntent().getSerializableExtra(ARG_BLOG_POST_ID_LIST);
        }

        if (!TextUtils.isEmpty(title)) {
            this.setTitle(title);
        }

        // when Android serialized the list it was converted to ArrayList<ReaderBlogIdPostId>,
        // so convert it back to ReaderBlogIdPostIdList
        final ReaderBlogIdPostIdList idList;
        if (serializedList != null) {
            idList = new ReaderBlogIdPostIdList((ArrayList<ReaderBlogIdPostId>) serializedList);
        } else {
            idList = new ReaderBlogIdPostIdList();
        }

        mPageAdapter = new PostPagerAdapter(getFragmentManager(), idList);
        mViewPager.setAdapter(mPageAdapter);
        if (position >= 0 || position < mPageAdapter.getCount()) {
            mViewPager.setCurrentItem(position);
        }

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                onRequestFullScreen(false);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    onRequestFullScreen(false);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, (String) this.getTitle());
        outState.putInt(ARG_POSITION, mViewPager.getCurrentItem());
        outState.putSerializable(ARG_BLOG_POST_ID_LIST, mPageAdapter.mIdList);
    }

    @Override
    public boolean onRequestFullScreen(boolean enableFullScreen) {
        if (!isFullScreenSupported() || enableFullScreen == mIsFullScreen) {
            return false;
        }

        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return false;
        }

        if (enableFullScreen) {
            actionBar.hide();
        } else {
            actionBar.show();
        }

        mIsFullScreen = enableFullScreen;
        return true;
    }

    @Override
    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @Override
    public boolean isFullScreenSupported() {
        return true;
    }

    class PostPagerAdapter extends FragmentStatePagerAdapter {
        private final ReaderBlogIdPostIdList mIdList;

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList idList) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList) idList.clone();
        }

        @Override
        public int getCount() {
            return mIdList.size();
        }

        @Override
        public Fragment getItem(int position) {
            long blogId = mIdList.get(position).getBlogId();
            long postId = mIdList.get(position).getPostId();
            return ReaderPostDetailFragment.newInstance(blogId, postId);
        }
    }
}
