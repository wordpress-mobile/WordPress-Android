package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.ui.reader.ReaderActivity.ReaderPostListType;

import java.io.Serializable;

public class ReaderPostPagerActivity extends Activity
        implements ReaderUtils.FullScreenListener {

    protected static final String ARG_BLOG_POST_ID_LIST = "blog_post_id_list";
    protected static final String ARG_POSITION = "position";
    protected static final String ARG_TITLE = "title";

    private ViewPager mViewPager;
    private PostPagerAdapter mPageAdapter;
    private boolean mIsFullScreen;
    private ReaderPostListType mPostListType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (isFullScreenSupported()) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_pager);

        // remove the window background since each fragment already has a background color
        getWindow().setBackgroundDrawable(null);

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
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
        } else {
            position = getIntent().getIntExtra(ARG_POSITION, 0);
            title = getIntent().getStringExtra(ARG_TITLE);
            serializedList = getIntent().getSerializableExtra(ARG_BLOG_POST_ID_LIST);
            if (getIntent().hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) getIntent().getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
            }
        }

        if (!TextUtils.isEmpty(title)) {
            this.setTitle(title);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mPageAdapter = new PostPagerAdapter(getFragmentManager(), new ReaderBlogIdPostIdList(serializedList));
        mViewPager.setAdapter(mPageAdapter);
        if (mPageAdapter.isValidPosition(position)) {
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

        if (savedInstanceState == null) {
            // animate next/prev buttons after a short delay so user is aware they can swipe
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing()) {
                        animateNavButtons(mViewPager.getCurrentItem());
                    }
                }
            }, 750);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, (String) this.getTitle());
        if (mViewPager != null) {
            outState.putInt(ARG_POSITION, mViewPager.getCurrentItem());
        }
        if (mPageAdapter != null) {
            outState.putSerializable(ARG_BLOG_POST_ID_LIST, mPageAdapter.mIdList);
        }
        if (mPostListType != null) {
            outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, mPostListType);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onRequestFullScreen(boolean enableFullScreen) {
        if (!isFullScreenSupported() || enableFullScreen == mIsFullScreen) {
            return false;
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (enableFullScreen) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
        }

        mIsFullScreen = enableFullScreen;
        return true;
    }

    private void animateNavButtons(int position) {
        boolean canGoPrev = (position > 0);
        boolean canGoNext = (position < mPageAdapter.getCount() - 1);

        if (canGoPrev) {
            AniUtils.fadeInFadeOut(findViewById(R.id.image_previous_page), AniUtils.Duration.LONG);
        }
        if (canGoNext) {
            AniUtils.fadeInFadeOut(findViewById(R.id.image_next_page), AniUtils.Duration.LONG);
        }
    }

    ReaderPostListType getPostListType() {
        return mPostListType;
    }

    @Override
    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    @Override
    public boolean isFullScreenSupported() {
        return true;
    }

    private class PostPagerAdapter extends FragmentStatePagerAdapter {
        private final ReaderBlogIdPostIdList mIdList;
        private final long END_ID = -1;

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList idList) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList) idList.clone();
            // add a bogus entry to the end of the list so we can show PostPagerEndFragment
            // when the user scrolls beyond the last post
            if (mIdList.indexOf(END_ID, END_ID) == -1) {
                mIdList.add(new ReaderBlogIdPostId(END_ID, END_ID));
            }
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        @Override
        public int getCount() {
            return mIdList.size();
        }

        @Override
        public Fragment getItem(int position) {
            long blogId = mIdList.get(position).getBlogId();
            long postId = mIdList.get(position).getPostId();
            if (blogId == END_ID && postId == END_ID) {
                return PostPagerEndFragment.newInstance();
            } else {
                return ReaderPostDetailFragment.newInstance(blogId, postId, getPostListType());
            }
        }
    }

    /*
     * fragment that appears when user scrolls beyond the last post
     */
    public static class PostPagerEndFragment extends Fragment {
        private static PostPagerEndFragment newInstance() {
            return new PostPagerEndFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.reader_fragment_end, container, false);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }
            });
            return view;
        }
    }
}
