package org.wordpress.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.networking.NetworkUtils;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultAndCountListener;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.util.AppLog;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

/*
 * shows reader post detail fragments in a ViewPager - primarily used for easy swiping between
 * posts with a specific tag or in a specific blog, but can also be used to show a single
 * post detail
 */
public class ReaderPostPagerActivity extends Activity
        implements ReaderUtils.FullScreenListener {

    private ViewPager mViewPager;
    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private ReaderPostListType mPostListType;

    private boolean mIsFullScreen;
    private boolean mIsRequestingMorePosts;
    private boolean mIsSinglePostView;

    protected static final String ARG_IS_SINGLE_POST = "is_single_post";

    // IDs for non-post fragments (no more posts & loading posts) - must be < 0
    private static final long NO_MORE_FRAGMENT_ID = -1;
    private static final long LOADING_FRAGMENT_ID = -2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (isFullScreenSupported()) {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_pager);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        final String title;
        final long blogId;
        final long postId;
        if (savedInstanceState != null) {
            title = savedInstanceState.getString(ReaderConstants.ARG_TITLE);
            blogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            postId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mIsSinglePostView = savedInstanceState.getBoolean(ARG_IS_SINGLE_POST);
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
        } else {
            title = getIntent().getStringExtra(ReaderConstants.ARG_TITLE);
            blogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            postId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
            mIsSinglePostView = getIntent().getBooleanExtra(ARG_IS_SINGLE_POST, false);
            if (getIntent().hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) getIntent().getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getIntent().hasExtra(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) getIntent().getSerializableExtra(ReaderConstants.ARG_TAG);
            }
        }

        if (mPostListType == null) {
            mPostListType = ReaderPostListType.TAG_FOLLOWED;
        } else if (mPostListType == ReaderPostListType.BLOG_PREVIEW) {
            mCurrentBlogId = blogId;
        }

        if (!TextUtils.isEmpty(title)) {
            this.setTitle(title);
        }

        loadPosts(blogId, postId, false);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE);

                onRequestFullScreen(false);
                // request older posts when the loading fragment appears
                if (mViewPager.getAdapter() != null) {
                    PostPagerAdapter adapter = (PostPagerAdapter) mViewPager.getAdapter();
                    ReaderBlogIdPostId id = adapter.mIdList.get(position);
                    if (id != null
                            && id.getBlogId() == LOADING_FRAGMENT_ID
                            && id.getPostId() == LOADING_FRAGMENT_ID) {
                        adapter.requestMorePosts();
                    }
                }
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
    protected void onSaveInstanceState(@Nonnull Bundle outState) {
        outState.putString(ReaderConstants.ARG_TITLE, (String) this.getTitle());
        outState.putBoolean(ARG_IS_SINGLE_POST, mIsSinglePostView);

        if (hasCurrentTag()) {
            outState.putSerializable(ReaderConstants.ARG_TAG, getCurrentTag());
        }
        if (getPostListType() != null) {
            outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());
        }

        if (mViewPager != null && mViewPager.getAdapter() != null) {
            PostPagerAdapter adapter = (PostPagerAdapter) mViewPager.getAdapter();
            ReaderBlogIdPostId id = adapter.getCurrentBlogIdPostId();
            if (id != null) {
                outState.putLong(ReaderConstants.ARG_BLOG_ID, id.getBlogId());
                outState.putLong(ReaderConstants.ARG_POST_ID, id.getPostId());
            }
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

    /*
     * loads the posts used to populate the pager adapter - passed blogId/postId will be made
     * active after loading unless gotoNext=true, in which case the post after the passed one
     * will be made active
     */
    private void loadPosts(final long blogId,
                           final long postId,
                           final boolean gotoNext) {
        new Thread() {
            @Override
            public void run() {
                final ReaderPostList postList;
                if (mIsSinglePostView) {
                    ReaderPost post = ReaderPostTable.getPost(blogId, postId);
                    if (post == null) {
                        return;
                    }
                    postList = new ReaderPostList();
                    postList.add(post);
                } else {
                    int maxPosts = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                        case TAG_PREVIEW:
                            postList = ReaderPostTable.getPostsWithTag(getCurrentTag(), maxPosts);
                            break;
                        case BLOG_PREVIEW:
                            postList = ReaderPostTable.getPostsInBlog(blogId, maxPosts);
                            break;
                        default:
                            return;
                    }
                }

                final ReaderBlogIdPostIdList ids = postList.getBlogIdPostIdList();
                final int currentPosition = mViewPager.getCurrentItem();
                final int newPosition;
                if (gotoNext) {
                    newPosition = ids.indexOf(blogId, postId) + 1;
                } else {
                    newPosition = ids.indexOf(blogId, postId);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PostPagerAdapter adapter = new PostPagerAdapter(getFragmentManager(), ids);
                        mViewPager.setAdapter(adapter);
                        if (adapter.isValidPosition(newPosition)) {
                            mViewPager.setCurrentItem(newPosition);
                        } else if (adapter.isValidPosition(currentPosition)) {
                            mViewPager.setCurrentItem(currentPosition);
                        }
                    }
                });
            }
        }.start();
    }

    private ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        boolean isResultOK = (resultCode == Activity.RESULT_OK);
        if (isResultOK && requestCode == ReaderConstants.INTENT_READER_REBLOG) {
            // update the reblog status in the detail view if the user returned
            // from the reblog activity after successfully reblogging
            ReaderPostDetailFragment fragment = getActiveDetailFragment();
            if (fragment != null) {
                fragment.doPostReblogged();
            }
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
        if (mViewPager == null || mViewPager.getAdapter() == null) {
            return null;
        }

        PostPagerAdapter adapter = (PostPagerAdapter) mViewPager.getAdapter();
        Fragment fragment = adapter.getFragmentAtPosition(mViewPager.getCurrentItem());

        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    /**
     * pager adapter containing post detail fragments
     **/
    private class PostPagerAdapter extends FragmentStatePagerAdapter {
        private final ReaderBlogIdPostIdList mIdList;
        private boolean mAllPostsLoaded;

        // this is used to retain a weak reference to created fragments so we can access them
        // in getFragmentAtPosition() - necessary because we need to pause the web view in
        // the active fragment when the user swipes away from it, but the adapter provides
        // no way to access the active fragment
        private final SparseArray<WeakReference<Fragment>> mFragmentMap =
                new SparseArray<WeakReference<Fragment>>();

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList ids) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList)ids.clone();
            checkLastFragment();
        }

        /*
         * add a bogus entry to the end of the list which tells the adapter to show
         * the "no more posts" or loading fragment after the last post depending
         * on whether more posts can be requested
         */
        private void checkLastFragment() {
            // no end fragment in single post view
            if (mIsSinglePostView) {
                return;
            }

            boolean canRequestMore = !mAllPostsLoaded
                    && mIdList.size() < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
                    && NetworkUtils.isNetworkAvailable(ReaderPostPagerActivity.this);

            if (canRequestMore) {
                ensureLoadingPageExists();
            } else {
                ensureNoMorePageExists();
            }
        }

        private void ensureLoadingPageExists() {
            int noMoreIndex = mIdList.indexOf(NO_MORE_FRAGMENT_ID, NO_MORE_FRAGMENT_ID);
            int loadingIndex = mIdList.indexOf(LOADING_FRAGMENT_ID, LOADING_FRAGMENT_ID);
            boolean isChanged = false;

            if (loadingIndex == -1) {
                if (noMoreIndex >= 0) {
                    // loading page doesn't exist but "no more" page does, so replace the
                    // "no more" page with the loading page (should never happen)
                    mIdList.set(noMoreIndex, new ReaderBlogIdPostId(LOADING_FRAGMENT_ID, LOADING_FRAGMENT_ID));
                } else {
                    // loading page doesn't exist and "no more" page doesn't exist, so add
                    // the loading page at the end
                    mIdList.add(new ReaderBlogIdPostId(LOADING_FRAGMENT_ID, LOADING_FRAGMENT_ID));
                }
                isChanged = true;
            } else if (noMoreIndex >= 0) {
                // loading page already exists, make sure the "no more" page is removed
                mIdList.remove(noMoreIndex);
                isChanged = true;
            }

            if (isChanged) {
                notifyDataSetChanged();
            }
        }

        private void ensureNoMorePageExists() {
            int noMoreIndex = mIdList.indexOf(NO_MORE_FRAGMENT_ID, NO_MORE_FRAGMENT_ID);
            int loadingIndex = mIdList.indexOf(LOADING_FRAGMENT_ID, LOADING_FRAGMENT_ID);
            boolean isChanged = false;

            if (noMoreIndex == -1) {
                if (loadingIndex >= 0) {
                    // "no more" page doesn't exist but loading page does, so replace the loading
                    // page with the "no more page"
                    mIdList.set(loadingIndex, new ReaderBlogIdPostId(NO_MORE_FRAGMENT_ID, NO_MORE_FRAGMENT_ID));
                } else {
                    // loading page doesn't exist, so add "no more" page to the end of the list
                    mIdList.add(new ReaderBlogIdPostId(NO_MORE_FRAGMENT_ID, NO_MORE_FRAGMENT_ID));
                }
                isChanged = true;
            } else if (loadingIndex >= 0) {
                // "no more" page already exists, make sure the loading page is removed
                mIdList.remove(loadingIndex);
                isChanged = true;
            }

            if (isChanged) {
                notifyDataSetChanged();
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

            if (blogId == NO_MORE_FRAGMENT_ID && postId == NO_MORE_FRAGMENT_ID) {
                return PostPagerNoMoreFragment.newInstance();
            } else if (blogId == LOADING_FRAGMENT_ID && postId == LOADING_FRAGMENT_ID) {
                return PostPagerLoadingFragment.newInstance();
            } else {
                return ReaderPostDetailFragment.newInstance(blogId, postId, getPostListType());
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);
            if (item instanceof Fragment) {
                mFragmentMap.put(position, new WeakReference<Fragment>((Fragment) item));
            }
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragmentMap.remove(position);
            super.destroyItem(container, position, object);
        }

        private Fragment getFragmentAtPosition(int position) {
            if (isValidPosition(position) && mFragmentMap.get(position) != null) {
                return mFragmentMap.get(position).get();
            } else {
                return null;
            }
        }

        private ReaderBlogIdPostId getCurrentBlogIdPostId() {
            int position = mViewPager.getCurrentItem();
            if (isValidPosition(position)) {
                return mIdList.get(position);
            } else {
                return null;
            }
        }

        private ReaderBlogIdPostId getPreviousBlogIdPostId() {
            int position = mViewPager.getCurrentItem() - 1;
            if (isValidPosition(position)) {
                return mIdList.get(position);
            } else {
                return null;
            }
        }

        private void requestMorePosts() {
            if (mIsRequestingMorePosts) {
                return;
            }

            mIsRequestingMorePosts = true;
            AppLog.d(AppLog.T.READER, "reader pager > requesting older posts");

            switch (getPostListType()) {
                case TAG_PREVIEW:
                case TAG_FOLLOWED:
                    UpdateResultAndCountListener resultListener = new UpdateResultAndCountListener() {
                        @Override
                        public void onUpdateResult(ReaderActions.UpdateResult result, int numNewPosts) {
                            doAfterUpdate(numNewPosts > 0);
                        }
                    };
                    ReaderPostActions.updatePostsInTag(
                            getCurrentTag(),
                            ReaderActions.RequestDataAction.LOAD_OLDER,
                            resultListener);
                    break;

                case BLOG_PREVIEW:
                    ActionListener actionListener = new ActionListener() {
                        @Override
                        public void onActionResult(boolean succeeded) {
                            doAfterUpdate(succeeded);
                        }
                    };
                    ReaderPostActions.requestPostsForBlog(
                            mCurrentBlogId,
                            null,
                            ReaderActions.RequestDataAction.LOAD_OLDER,
                            actionListener);
                    break;
            }
        }

        private void doAfterUpdate(boolean hasNewPosts) {
            if (isFinishing()) {
                return;
            }

            mIsRequestingMorePosts = false;

            if (hasNewPosts) {
                AppLog.d(AppLog.T.READER, "reader pager > older posts received");
                // remember which post to keep active
                ReaderBlogIdPostId id = getCurrentBlogIdPostId();
                boolean gotoNext;
                // if this is an end fragment, get the previous post and
                // tell loadPosts() to move to the post after it
                if (id != null && id.getBlogId() < 0 && id.getPostId() < 0) {
                    id = getPreviousBlogIdPostId();
                    gotoNext = true;
                } else {
                    gotoNext = false;
                }
                long blogId = (id != null ? id.getBlogId() : 0);
                long postId = (id != null ? id.getPostId() : 0);
                loadPosts(blogId, postId, gotoNext);
            } else {
                AppLog.d(AppLog.T.READER, "reader pager > all posts loaded");
                mAllPostsLoaded = true;
                checkLastFragment();
            }
        }
    }

    /*
     * used to animate in the checkmark or progress bar on end fragments
     */
    private static void animateIn(final View target) {
        // don't animate if the target view is already visible
        if (target == null || target.getVisibility() == View.VISIBLE) {
            return;
        }

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.25f, 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.25f, 1.0f);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target, scaleX, scaleY);
        animator.setDuration(750);
        animator.setInterpolator(new OvershootInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                target.setVisibility(View.VISIBLE);
            }
        });

        animator.start();
    }

    /**
     * fragment that appears when user scrolls beyond the last post and no more posts can be loaded
     **/
    public static class PostPagerNoMoreFragment extends Fragment {

        private static PostPagerNoMoreFragment newInstance() {
            return new PostPagerNoMoreFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.reader_fragment_no_more, container, false);
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

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            // setUserVisibleHint wasn't available until API 15 (ICE_CREAM_SANDWICH_MR1)
            if (Build.VERSION.SDK_INT >= 15) {
                super.setUserVisibleHint(isVisibleToUser);
            }

            if (getView() != null) {
                TextView txtCheckmark = (TextView) getView().findViewById(R.id.text_checkmark);
                if (isVisibleToUser) {
                    animateIn(txtCheckmark);
                } else {
                    txtCheckmark.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * fragment that appears while loading older posts
     **/
    public static class PostPagerLoadingFragment extends Fragment {

        private static PostPagerLoadingFragment newInstance() {
            return new PostPagerLoadingFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.reader_fragment_loading, container, false);
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            if (Build.VERSION.SDK_INT >= 15) {
                super.setUserVisibleHint(isVisibleToUser);
            }
            if (isVisibleToUser && getView() != null) {
                ProgressBar progress = (ProgressBar) getView().findViewById(R.id.progress_loading);
                animateIn(progress);
            }
        }
    }
}
