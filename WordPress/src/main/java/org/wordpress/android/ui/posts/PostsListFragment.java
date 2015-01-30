package org.wordpress.android.ui.posts;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.MessageType;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.widgets.FloatingActionButton;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import java.util.List;
import java.util.Vector;

public class PostsListFragment extends ListFragment
        implements WordPress.OnPostUploadedListener {
    public static final int POSTS_REQUEST_COUNT = 20;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnSinglePostLoadedListener mOnSinglePostLoadedListener;
    private PostsListAdapter mPostsListAdapter;
    private FloatingActionButton mFabButton;
    private ApiHelper.FetchPostsTask mCurrentFetchPostsTask;
    private ApiHelper.FetchSinglePostTask mCurrentFetchSinglePostTask;
    private View mProgressFooterView;

    private View mEmptyView;
    private View mEmptyViewImage;
    private TextView mEmptyViewTitle;
    private MessageType mEmptyViewMessage = MessageType.NO_CONTENT;

    private EmptyViewAnimationHandler mEmptyViewAnimationHandler;
    private boolean mSwipedToRefresh;
    private boolean mKeepSwipeRefreshLayoutVisible;

    private boolean mCanLoadMorePosts = true;
    private boolean mIsPage, mShouldSelectFirstPost, mIsFetchingPosts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isAdded()) {
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null) {
                mIsPage = extras.getBoolean(PostsActivity.EXTRA_VIEW_PAGES);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_listview, container, false);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewImage = view.findViewById(R.id.empty_tags_box_top);
        mEmptyViewTitle = (TextView) view.findViewById(R.id.title_empty);
        return view;
    }

    private void initSwipeToRefreshHelper() {
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(
                getActivity(),
                (SwipeRefreshLayout) getView().findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            updateEmptyView(MessageType.NETWORK_ERROR);
                            return;
                        }
                        mSwipedToRefresh = true;
                        refreshPosts((PostsActivity) getActivity());
                    }
                });
    }

    private void refreshPosts(PostsActivity postsActivity) {
        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null) {
            ToastUtils.showToast(getActivity(), mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                    Duration.LONG);
            return;
        }
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges(currentBlog.getLocalTableBlogId(), mIsPage);
        if (hasLocalChanges) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(postsActivity);
            dialogBuilder.setTitle(getResources().getText(R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(R.string.overwrite_local_changes));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSwipeToRefreshHelper.setRefreshing(true);
                            requestPosts(false);
                        }
                    }
            );
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            mSwipeToRefreshHelper.setRefreshing(true);
            requestPosts(false);
        }
    }

    public PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            PostsListAdapter.OnLoadMoreListener loadMoreListener = new PostsListAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (mCanLoadMorePosts && !mIsFetchingPosts)
                        requestPosts(true);
                }
            };

            PostsListAdapter.OnPostsLoadedListener postsLoadedListener = new PostsListAdapter.OnPostsLoadedListener() {
                @Override
                public void onPostsLoaded(int postCount) {
                    if (!isAdded()) {
                        return;
                    }

                    // Now that posts have been loaded, show the empty view if there are no results to display
                    // This avoids the problem of the empty view immediately appearing when set at design time
                    if (postCount == 0) {
                        mEmptyView.setVisibility(View.VISIBLE);
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                    }

                    if (!isRefreshing() || mKeepSwipeRefreshLayoutVisible) {
                        // No posts and not currently refreshing. Display the "no posts/pages" message
                        updateEmptyView(MessageType.NO_CONTENT);
                    }

                    if (postCount == 0 && mCanLoadMorePosts) {
                        // No posts, let's request some if network available
                        if (isAdded() && NetworkUtils.isNetworkAvailable(getActivity())) {
                            setRefreshing(true);
                            requestPosts(false);
                        } else {
                            updateEmptyView(MessageType.NETWORK_ERROR);
                        }
                    } else if (mShouldSelectFirstPost) {
                        // Select the first row on a tablet, if requested
                        mShouldSelectFirstPost = false;
                        if (mPostsListAdapter.getCount() > 0) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(0);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                                getListView().setItemChecked(0, true);
                            }
                        }
                    } else if (isAdded() && ((PostsActivity) getActivity()).isDualPane()) {
                        // Reload the last selected position, if available
                        int selectedPosition = getListView().getCheckedItemPosition();
                        if (selectedPosition != ListView.INVALID_POSITION && selectedPosition < mPostsListAdapter.getCount()) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(selectedPosition);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                            }
                        }
                    }
                }
            };
            mPostsListAdapter = new PostsListAdapter(getActivity(), mIsPage, loadMoreListener, postsLoadedListener);
        }

        return mPostsListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mProgressFooterView = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        getListView().addFooterView(mProgressFooterView, null, false);
        mProgressFooterView.setVisibility(View.GONE);
        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(1);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                if (position >= getPostListAdapter().getCount()) //out of bounds
                    return;
                if (v == null) //view is gone
                    return;
                PostsListPost postsListPost = (PostsListPost) getPostListAdapter().getItem(position);
                if (postsListPost == null)
                    return;
                if (!mIsFetchingPosts || isLoadingMorePosts()) {
                    showPost(postsListPost.getPostId());
                } else if (isAdded()) {
                    Toast.makeText(getActivity(), mIsPage ? R.string.loading_pages : R.string.loading_posts,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        initSwipeToRefreshHelper();
        WordPress.setOnPostUploadedListener(this);

        mFabButton = (FloatingActionButton) getView().findViewById(R.id.fab_button);
        mFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPost();
            }
        });

        mEmptyViewAnimationHandler = new EmptyViewAnimationHandler();

        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            ((PostsActivity) getActivity()).requestPosts();
        } else {
            updateEmptyView(MessageType.NETWORK_ERROR);
        }
    }

    private void newPost() {
        if (getActivity() instanceof PostsActivity) {
            ((PostsActivity)getActivity()).newPost();
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnSinglePostLoadedListener = (OnSinglePostLoadedListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();
        if (WordPress.getCurrentBlog() != null) {
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getPostListAdapter());
            }

            getPostListAdapter().loadPosts();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mFabButton != null) {
            mFabButton.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }
    }

    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    private void showPost(long selectedId) {
        if (WordPress.getCurrentBlog() == null)
            return;

        Post post = WordPress.wpDB.getPostForLocalTablePostId(selectedId);
        if (post != null) {
            WordPress.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
        } else {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.post_not_found));
                ft.add(alert, "alert");
                ft.commitAllowingStateLoss();
            }
        }
    }

    boolean isLoadingMorePosts() {
        return mIsFetchingPosts && (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE);
    }

    public void requestPosts(boolean loadMore) {
        if (!isAdded() || WordPress.getCurrentBlog() == null || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(MessageType.NETWORK_ERROR);
            return;
        }

        updateEmptyView(MessageType.LOADING);

        int postCount = getPostListAdapter().getRemotePostCount() + POSTS_REQUEST_COUNT;
        if (!loadMore) {
            mCanLoadMorePosts = true;
            postCount = POSTS_REQUEST_COUNT;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(mIsPage);
        apiArgs.add(postCount);
        apiArgs.add(loadMore);
        if (mProgressFooterView != null && loadMore) {
            mProgressFooterView.setVisibility(View.VISIBLE);
        }

        mCurrentFetchPostsTask = new ApiHelper.FetchPostsTask(new ApiHelper.FetchPostsTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded())
                    return;

                if (mEmptyViewAnimationHandler.isShowingLoadingAnimation() || mEmptyViewAnimationHandler.isBetweenSequences()) {
                    // Keep the SwipeRefreshLayout animation visible until the EmptyViewAnimationHandler dismisses it
                    mKeepSwipeRefreshLayoutVisible = true;
                } else {
                    mSwipeToRefreshHelper.setRefreshing(false);
                }

                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }

                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    mCanLoadMorePosts = false;
                }

                getPostListAdapter().loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded()) {
                    return;
                }
                mSwipeToRefreshHelper.setRefreshing(false);
                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }
                if (errorType != ErrorType.TASK_CANCELLED && errorType != ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            if (mEmptyView == null || mEmptyView.getVisibility() != View.VISIBLE) {
                                ToastUtils.showToast(getActivity(),
                                        mIsPage ? R.string.error_refresh_unauthorized_pages :
                                                R.string.error_refresh_unauthorized_posts, Duration.LONG);
                            }
                            updateEmptyView(MessageType.PERMISSION_ERROR);
                            return;
                        default:
                            ToastUtils.showToast(getActivity(),
                                    mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                                    Duration.LONG);
                            updateEmptyView(MessageType.GENERIC_ERROR);
                            return;
                    }
                }
            }
        });

        mIsFetchingPosts = true;
        mCurrentFetchPostsTask.execute(apiArgs);
    }

    protected void clear() {
        if (getPostListAdapter() != null) {
            getPostListAdapter().clear();
        }
        mCanLoadMorePosts = true;
        if (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE) {
            mProgressFooterView.setVisibility(View.GONE);
        }
        mEmptyViewAnimationHandler.clear();
    }

    public void setShouldSelectFirstPost(boolean shouldSelect) {
        mShouldSelectFirstPost = shouldSelect;
    }

    @Override
    public void OnPostUploaded(int localBlogId, String postId, boolean isPage) {
        if (!isAdded()) {
            return;
        }

        // If the user switched to a different blog while uploading his post, don't reload posts and refresh the view
        boolean sameBlogId = true;
        if (WordPress.getCurrentBlog() == null || WordPress.getCurrentBlog().getLocalTableBlogId() != localBlogId) {
            sameBlogId = false;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(MessageType.NETWORK_ERROR);
            return;
        }

        // Fetch the newly uploaded post
        if (!TextUtils.isEmpty(postId)) {
            final boolean reloadPosts = sameBlogId;
            List<Object> apiArgs = new Vector<Object>();
            apiArgs.add(WordPress.wpDB.instantiateBlogByLocalId(localBlogId));
            apiArgs.add(postId);
            apiArgs.add(isPage);

            mCurrentFetchSinglePostTask = new ApiHelper.FetchSinglePostTask(
                    new ApiHelper.FetchSinglePostTask.Callback() {
                @Override
                public void onSuccess() {
                    mCurrentFetchSinglePostTask = null;
                    mIsFetchingPosts = false;
                    if (!isAdded() || !reloadPosts) {
                        return;
                    }
                    mSwipeToRefreshHelper.setRefreshing(false);
                    getPostListAdapter().loadPosts();
                    mOnSinglePostLoadedListener.onSinglePostLoaded();
                }

                @Override
                public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    mCurrentFetchSinglePostTask = null;
                    mIsFetchingPosts = false;
                    if (!isAdded() || !reloadPosts) {
                        return;
                    }
                    if (errorType != ErrorType.TASK_CANCELLED) {
                        ToastUtils.showToast(getActivity(),
                                mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts, Duration.LONG);
                    }
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });

            mSwipeToRefreshHelper.setRefreshing(true);
            mIsFetchingPosts = true;
            mCurrentFetchSinglePostTask.execute(apiArgs);
        }
    }

    @Override
    public void OnPostUploadFailed(int localBlogId) {
        mSwipeToRefreshHelper.setRefreshing(true);

        if (!isAdded()) {
            return;
        }

        // If the user switched to a different blog while uploading his post, don't reload posts and refresh the view
        if (WordPress.getCurrentBlog() == null || WordPress.getCurrentBlog().getLocalTableBlogId() != localBlogId) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(MessageType.NETWORK_ERROR);
            return;
        }

        mSwipeToRefreshHelper.setRefreshing(false);
        // Refresh the posts list to revert post status back to local draft or local changes
        getPostListAdapter().loadPosts();
    }

    public void onBlogChanged() {
        if (mCurrentFetchPostsTask != null) {
            mCurrentFetchPostsTask.cancel(true);
        }
        if (mCurrentFetchSinglePostTask != null) {
            mCurrentFetchSinglePostTask.cancel(true);
        }
        mIsFetchingPosts = false;
        mSwipeToRefreshHelper.setRefreshing(false);
    }

    private void updateEmptyView(final MessageType messageType) {
        if (mPostsListAdapter != null && mPostsListAdapter.getCount() == 0) {
            // Handle animation display
            if (mEmptyViewMessage == MessageType.NO_CONTENT && messageType == MessageType.LOADING) {
                // Show the NO_CONTENT > LOADING sequence, but only if the user swiped to refresh
                if (mSwipedToRefresh) {
                    mSwipedToRefresh = false;
                    mEmptyViewAnimationHandler.showLoadingSequence();
                    return;
                }
            } else if (mEmptyViewMessage == MessageType.LOADING && messageType == MessageType.NO_CONTENT) {
                // Show the LOADING > NO_CONTENT sequence
                mEmptyViewAnimationHandler.showNoContentSequence();
                return;
            }
        } else {
            // Dismiss the SwipeRefreshLayout animation if it was set to persist
            if (mKeepSwipeRefreshLayoutVisible) {
                mSwipeToRefreshHelper.setRefreshing(false);
                mKeepSwipeRefreshLayoutVisible = false;
            }
        }

        if (mEmptyView != null) {
            int stringId = 0;

            // Don't modify the empty view image if the NO_CONTENT > LOADING sequence has already run -
            // let the EmptyViewAnimationHandler take care of it
            if (!mEmptyViewAnimationHandler.isBetweenSequences()) {
                if (messageType == MessageType.NO_CONTENT) {
                    mEmptyViewImage.setVisibility(View.VISIBLE);
                } else {
                    mEmptyViewImage.setVisibility(View.GONE);
                }
            }

            switch (messageType) {
                case LOADING:
                    stringId = mIsPage ? R.string.loading_pages : R.string.loading_posts;
                    break;
                case NO_CONTENT:
                    stringId = mIsPage ? R.string.pages_empty_list : R.string.posts_empty_list;
                    break;
                case NETWORK_ERROR:
                    stringId = R.string.no_network_message;
                    break;
                case PERMISSION_ERROR:
                    stringId = mIsPage ? R.string.error_refresh_unauthorized_pages :
                            R.string.error_refresh_unauthorized_posts;
                    break;
                case GENERIC_ERROR:
                    stringId = mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts;
                    break;
            }

            mEmptyViewTitle.setText(getText(stringId));
            mEmptyViewMessage = messageType;
        }
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Post post);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Post post);
    }

    public interface OnSinglePostLoadedListener {
        public void onSinglePostLoaded();
    }

    public enum AnimationStage {
        PRE_ANIMATION,
        FADE_OUT_IMAGE, SLIDE_TEXT_UP, FADE_OUT_NO_CONTENT_TEXT, FADE_IN_LOADING_TEXT,
        IN_BETWEEN,
        FADE_OUT_LOADING_TEXT, FADE_IN_NO_CONTENT_TEXT, SLIDE_TEXT_DOWN, FADE_IN_IMAGE,
        FINISHED;

        public AnimationStage next() {
            return AnimationStage.values()[ordinal()+1];
        }

        public boolean isLowerThan(AnimationStage animationStage) {
            return (ordinal() < animationStage.ordinal());
        }
    }

    private class EmptyViewAnimationHandler implements ObjectAnimator.AnimatorListener {
        private static final int ANIMATION_DURATION = 150;

        private AnimationStage mAnimationStage = AnimationStage.PRE_ANIMATION;
        private boolean mHasDisplayedLoadingSequence;

        private ObjectAnimator mObjectAnimator;

        private final int mSlideDistance;

        public EmptyViewAnimationHandler() {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mEmptyViewImage.getLayoutParams();
            mSlideDistance = -((layoutParams.height + layoutParams.bottomMargin + layoutParams.topMargin) / 2);
        }

        public boolean isShowingLoadingAnimation() {
            return (mAnimationStage != AnimationStage.PRE_ANIMATION &&
                    mAnimationStage.isLowerThan(AnimationStage.IN_BETWEEN));
        }

        public boolean isBetweenSequences() {
            return (mAnimationStage == AnimationStage.IN_BETWEEN);
        }

        public void clear() {
            mAnimationStage = AnimationStage.PRE_ANIMATION;
            mHasDisplayedLoadingSequence = false;
        }

        public void startAnimation(Object target, String propertyName, float fromValue, float toValue) {
            if (mObjectAnimator != null) {
                mObjectAnimator.removeAllListeners();
            }

            mObjectAnimator = ObjectAnimator.ofFloat(target, propertyName, fromValue, toValue);
            mObjectAnimator.setDuration(ANIMATION_DURATION);
            mObjectAnimator.addListener(this);

            mObjectAnimator.start();
        }

        public void showLoadingSequence() {
            mAnimationStage = AnimationStage.FADE_OUT_IMAGE;
            mEmptyViewMessage = MessageType.LOADING;
            startAnimation(mEmptyViewImage, "alpha", 1f, 0f);
        }

        public void showNoContentSequence() {
            // If the data was auto-refreshed, the NO_CONTENT > LOADING sequence was not shown before this one, and
            // some special handling will be needed
            if (isShowingLoadingAnimation() || isBetweenSequences()) {
                mHasDisplayedLoadingSequence = true;
            } else {
                mHasDisplayedLoadingSequence = false;
            }

            if (isShowingLoadingAnimation()) {
                // Delay starting the LOADING > NO_CONTENT sequence if NO_CONTENT > LOADING hasn't finished yet
                int delayTime = ((7-mAnimationStage.ordinal()) * ANIMATION_DURATION);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAnimationStage = AnimationStage.FADE_OUT_LOADING_TEXT;
                        startAnimation(mEmptyViewTitle, "alpha", 1f, 0.1f);
                    }
                }, delayTime);
            } else {
                mAnimationStage = AnimationStage.FADE_OUT_LOADING_TEXT;
                startAnimation(mEmptyViewTitle, "alpha", 1f, 0.1f);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!isAdded()) {
                return;
            }

            mAnimationStage = mAnimationStage.next();

            // A step in the animation has completed. Set up the next animation in the chain
            switch (mAnimationStage) {
                // NO_CONTENT > LOADING section
                case SLIDE_TEXT_UP:
                    startAnimation(mEmptyViewTitle, "translationY", 0, mSlideDistance);
                    break;
                case FADE_OUT_NO_CONTENT_TEXT:
                    startAnimation(mEmptyViewTitle, "alpha", 1f, 0.1f);
                    break;
                case FADE_IN_LOADING_TEXT:
                    mEmptyViewTitle.setText(mIsPage ? R.string.loading_pages : R.string.loading_posts);

                    startAnimation(mEmptyViewTitle, "alpha", 0.1f, 1f);
                    break;

                // LOADING > NO_CONTENT section
                case FADE_IN_NO_CONTENT_TEXT:
                    mSwipeToRefreshHelper.setRefreshing(false);
                    mKeepSwipeRefreshLayoutVisible = false;

                    mEmptyViewTitle.setText(mIsPage ? R.string.pages_empty_list : R.string.posts_empty_list);
                    mEmptyViewMessage = MessageType.NO_CONTENT;

                    startAnimation(mEmptyViewTitle, "alpha", 0.1f, 1f);
                    break;
                case SLIDE_TEXT_DOWN:
                    startAnimation(mEmptyViewTitle, "translationY", mSlideDistance, 0);

                    if (!mHasDisplayedLoadingSequence) {
                        // Force mEmptyViewImage to take up space in the layout, so that mEmptyViewTitle lands
                        // where it should at the end of its slide animation
                        mEmptyViewImage.setVisibility(View.INVISIBLE);
                    }
                    break;
                case FADE_IN_IMAGE:
                    if (!mHasDisplayedLoadingSequence) {
                        // Uses AlphaAnimation instead of an ObjectAnimator to address a display glitch
                        // in the auto-refresh case
                        AniUtils.startAnimation(mEmptyViewImage, android.R.anim.fade_in, ANIMATION_DURATION);
                        mEmptyViewImage.setVisibility(View.VISIBLE);
                    } else {
                        startAnimation(mEmptyViewImage, "alpha", 0f, 1f);
                    }
                    break;
                default:
                    return;
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mAnimationStage = AnimationStage.PRE_ANIMATION;
        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
}
