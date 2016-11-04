package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderCommentListActivity.DirectOperation;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.ui.reader.services.ReaderPostService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.util.HashSet;

import de.greenrobot.event.EventBus;

/*
 * shows reader post detail fragments in a ViewPager - primarily used for easy swiping between
 * posts with a specific tag or in a specific blog, but can also be used to show a single
 * post detail
 */
public class ReaderPostPagerActivity extends AppCompatActivity
        implements ReaderInterfaces.AutoHideToolbarListener {
    private static final String KEY_TRACKED_POST = "tracked_post";

    private WPViewPager mViewPager;
    private ProgressBar mProgress;
    private Toolbar mToolbar;

    private ReaderTag mCurrentTag;
    private boolean mIsFeed;
    private long mBlogId;
    private long mPostId;
    private String mBlogSlug;
    private String mPostSlug;
    private int mCommentId;
    private DirectOperation mDirectOperation;
    private String mInterceptedUri;
    private int mLastSelectedPosition = -1;
    private ReaderPostListType mPostListType;

    private boolean mIsRequestingMorePosts;
    private boolean mIsSinglePostView;
    private boolean mIsRelatedPostView;

    private final HashSet<Integer> mTrackedPositions = new HashSet<>();
    private boolean mTrackedPost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_pager);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mViewPager = (WPViewPager) findViewById(R.id.viewpager);
        mProgress = (ProgressBar) findViewById(R.id.progress_loading);

        if (savedInstanceState != null) {
            mIsFeed = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_FEED);
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mBlogSlug = savedInstanceState.getString(ReaderConstants.ARG_BLOG_SLUG);
            mPostSlug = savedInstanceState.getString(ReaderConstants.ARG_POST_SLUG);
            mDirectOperation = (DirectOperation) savedInstanceState
                    .getSerializable(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = savedInstanceState.getInt(ReaderConstants.ARG_COMMENT_ID);
            mIsSinglePostView = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_SINGLE_POST);
            mIsRelatedPostView = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_RELATED_POST);
            mInterceptedUri = savedInstanceState.getString(ReaderConstants.ARG_INTERCEPTED_URI);
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
            mTrackedPost = savedInstanceState.getBoolean(KEY_TRACKED_POST);
        } else {
            mIsFeed = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_FEED, false);
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
            mBlogSlug = getIntent().getStringExtra(ReaderConstants.ARG_BLOG_SLUG);
            mPostSlug = getIntent().getStringExtra(ReaderConstants.ARG_POST_SLUG);
            mDirectOperation = (DirectOperation) getIntent()
                    .getSerializableExtra(ReaderConstants.ARG_DIRECT_OPERATION);
            mCommentId = getIntent().getIntExtra(ReaderConstants.ARG_COMMENT_ID, 0);
            mIsSinglePostView = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_SINGLE_POST, false);
            mIsRelatedPostView = getIntent().getBooleanExtra(ReaderConstants.ARG_IS_RELATED_POST, false);
            mInterceptedUri = getIntent().getStringExtra(ReaderConstants.ARG_INTERCEPTED_URI);
            if (getIntent().hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) getIntent().getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getIntent().hasExtra(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) getIntent().getSerializableExtra(ReaderConstants.ARG_TAG);
            }
        }

        if (mPostListType == null) {
            mPostListType = ReaderPostListType.TAG_FOLLOWED;
        }

        setTitle(mIsRelatedPostView ? R.string.reader_title_related_post_detail : R.string.reader_title_post_detail);

        // for related posts, show an X in the toolbar which closes the activity - using the
        // back button will navigate through related posts
        if (mIsRelatedPostView) {
            mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                onShowHideToolbar(true);
                trackPostAtPositionIfNeeded(position);

                // pause the previous web view - important because otherwise embedded content
                // will continue to play
                if (mLastSelectedPosition > -1 && mLastSelectedPosition != position) {
                    ReaderPostDetailFragment lastFragment = getDetailFragmentAtPosition(mLastSelectedPosition);
                    if (lastFragment != null) {
                        lastFragment.pauseWebView();
                    }
                }

                // resume the newly active webView if it was previously paused
                ReaderPostDetailFragment thisFragment = getDetailFragmentAtPosition(position);
                if (thisFragment != null) {
                    thisFragment.resumeWebViewIfPaused();
                }

                mLastSelectedPosition = position;
            }
        });

        mViewPager.setPageTransformer(false,
                new ReaderViewPagerTransformer(ReaderViewPagerTransformer.TransformType.SLIDE_OVER));
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        if (!hasPagerAdapter()) {
            if (mBlogSlug == null) {
                loadPosts(mBlogId, mPostId);
            } else {
                loadPost(mBlogSlug, mPostSlug);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasPagerAdapter() {
        return (mViewPager != null && mViewPager.getAdapter() != null);
    }

    private PostPagerAdapter getPagerAdapter() {
        if (mViewPager != null && mViewPager.getAdapter() != null) {
            return (PostPagerAdapter) mViewPager.getAdapter();
        } else {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(ReaderConstants.ARG_IS_SINGLE_POST, mIsSinglePostView);
        outState.putBoolean(ReaderConstants.ARG_IS_RELATED_POST, mIsRelatedPostView);
        outState.putString(ReaderConstants.ARG_INTERCEPTED_URI, mInterceptedUri);

        outState.putSerializable(ReaderConstants.ARG_DIRECT_OPERATION, mDirectOperation);
        outState.putInt(ReaderConstants.ARG_COMMENT_ID, mCommentId);

        if (hasCurrentTag()) {
            outState.putSerializable(ReaderConstants.ARG_TAG, getCurrentTag());
        }
        if (getPostListType() != null) {
            outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());
        }

        ReaderBlogIdPostId id = getAdapterCurrentBlogIdPostId();
        if (id != null) {
            outState.putLong(ReaderConstants.ARG_BLOG_ID, id.getBlogId());
            outState.putLong(ReaderConstants.ARG_POST_ID, id.getPostId());
        }

        outState.putString(ReaderConstants.ARG_BLOG_SLUG, mBlogSlug);
        outState.putString(ReaderConstants.ARG_POST_SLUG, mPostSlug);

        outState.putBoolean(KEY_TRACKED_POST, mTrackedPost);

        super.onSaveInstanceState(outState);
    }

    private ReaderBlogIdPostId getAdapterCurrentBlogIdPostId() {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getCurrentBlogIdPostId();
    }

    private ReaderBlogIdPostId getAdapterBlogIdPostIdAtPosition(int position) {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getBlogIdPostIdAtPosition(position);
    }

    @Override
    public void onBackPressed() {
        ReaderPostDetailFragment fragment = getActiveDetailFragment();
        if (fragment != null && fragment.isCustomViewShowing()) {
            // if full screen video is showing, hide the custom view rather than navigate back
            fragment.hideCustomView();
        } else if (fragment != null && fragment.goBackInPostHistory()) {
            // noop - fragment moved back to a previous post
        } else {
            super.onBackPressed();
        }
    }

    /*
     * perform analytics tracking and bump the page view for the post at the passed position
     * if it hasn't already been done
     */
    private void trackPostAtPositionIfNeeded(int position) {
        if (!hasPagerAdapter() || mTrackedPositions.contains(position)) return;

        ReaderBlogIdPostId idPair = getAdapterBlogIdPostIdAtPosition(position);
        if (idPair == null) return;

        AppLog.d(AppLog.T.READER, "reader pager > tracking post at position " + position);
        mTrackedPositions.add(position);

        // bump the page view
        ReaderPostActions.bumpPageViewForPost(idPair.getBlogId(), idPair.getPostId());

        // analytics tracking
        AnalyticsUtils.trackWithReaderPostDetails(
                AnalyticsTracker.Stat.READER_ARTICLE_OPENED,
                ReaderPostTable.getBlogPost(idPair.getBlogId(), idPair.getPostId(), true));
    }

    /*
     * perform analytics tracking and bump the page view for the post if it hasn't already been done
     */
    private void trackPostSlugIfNeeded() {
        AppLog.d(AppLog.T.READER, "reader pager > tracking post via slug");
        mTrackedPost = true;

        // bump the page view
        ReaderPostActions.bumpPageViewForPost(mBlogSlug, mPostSlug);

        // analytics tracking
        AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_ARTICLE_OPENED, ReaderPostTable
                .getBlogPost(mBlogSlug, mPostSlug, true));
    }

    /*
     * loads the blogId/postId pairs used to populate the pager adapter - passed blogId/postId will
     * be made active after loading unless gotoNext=true, in which case the post after the passed
     * one will be made active
     */
    private void loadPosts(final long blogId, final long postId) {
        new Thread() {
            @Override
            public void run() {
                final ReaderBlogIdPostIdList idList;
                if (mIsSinglePostView) {
                    idList = new ReaderBlogIdPostIdList();
                    idList.add(new ReaderBlogIdPostId(blogId, postId));
                } else {
                    int maxPosts = ReaderConstants.READER_MAX_POSTS_TO_DISPLAY;
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                        case TAG_PREVIEW:
                            idList = ReaderPostTable.getBlogIdPostIdsWithTag(getCurrentTag(), maxPosts);
                            break;
                        case BLOG_PREVIEW:
                            idList = ReaderPostTable.getBlogIdPostIdsInBlog(blogId, maxPosts);
                            break;
                        default:
                            return;
                    }
                }

                final int currentPosition = mViewPager.getCurrentItem();
                final int newPosition = idList.indexOf(blogId, postId);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppLog.d(AppLog.T.READER, "reader pager > creating adapter");
                        PostPagerAdapter adapter =
                                new PostPagerAdapter(getFragmentManager(), idList);
                        mViewPager.setAdapter(adapter);
                        if (adapter.isValidPosition(newPosition)) {
                            mViewPager.setCurrentItem(newPosition);
                            trackPostAtPositionIfNeeded(newPosition);
                        } else if (adapter.isValidPosition(currentPosition)) {
                            mViewPager.setCurrentItem(currentPosition);
                            trackPostAtPositionIfNeeded(currentPosition);
                        }
                    }
                });
            }
        }.start();
    }

    /*
     * loads the blogSlug/postSlug
     */
    private void loadPost(String blogSlug, String postSlug) {
        AppLog.d(AppLog.T.READER, "reader pager > creating adapter");
        mViewPager.setAdapter(new PostPagerAdapter(getFragmentManager(), blogSlug, postSlug));
        trackPostSlugIfNeeded();
    }

    private ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private ReaderPostListType getPostListType() {
        return mPostListType;
    }

    private Fragment getActivePagerFragment() {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getActiveFragment();
    }

    private ReaderPostDetailFragment getActiveDetailFragment() {
        Fragment fragment = getActivePagerFragment();
        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    private Fragment getPagerFragmentAtPosition(int position) {
        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) {
            return null;
        }
        return adapter.getFragmentAtPosition(position);
    }

    private ReaderPostDetailFragment getDetailFragmentAtPosition(int position) {
        Fragment fragment = getPagerFragmentAtPosition(position);
        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    /*
     * called when user scrolls towards the last posts - requests older posts with the
     * current tag or in the current blog
     */
    private void requestMorePosts() {
        if (mIsRequestingMorePosts) return;

        AppLog.d(AppLog.T.READER, "reader pager > requesting older posts");
        switch (getPostListType()) {
            case TAG_PREVIEW:
            case TAG_FOLLOWED:
                ReaderPostService.startServiceForTag(
                        this,
                        getCurrentTag(),
                        ReaderPostService.UpdateAction.REQUEST_OLDER);
                break;

            case BLOG_PREVIEW:
                if (mBlogSlug == null) {
                    ReaderPostService.startServiceForBlog(
                            this,
                            mBlogId,
                            ReaderPostService.UpdateAction.REQUEST_OLDER);
                } else {
                    ReaderPostService.startServiceForBlog(
                            this,
                            mBlogSlug,
                            ReaderPostService.UpdateAction.REQUEST_OLDER);
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsStarted event) {
        if (isFinishing()) return;

        mIsRequestingMorePosts = true;
        mProgress.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ReaderEvents.UpdatePostsEnded event) {
        if (isFinishing()) return;

        PostPagerAdapter adapter = getPagerAdapter();
        if (adapter == null) return;

        mIsRequestingMorePosts = false;
        mProgress.setVisibility(View.GONE);

        if (event.getResult() == ReaderActions.UpdateResult.HAS_NEW) {
            AppLog.d(AppLog.T.READER, "reader pager > older posts received");
            if (mBlogSlug == null) {
                // remember which post to keep active
                ReaderBlogIdPostId id = adapter.getCurrentBlogIdPostId();
                long blogId = (id != null ? id.getBlogId() : 0);
                long postId = (id != null ? id.getPostId() : 0);
                loadPosts(blogId, postId);
            } else {
                loadPost(mBlogSlug, mPostSlug);
            }
        } else {
            AppLog.d(AppLog.T.READER, "reader pager > all posts loaded");
            adapter.mAllPostsLoaded = true;
        }
    }

    /*
     * called by detail fragment to show/hide the toolbar when user scrolls
     */
    @Override
    public void onShowHideToolbar(boolean show) {
        if (!isFinishing()) {
            AniUtils.animateTopBar(mToolbar, show);
        }
    }

    /**
     * pager adapter containing post detail fragments
     **/
    private class PostPagerAdapter extends FragmentStatePagerAdapter {
        private ReaderBlogIdPostIdList mIdList;
        private String mSingleBlogSlug;
        private String mSinglePostSlug;
        private boolean mAllPostsLoaded;

        // this is used to retain created fragments so we can access them in
        // getFragmentAtPosition() - necessary because the pager provides no
        // built-in way to do this - note that destroyItem() removes fragments
        // from this map when they're removed from the adapter, so this doesn't
        // retain *every* fragment
        private final SparseArray<Fragment> mFragmentMap = new SparseArray<>();

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList ids) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList)ids.clone();
        }

        PostPagerAdapter(FragmentManager fm, String blogSlug, String postSlug) {
            super(fm);
            mSingleBlogSlug = blogSlug;
            mSinglePostSlug = postSlug;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // work around "Fragement no longer exists for key" Android bug
            // by catching the IllegalStateException
            // https://code.google.com/p/android/issues/detail?id=42601
            try {
                AppLog.d(AppLog.T.READER, "reader pager > adapter restoreState");
                super.restoreState(state, loader);
            } catch (IllegalStateException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        }

        @Override
        public Parcelable saveState() {
            AppLog.d(AppLog.T.READER, "reader pager > adapter saveState");
            return super.saveState();
        }

        private boolean canRequestMostPosts() {
            return !mAllPostsLoaded
                && !mIsSinglePostView
                && (mIdList != null && mIdList.size() < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY)
                && NetworkUtils.isNetworkAvailable(ReaderPostPagerActivity.this);
        }

        boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        @Override
        public int getCount() {
            return mSingleBlogSlug != null ? 1 : mIdList.size();
        }

        @Override
        public Fragment getItem(int position) {
            if ((position == getCount() - 1) && canRequestMostPosts()) {
                requestMorePosts();
            }

            if (mSingleBlogSlug == null) {
                return ReaderPostDetailFragment.newInstance(
                        mIsFeed,
                        mIdList.get(position).getBlogId(),
                        mIdList.get(position).getPostId(),
                        mDirectOperation,
                        mCommentId,
                        mIsRelatedPostView,
                        mInterceptedUri,
                        getPostListType());
            } else {
                return ReaderPostDetailFragment.newInstance(
                        mSingleBlogSlug,
                        mSinglePostSlug,
                        mDirectOperation,
                        mCommentId,
                        mIsRelatedPostView,
                        mInterceptedUri,
                        getPostListType());
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object item = super.instantiateItem(container, position);
            if (item instanceof Fragment) {
                mFragmentMap.put(position, (Fragment) item);
            }
            return item;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            mFragmentMap.remove(position);
            super.destroyItem(container, position, object);
        }

        private Fragment getActiveFragment() {
            return getFragmentAtPosition(mViewPager.getCurrentItem());
        }

        private Fragment getFragmentAtPosition(int position) {
            if (isValidPosition(position)) {
                return mFragmentMap.get(position);
            } else {
                return null;
            }
        }

        private ReaderBlogIdPostId getCurrentBlogIdPostId() {
            return getBlogIdPostIdAtPosition(mViewPager.getCurrentItem());

        }

        ReaderBlogIdPostId getBlogIdPostIdAtPosition(int position) {
            if (isValidPosition(position)) {
                return mIdList.get(position);
            } else {
                return null;
            }
        }
    }
}
