package org.wordpress.android.ui.reader;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class ReaderPostPagerActivity extends Activity
        implements ReaderUtils.FullScreenListener {

    static final String ARG_BLOG_POST_ID_LIST = "blog_post_id_list";
    static final String ARG_POSITION = "position";
    static final String ARG_TITLE = "title";

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
                    // return from fullscreen and pause the active web view when the user
                    // starts scrolling - important because otherwise embedded content in
                    // the web view will continue to play
                    onRequestFullScreen(false);
                    ReaderPostDetailFragment fragment = getActiveDetailFragment();
                    if (fragment != null) {
                        fragment.pauseWebView();
                    }
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
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
        super.onSaveInstanceState(outState);
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
    public void onBackPressed() {
        ReaderPostDetailFragment fragment = getActiveDetailFragment();
        if (fragment != null && fragment.isCustomViewShowing()) {
            // if fullscreen video is showing, hide the custom view rather than navigate back
            fragment.hideCustomView();
        } else if (fragment != null && fragment.isAddCommentBoxShowing()) {
            // if comment reply entry is showing, hide it rather than navigate back
            fragment.hideAddCommentBox();
        } else {
            super.onBackPressed();
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

    private ReaderPostDetailFragment getActiveDetailFragment() {
        if (mViewPager == null || mPageAdapter == null) {
            return null;
        }

        Fragment fragment = mPageAdapter.getFragmentAtPosition(mViewPager.getCurrentItem());
        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    private class PostPagerAdapter extends FragmentStatePagerAdapter {
        private final ReaderBlogIdPostIdList mIdList;
        private final long END_ID = -1;

        // this is used to retain a weak reference to created fragments so we can access them
        // in getFragmentAtPosition() - necessary because we need to pause the web view in
        // the active fragment when the user swipes away from it, but the adapter provides
        // no way to access the active fragment
        private final HashMap<String, WeakReference<Fragment>> mFragmentMap =
                new HashMap<String, WeakReference<Fragment>>();

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList idList) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList) idList.clone();
            // add a bogus entry to the end of the list so we can show PostPagerEndFragment
            // when the user scrolls beyond the last post - note that this is only done
            // if there's more than one post
            if (mIdList.size() > 1 && mIdList.indexOf(END_ID, END_ID) == -1) {
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

            Fragment fragment;
            if (blogId == END_ID && postId == END_ID) {
                fragment = PostPagerEndFragment.newInstance();
            } else {
                fragment = ReaderPostDetailFragment.newInstance(blogId, postId, getPostListType());
            }

            mFragmentMap.put(getItemKey(position), new WeakReference<Fragment>(fragment));

            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragmentMap.remove(getItemKey(position));
            super.destroyItem(container, position, object);
        }

        private String getItemKey(int position) {
            return mIdList.get(position).getBlogId() + ":" + mIdList.get(position).getPostId();
        }

        private Fragment getFragmentAtPosition(int position) {
            if (!isValidPosition(position)) {
                return null;
            }
            String key = getItemKey(position);
            if (!mFragmentMap.containsKey(key)) {
                return null;
            }
            return mFragmentMap.get(key).get();
        }
    }

    /*
     * fragment that appears when user scrolls beyond the last post
     */
    public static class PostPagerEndFragment extends Fragment {
        private TextView mTxtCheckmark;

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

            mTxtCheckmark = (TextView) view.findViewById(R.id.text_checkmark);

            return view;
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            // setUserVisibleHint wasn't available until API 15 (ICE_CREAM_SANDWICH_MR1)
            if (Build.VERSION.SDK_INT >= 15) {
                super.setUserVisibleHint(isVisibleToUser);
            }
            if (isVisibleToUser) {
                showCheckmark();
            } else {
                hideCheckmark();
            }
        }

        private void showCheckmark() {
            if (!isVisible()) {
                return;
            }

            mTxtCheckmark.setVisibility(View.VISIBLE);

            AnimatorSet set = new AnimatorSet();
            set.setDuration(750);
            set.setInterpolator(new OvershootInterpolator());
            set.playTogether(ObjectAnimator.ofFloat(mTxtCheckmark, "scaleX", 0.25f, 1f),
                             ObjectAnimator.ofFloat(mTxtCheckmark, "scaleY", 0.25f, 1f));
            set.start();
        }

        private void hideCheckmark() {
            if (isVisible()) {
                mTxtCheckmark.setVisibility(View.INVISIBLE);
            }
        }
    }
}
