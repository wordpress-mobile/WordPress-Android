package org.wordpress.android.ui.reader;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.cocosw.undobar.UndoBarController;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderAnim.AnimationEndListener;
import org.wordpress.android.ui.reader.ReaderAnim.Duration;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
import org.wordpress.android.ui.reader.utils.ReaderTips;
import org.wordpress.android.ui.reader.utils.ReaderTips.ReaderTipType;
import org.wordpress.android.ui.reader.views.ReaderViewPager;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import javax.annotation.Nonnull;

/*
 * shows reader post detail fragments in a ViewPager - primarily used for easy swiping between
 * posts with a specific tag or in a specific blog, but can also be used to show a single
 * post detail
 */
public class ReaderPostPagerActivity extends Activity
        implements ReaderInterfaces.FullScreenListener,
                   ReaderInterfaces.OnPostPopupListener {

    private ReaderViewPager mViewPager;
    private ProgressBar mProgress;

    private ReaderTag mCurrentTag;
    private long mBlogId;
    private long mPostId;
    private ReaderPostListType mPostListType;

    private boolean mIsFullScreen;
    private boolean mIsRequestingMorePosts;
    private boolean mIsSinglePostView;
    private boolean mHasAlreadyLoaded;

    private static final String ARG_IS_SINGLE_POST = "is_single_post";
    private static final String ARG_HAS_ALREADY_LOADED = "has_loaded";

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

        mViewPager = (ReaderViewPager) findViewById(R.id.viewpager);
        mProgress = (ProgressBar) findViewById(R.id.progress_loading);

        final String title;
        if (savedInstanceState != null) {
            title = savedInstanceState.getString(ReaderConstants.ARG_TITLE);
            mBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            mPostId = savedInstanceState.getLong(ReaderConstants.ARG_POST_ID);
            mIsSinglePostView = savedInstanceState.getBoolean(ARG_IS_SINGLE_POST);
            mHasAlreadyLoaded = savedInstanceState.getBoolean(ARG_HAS_ALREADY_LOADED);
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
        } else {
            title = getIntent().getStringExtra(ReaderConstants.ARG_TITLE);
            mBlogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
            mPostId = getIntent().getLongExtra(ReaderConstants.ARG_POST_ID, 0);
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
        }

        if (!TextUtils.isEmpty(title)) {
            this.setTitle(title);
        }

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                onRequestFullScreen(false);
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE);
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
                    // don't show swipe tip in the future since user obviously knows how to swipe
                    ReaderTips.setTipShown(ReaderTipType.READER_SWIPE_POSTS);
                    ReaderTips.hideTip(ReaderPostPagerActivity.this);
                }
            }
        });

        mViewPager.setPageTransformer(false,
                new ReaderViewPagerTransformer(ReaderViewPagerTransformer.TransformType.SLIDE_OVER));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPagerAdapter()) {
            loadPosts(mBlogId, mPostId);
        }
    }

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
    protected void onSaveInstanceState(@Nonnull Bundle outState) {
        outState.putString(ReaderConstants.ARG_TITLE, (String) this.getTitle());
        outState.putBoolean(ARG_IS_SINGLE_POST, mIsSinglePostView);
        outState.putBoolean(ARG_HAS_ALREADY_LOADED, mHasAlreadyLoaded);

        if (hasCurrentTag()) {
            outState.putSerializable(ReaderConstants.ARG_TAG, getCurrentTag());
        }
        if (getPostListType() != null) {
            outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());
        }

        if (hasPagerAdapter()) {
            ReaderBlogIdPostId id = getPagerAdapter().getCurrentBlogIdPostId();
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
        } else {
            super.onBackPressed();
            if (isFullScreenSupported()) {
                overridePendingTransition(R.anim.reader_activity_scale_in, R.anim.reader_activity_slide_out);
            }
        }
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
                        PostPagerAdapter adapter = new PostPagerAdapter(getFragmentManager(), idList);
                        mViewPager.setAdapter(adapter);
                        if (adapter.isValidPosition(newPosition)) {
                            mViewPager.setCurrentItem(newPosition);
                        } else if (adapter.isValidPosition(currentPosition)) {
                            mViewPager.setCurrentItem(currentPosition);
                        }
                        // let user know they can swipe through posts the first time around
                        if (!mHasAlreadyLoaded && idList.size() > 1) {
                            ReaderTips.showTipDelayed(ReaderPostPagerActivity.this, ReaderTipType.READER_SWIPE_POSTS);
                        }
                        mHasAlreadyLoaded = true;
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

    private Fragment getActivePagerFragment() {
        if (hasPagerAdapter()) {
            return getPagerAdapter().getActiveFragment();
        } else {
            return null;
        }
    }

    private ReaderPostDetailFragment getActiveDetailFragment() {
        Fragment fragment = getActivePagerFragment();
        if (fragment instanceof ReaderPostDetailFragment) {
            return (ReaderPostDetailFragment) fragment;
        } else {
            return null;
        }
    }

    /*
     * user tapped the dropdown arrow to the right of the title on a detail fragment
     */
    @Override
    public void onShowPostPopup(View view, final ReaderPost post) {
        if (view == null || post == null) {
            return;
        }

        PopupMenu popup = new PopupMenu(this, view);
        MenuItem menuItem = popup.getMenu().add(getString(R.string.reader_menu_block_blog));
        menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                blockBlogForPost(post.blogId, post.postId);
                return true;
            }
        });
        popup.show();
    }

    /*
     * blocks the blog associated with the passed post and removes all posts in that blog
     */
    private void blockBlogForPost(final long blogId, final long postId) {
        if (!NetworkUtils.checkConnection(this)) {
            return;
        }

        Fragment fragment = getActivePagerFragment();
        if (fragment == null) {
            return;
        }

        // perform call to block this blog - returns list of posts deleted by blocking so
        // they can be restored if the user undoes the block
        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (!succeeded && !isFinishing()) {
                    hideUndoBar();
                    ToastUtils.showToast(
                            ReaderPostPagerActivity.this,
                            R.string.reader_toast_err_block_blog,
                            ToastUtils.Duration.LONG);
                }
            }
        };
        final BlockedBlogResult blockResult = ReaderBlogActions.blockBlogFromReader(blogId, actionListener);
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOCKED_BLOG);

        // animate out the active fragment
        AnimationEndListener animEndListener = new AnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                blockBlogForPostCompleted(blogId, postId, blockResult);
            }
        };
        ReaderAnim.scaleOut(fragment.getView(), View.INVISIBLE, Duration.SHORT, animEndListener);
    }

    /*
     * called after successfully blocking a blog and animating out the active fragment
     */
    private void blockBlogForPostCompleted(final long blogId,
                                           final long postId,
                                           final BlockedBlogResult blockResult) {
        if (isFinishing()) {
            return;
        }

        // show the undo bar - on undo we restore the deleted posts, and reselect the
        // one the blog was blocked from
        UndoBarController.UndoListener undoListener = new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable parcelable) {
                ReaderBlogActions.undoBlockBlogFromReader(blockResult);
                loadPosts(blogId, postId);
            }
        };
        new UndoBarController.UndoBar(ReaderPostPagerActivity.this)
                .message(getString(R.string.reader_toast_blog_blocked))
                .listener(undoListener)
                .translucent(true)
                .show();

        // reload the adapter and move to the best post not in the blocked blog
        int position = mViewPager.getCurrentItem();
        ReaderBlogIdPostId newId = (hasPagerAdapter() ? getPagerAdapter().getBestIdNotInBlog(position, blogId) : null);
        long newBlogId = (newId != null ? newId.getBlogId() : 0);
        long newPostId = (newId != null ? newId.getPostId() : 0);
        loadPosts(newBlogId, newPostId);
    }

    private void hideUndoBar() {
        if (!isFinishing()) {
            UndoBarController.clear(this);
        }
    }

    /**
     * pager adapter containing post detail fragments
     **/
    private class PostPagerAdapter extends FragmentStatePagerAdapter {
        private ReaderBlogIdPostIdList mIdList = new ReaderBlogIdPostIdList();
        private boolean mAllPostsLoaded;

        // this is used to retain created fragments so we can access them in
        // getFragmentAtPosition() - necessary because the pager provides no
        // built-in way to do this - note that destroyItem() removes fragments
        // from this map when they're removed from the adapter, so this doesn't
        // retain *every* fragment
        private final SparseArray<Fragment> mFragmentMap = new SparseArray<Fragment>();

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList ids) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList)ids.clone();
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
                && mIdList.size() < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY
                && NetworkUtils.isNetworkAvailable(ReaderPostPagerActivity.this);
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
            if ((position == getCount() - 1) && canRequestMostPosts()) {
                requestMorePosts();
            }

            boolean disableBlockBlog = mIsSinglePostView;
            return ReaderPostDetailFragment.newInstance(
                    mIdList.get(position).getBlogId(),
                    mIdList.get(position).getPostId(),
                    disableBlockBlog,
                    getPostListType());
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
            int position = mViewPager.getCurrentItem();
            if (isValidPosition(position)) {
                return mIdList.get(position);
            } else {
                return null;
            }
        }

        /*
         * returns the id pair of the previous/next post that isn't in the same blog
         * as the one at the passed position
         */
        private ReaderBlogIdPostId getBestIdNotInBlog(int position, long blogId) {
            if (!isValidPosition(position)) {
                return null;
            }

            // search backwards
            if (position > 0) {
                for (int index = position - 1; index >= 0; index--) {
                    if (mIdList.get(index).getBlogId() != blogId) {
                        return mIdList.get(index);
                    }
                }
            }

            // search forwards
            if (position < getCount() - 1) {
                for (int index = position + 1; index < getCount(); index++) {
                    if (mIdList.get(index).getBlogId() != blogId) {
                        return mIdList.get(index);
                    }
                }
            }

            return null;
        }

        private void requestMorePosts() {
            if (mIsRequestingMorePosts) {
                return;
            }

            mIsRequestingMorePosts = true;
            mProgress.setVisibility(View.VISIBLE);
            AppLog.d(AppLog.T.READER, "reader pager > requesting older posts");

            ReaderActions.UpdateResultListener resultListener = new ReaderActions.UpdateResultListener() {
                @Override
                public void onUpdateResult(ReaderActions.UpdateResult result) {
                    doAfterUpdate(result);
                }
            };

            switch (getPostListType()) {
                case TAG_PREVIEW:
                case TAG_FOLLOWED:
                    ReaderPostActions.updatePostsInTag(
                            getCurrentTag(),
                            ReaderActions.RequestDataAction.LOAD_OLDER,
                            resultListener);
                    break;

                case BLOG_PREVIEW:
                    ReaderPostActions.requestPostsForBlog(
                            mBlogId,
                            null,
                            ReaderActions.RequestDataAction.LOAD_OLDER,
                            resultListener);
                    break;
            }
        }

        private void doAfterUpdate(ReaderActions.UpdateResult result) {
            mIsRequestingMorePosts = false;

            if (isFinishing()) {
                return;
            }

            mProgress.setVisibility(View.GONE);

            if (result == ReaderActions.UpdateResult.HAS_NEW) {
                AppLog.d(AppLog.T.READER, "reader pager > older posts received");
                // remember which post to keep active
                ReaderBlogIdPostId id = getCurrentBlogIdPostId();
                // if this is an end fragment, get the previous post and tell loadPosts() to
                // move to the post after it (ie: show the first new post)
                long blogId = (id != null ? id.getBlogId() : 0);
                long postId = (id != null ? id.getPostId() : 0);
                loadPosts(blogId, postId);
            } else {
                AppLog.d(AppLog.T.READER, "reader pager > all posts loaded");
                mAllPostsLoaded = true;
            }
        }
    }
}
