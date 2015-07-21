package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.EmptyViewAnimationHandler;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.posts.PostUploadEvents.PostUploadFailed;
import org.wordpress.android.ui.posts.PostUploadEvents.PostUploadSucceed;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.reader.views.ReaderRecyclerView;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import java.util.List;
import java.util.Vector;

import de.greenrobot.event.EventBus;

public class PostsListFragment extends Fragment implements EmptyViewAnimationHandler.OnAnimationProgressListener {
    public static final int POSTS_REQUEST_COUNT = 20;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnSinglePostLoadedListener mOnSinglePostLoadedListener;
    private PostsListAdapter mPostsListAdapter;
    private View mFabView;
    private ApiHelper.FetchPostsTask mCurrentFetchPostsTask;
    private ApiHelper.FetchSinglePostTask mCurrentFetchSinglePostTask;

    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private View mEmptyViewImage;
    private ProgressBar mProgressLoadMore;
    private TextView mEmptyViewTitle;
    private EmptyViewMessageType mEmptyViewMessage = EmptyViewMessageType.NO_CONTENT;

    private EmptyViewAnimationHandler mEmptyViewAnimationHandler;
    private boolean mSwipedToRefresh;
    private boolean mKeepSwipeRefreshLayoutVisible;
    private boolean mShowSelection;

