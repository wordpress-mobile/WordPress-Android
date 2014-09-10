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

import com.cocosw.undobar.UndoBarController;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderAnim.AnimationEndListener;
import org.wordpress.android.ui.reader.ReaderAnim.Duration;
import org.wordpress.android.ui.reader.ReaderPostPagerEndFragment.EndFragmentType;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener;
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultAndCountListener;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult;
import org.wordpress.android.ui.reader.actions.ReaderPostActions;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;
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

    private ViewPager mViewPager;
    private PostPagerAdapter mPagerAdapter;

    private ReaderTag mCurrentTag;
    private long mCurrentBlogId;
    private ReaderPostListType mPostListType;

    private boolean mIsFullScreen;
    private boolean mIsRequestingMorePosts;
    private boolean mIsSinglePostView;

    protected static final String ARG_IS_SINGLE_POST = "is_single_post";

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
                onRequestFullScreen(false);

                if (hasPagerAdapter()) {
                    Fragment fragment = getPagerAdapter().getFragmentAtPosition(position);
                    if (fragment instanceof ReaderPostDetailFragment) {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_OPENED_ARTICLE);
                    } else if (fragment instanceof ReaderPostPagerEndFragment) {
                        // if the end fragment is now active and more posts can be requested,
                        // request them now
                        if (getPagerAdapter().canRequestMostPosts()) {
                            getPagerAdapter().requestMorePosts();
                        }
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

        mViewPager.setPageTransformer(false,
                new ReaderViewPagerTransformer(ReaderViewPagerTransformer.TransformType.SLIDE_OVER));
    }

    private boolean hasPagerAdapter() {
        return (mPagerAdapter != null);
    }

    private PostPagerAdapter getPagerAdapter() {
        return mPagerAdapter;
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
    private void loadPosts(final long blogId,
                           final long postId,
                           final boolean gotoNext) {
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
                final int newPosition;
                if (gotoNext) {
                    newPosition = idList.indexOf(blogId, postId) + 1;
                } else {
                    newPosition = idList.indexOf(blogId, postId);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPagerAdapter = new PostPagerAdapter(getFragmentManager());
                        mPagerAdapter.showPosts(idList);
                        mViewPager.setAdapter(mPagerAdapter);
                        if (mPagerAdapter.isValidPosition(newPosition)) {
                            mViewPager.setCurrentItem(newPosition);
                        } else if (mPagerAdapter.isValidPosition(currentPosition)) {
                            mViewPager.setCurrentItem(currentPosition);
                        }
                        mPagerAdapter.updateEndFragmentIfActive();
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
        if (!hasPagerAdapter()) {
            return null;
        }

        Fragment fragment = getPagerAdapter().getActiveFragment();
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

        Fragment fragment = getPagerAdapter().getActiveFragment();
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
                loadPosts(blogId, postId, false);
            }
        };
        new UndoBarController.UndoBar(ReaderPostPagerActivity.this)
                .message(getString(R.string.reader_toast_blog_blocked))
                .listener(undoListener)
                .translucent(true)
                .show();

        // reload the adapter and move to the best post not in the blocked blog
        int position = mViewPager.getCurrentItem();
        ReaderBlogIdPostId newId = getPagerAdapter().getBestIdNotInBlog(position, blogId);
        long newBlogId = (newId != null ? newId.getBlogId() : 0);
        long newPostId = (newId != null ? newId.getPostId() : 0);
        loadPosts(newBlogId, newPostId, false);
    }

    private void hideUndoBar() {
        if (!isFinishing()) {
            UndoBarController.clear(this);
        }
    }

    private static boolean isEndFragmentId(ReaderBlogIdPostId id) {
        return (id != null
                && id.getBlogId() == ReaderPostPagerEndFragment.END_FRAGMENT_ID
                && id.getPostId() == ReaderPostPagerEndFragment.END_FRAGMENT_ID);
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

        PostPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private boolean hasEndFragment() {
            return (mIdList.indexOf(
                        ReaderPostPagerEndFragment.END_FRAGMENT_ID,
                        ReaderPostPagerEndFragment.END_FRAGMENT_ID) > -1);
        }

        void showPosts(ReaderBlogIdPostIdList ids) {
            mIdList = (ReaderBlogIdPostIdList)ids.clone();
            // add a bogus entry to the end of the list which tells the adapter to show
            // the end fragment after the last post
            if (!mIsSinglePostView && !hasEndFragment()) {
                mIdList.add(new ReaderBlogIdPostId(ReaderPostPagerEndFragment.END_FRAGMENT_ID,
                                                   ReaderPostPagerEndFragment.END_FRAGMENT_ID));
            }
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
            if (isEndFragmentId(mIdList.get(position))) {
                EndFragmentType fragmentType =
                        (canRequestMostPosts() ? EndFragmentType.LOADING : EndFragmentType.NO_MORE);
                return ReaderPostPagerEndFragment.newInstance(fragmentType);
            } else {
                boolean disableBlockBlog = mIsSinglePostView;
                return ReaderPostDetailFragment.newInstance(
                        mIdList.get(position).getBlogId(),
                        mIdList.get(position).getPostId(),
                        disableBlockBlog,
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
            mIsRequestingMorePosts = false;

            if (isFinishing()) {
                return;
            }

            if (hasNewPosts) {
                AppLog.d(AppLog.T.READER, "reader pager > older posts received");
                // remember which post to keep active
                ReaderBlogIdPostId id = getCurrentBlogIdPostId();
                boolean gotoNext;
                // if this is an end fragment, get the previous post and tell loadPosts() to
                // move to the post after it (ie: show the first new post)
                if (isEndFragmentId(id)) {
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
                updateEndFragmentIfActive();
            }
        }

        /*
         * if the end fragment is active, make sure it reflects whether more posts can
         * be requested or we've hit the last post
         */
        private void updateEndFragmentIfActive() {
            Fragment fragment = getActiveFragment();
            if (fragment instanceof ReaderPostPagerEndFragment) {
                ((ReaderPostPagerEndFragment) fragment).setEndFragmentType(
                        canRequestMostPosts() ? EndFragmentType.LOADING : EndFragmentType.NO_MORE);
            }
        }
    }
}