    private boolean mCanLoadMorePosts = true;
    private boolean mIsPage, mShouldSelectFirstPost, mIsFetchingPosts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isAdded()) {
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null) {
                mIsPage = extras.getBoolean(PostsListActivity.EXTRA_VIEW_PAGES);
            }
            // If PostUploadService is not running, check for posts stuck with an uploading state
            Blog currentBlog = WordPress.getCurrentBlog();
            if (!ServiceUtils.isServiceRunning(getActivity(), PostUploadService.class) && currentBlog != null) {
                WordPress.wpDB.clearAllUploadingPosts(currentBlog.getLocalTableBlogId(), mIsPage);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_list, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        int spacingVertical = DisplayUtils.dpToPx(getActivity(), 1);
        mRecyclerView.addItemDecoration(new ReaderRecyclerView.ReaderItemDecoration(0, spacingVertical));

        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewImage = view.findViewById(R.id.empty_tags_box_top);
        mEmptyViewTitle = (TextView) view.findViewById(R.id.title_empty);
        mProgressLoadMore = (ProgressBar) view.findViewById(R.id.progress);

        return view;
    }

    private void initSwipeToRefreshHelper() {
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(
                getActivity(),
                (CustomSwipeRefreshLayout) getView().findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                            return;
                        }
                        mSwipedToRefresh = true;
                        refreshPosts((PostsListActivity) getActivity());
                    }
                });
    }

    private void refreshPosts(PostsListActivity postsListActivity) {
        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null) {
            ToastUtils.showToast(getActivity(), mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                    Duration.LONG);
            return;
        }
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges(currentBlog.getLocalTableBlogId(), mIsPage);
        if (hasLocalChanges) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(postsListActivity);
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
                        updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                    }

                    if (postCount == 0 && mCanLoadMorePosts) {
                        // No posts, let's request some if network available
                        if (isAdded() && NetworkUtils.isNetworkAvailable(getActivity())) {
                            setRefreshing(true);
                            requestPosts(false);
                        } else {
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                        }
                    } else if (mShouldSelectFirstPost) {
                        // Select the first row on a tablet, if requested
                        mShouldSelectFirstPost = false;
                        if (mPostsListAdapter.getItemCount() > 0) {
                            PostsListPost postsListPost = mPostsListAdapter.getItem(0);
                            if (postsListPost != null) {
                                mPostsListAdapter.setSelectedPosition(0);
                                showPost(postsListPost.getPostId());
                            }
                        }
                    }
                }
            };

            PostsListAdapter.OnPostSelectedListener postSelectedListener = new PostsListAdapter.OnPostSelectedListener() {
                @Override
                public void onPostSelected(PostsListPost post) {
                    if (isAdded()) {
                        showPost(post.getPostId());
                    }
                }
            };

            mPostsListAdapter = new PostsListAdapter(getActivity(), mIsPage);
            mPostsListAdapter.setOnLoadMoreListener(loadMoreListener);
            mPostsListAdapter.setOnPostsLoadedListener(postsLoadedListener);
            mPostsListAdapter.setOnPostSelectedListener(postSelectedListener);
            mPostsListAdapter.setShowSelection(mShowSelection);
        }

        return mPostsListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        initSwipeToRefreshHelper();

        mFabView = getView().findViewById(R.id.fab_button);
        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPost();
            }
        });

        mEmptyViewAnimationHandler = new EmptyViewAnimationHandler(mEmptyViewTitle, mEmptyViewImage, this);

        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            // If we remove or throttle the following call, we should make PostUpload events sticky
            ((PostsListActivity) getActivity()).requestPosts();
        } else {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
        }
    }

    private void newPost() {
        if (getActivity() instanceof PostsListActivity) {
            ((PostsListActivity)getActivity()).newPost();
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
            if (mRecyclerView.getAdapter() == null) {
                mRecyclerView.setAdapter(getPostListAdapter());
            }
            getPostListAdapter().loadPosts();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (mFabView != null) {
            mFabView.setVisibility(hidden ? View.GONE : View.VISIBLE);
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

    public void requestPosts(boolean loadMore) {
        if (!isAdded() || WordPress.getCurrentBlog() == null || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        updateEmptyView(EmptyViewMessageType.LOADING);

        int postCount = getPostListAdapter().getRemotePostCount() + POSTS_REQUEST_COUNT;
        if (!loadMore) {
            mCanLoadMorePosts = true;
            postCount = POSTS_REQUEST_COUNT;
        }
        List<Object> apiArgs = new Vector<>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(mIsPage);
        apiArgs.add(postCount);
        apiArgs.add(loadMore);

        // show progress bar at the bottom if we're loading more posts
        if (loadMore) {
            showLoadMoreProgress();
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

                hideLoadMoreProgress();

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
                hideLoadMoreProgress();

                if (errorType != ErrorType.TASK_CANCELLED && errorType != ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            if (mEmptyView == null || mEmptyView.getVisibility() != View.VISIBLE) {
                                ToastUtils.showToast(getActivity(),
                                        mIsPage ? R.string.error_refresh_unauthorized_pages :
                                                R.string.error_refresh_unauthorized_posts, Duration.LONG);
                            }
                            updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                            break;
                        default:
                            ToastUtils.showToast(getActivity(),
                                    mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                                    Duration.LONG);
                            updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                            break;
                    }
                }
            }
        });

        mIsFetchingPosts = true;
        mCurrentFetchPostsTask.execute(apiArgs);
    }

    private void showLoadMoreProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadMoreProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.GONE);
        }
    }

    public void setShouldSelectFirstPost(boolean shouldSelect) {
        mShouldSelectFirstPost = shouldSelect;
    }

    /*
     * determines whether the adapter highlights the selected post - used in dual-pane mode
     */
    public void setShowSelection(boolean showSelection) {
        mShowSelection = showSelection;
        if (mPostsListAdapter != null) {
            mPostsListAdapter.setShowSelection(showSelection);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostUploadSucceed event) {
        if (!isAdded()) {
            return;
        }

        // If the user switched to a different blog while uploading his post, don't reload posts and refresh the view
        boolean sameBlogId = true;
        if (WordPress.getCurrentBlog() == null || WordPress.getCurrentBlog().getLocalTableBlogId() != event.mLocalBlogId) {
            sameBlogId = false;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        // Fetch the newly uploaded post
        if (!TextUtils.isEmpty(event.mRemotePostId)) {
            final boolean reloadPosts = sameBlogId;
            List<Object> apiArgs = new Vector<>();
            apiArgs.add(WordPress.wpDB.instantiateBlogByLocalId(event.mLocalBlogId));
            apiArgs.add(event.mRemotePostId);
            apiArgs.add(event.mIsPage);

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

    @SuppressWarnings("unused")
    public void onEventMainThread(PostUploadFailed event) {
        mSwipeToRefreshHelper.setRefreshing(true);

        if (!isAdded()) {
            return;
        }

        // If the user switched to a different blog while uploading his post, don't reload posts and refresh the view
        if (WordPress.getCurrentBlog() == null || WordPress.getCurrentBlog().getLocalTableBlogId() != event.mLocalId) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        mSwipeToRefreshHelper.setRefreshing(false);
        // Refresh the posts list to revert post status back to local draft or local changes
        getPostListAdapter().loadPosts();
    }

    private void updateEmptyView(final EmptyViewMessageType emptyViewMessageType) {
        if (mPostsListAdapter != null && mPostsListAdapter.getItemCount() == 0) {
            // Handle animation display
            if (mEmptyViewMessage == EmptyViewMessageType.NO_CONTENT &&
                    emptyViewMessageType == EmptyViewMessageType.LOADING) {
                // Show the NO_CONTENT > LOADING sequence, but only if the user swiped to refresh
                if (mSwipedToRefresh) {
                    mSwipedToRefresh = false;
                    mEmptyViewAnimationHandler.showLoadingSequence();
                    return;
                }
            } else if (mEmptyViewMessage == EmptyViewMessageType.LOADING &&
                    emptyViewMessageType == EmptyViewMessageType.NO_CONTENT) {
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
                if (emptyViewMessageType == EmptyViewMessageType.NO_CONTENT) {
                    mEmptyViewImage.setVisibility(View.VISIBLE);
                } else {
                    mEmptyViewImage.setVisibility(View.GONE);
                }
            }

            switch (emptyViewMessageType) {
                case LOADING:
                    stringId = mIsPage ? R.string.pages_fetching : R.string.posts_fetching;
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
            mEmptyViewMessage = emptyViewMessageType;
        }
    }

    @Override
    public void onSequenceStarted(EmptyViewMessageType emptyViewMessageType) {
        mEmptyViewMessage = emptyViewMessageType;
    }

    @Override
    public void onNewTextFadingIn() {
        switch (mEmptyViewMessage) {
            case LOADING:
                mEmptyViewTitle.setText(mIsPage ? org.wordpress.android.R.string.pages_fetching :
                        org.wordpress.android.R.string.posts_fetching);
                break;
            case NO_CONTENT:
                mEmptyViewTitle.setText(mIsPage ? org.wordpress.android.R.string.pages_empty_list :
                        org.wordpress.android.R.string.posts_empty_list);
                mSwipeToRefreshHelper.setRefreshing(false);
                mKeepSwipeRefreshLayoutVisible = false;
                break;
            default:
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public interface OnPostSelectedListener {
        void onPostSelected(Post post);
    }

    public interface OnPostActionListener {
        void onPostAction(int action, Post post);
    }

    public interface OnSinglePostLoadedListener {
        void onSinglePostLoaded();
    }
}
