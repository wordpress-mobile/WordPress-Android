package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostEvents.PostUploadFailed;
import org.wordpress.android.ui.posts.services.PostEvents.PostUploadSucceed;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import java.util.List;
import java.util.Vector;

import de.greenrobot.event.EventBus;

public class PostsListFragment extends Fragment
        implements PostsListAdapter.OnPostsLoadedListener,
                   PostsListAdapter.OnLoadMoreListener,
                   PostsListAdapter.OnPostSelectedListener,
                   PostsListAdapter.OnPostButtonClickListener {

    public static final int POSTS_REQUEST_COUNT = 20;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private PostsListAdapter mPostsListAdapter;
    private FloatingActionButton mFabView;
    private ApiHelper.FetchPostsTask mCurrentFetchPostsTask;
    private ApiHelper.FetchSinglePostTask mCurrentFetchSinglePostTask;

    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private ProgressBar mProgressLoadMore;
    private TextView mEmptyViewTitle;
    private ImageView mEmptyViewImage;

    private boolean mCanLoadMorePosts = true;
    private boolean mIsPage;
    private boolean mIsFetchingPosts;

    private final PostsListPostList mTrashedPosts = new PostsListPostList();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

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
        View view = inflater.inflate(R.layout.post_list_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mProgressLoadMore = (ProgressBar) view.findViewById(R.id.progress);
        mFabView = (FloatingActionButton) view.findViewById(R.id.fab_button);

        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewTitle = (TextView) mEmptyView.findViewById(R.id.title_empty);
        mEmptyViewImage = (ImageView) mEmptyView.findViewById(R.id.image_empty);

        Context context = getActivity();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        int spacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getCurrentBlog(), mIsPage, v);
            }
        });

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
                            setRefreshing(false);
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                            return;
                        }
                        refreshPosts();
                    }
                });
    }

    private void refreshPosts() {
        if (!isAdded()) return;

        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null) {
            ToastUtils.showToast(getActivity(), mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                    Duration.LONG);
            return;
        }
        boolean hasLocalChanges = WordPress.wpDB.findLocalChanges(currentBlog.getLocalTableBlogId(), mIsPage);
        if (hasLocalChanges) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(getResources().getText(R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(R.string.overwrite_local_changes));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            setRefreshing(true);
                            requestPosts(false);
                        }
                    }
            );
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    setRefreshing(false);
                }
            });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            setRefreshing(true);
            requestPosts(false);
        }
    }

    public PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            mPostsListAdapter = new PostsListAdapter(getActivity(), WordPress.getCurrentBlog(), mIsPage);
            mPostsListAdapter.setOnLoadMoreListener(this);
            mPostsListAdapter.setOnPostsLoadedListener(this);
            mPostsListAdapter.setOnPostSelectedListener(this);
            mPostsListAdapter.setOnPostButtonClickListener(this);
        }

        return mPostsListAdapter;
    }

    private boolean isPostAdapterEmpty() {
        return (mPostsListAdapter != null && mPostsListAdapter.getItemCount() == 0);
    }

    private void loadPosts() {
        getPostListAdapter().loadPosts();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        initSwipeToRefreshHelper();

        if (WordPress.getCurrentBlog() != null && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostListAdapter());
        }

        // since setRetainInstance(true) is used, we only need to load adapter and request latest
        // posts the first time this is called (ie: not after device rotation)
        if (bundle == null) {
            loadPosts();
            if (NetworkUtils.checkConnection(getActivity())) {
                requestPosts(false);
            }
        }
    }

    public void onResume() {
        super.onResume();

        // scale in the fab after a brief delay if it's not already showing
        if (mFabView.getVisibility() != View.VISIBLE) {
            long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        AniUtils.showFab(mFabView, true);
                    }
                }
            }, delayMs);
        }
    }

    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    private void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    private void requestPosts(boolean loadMore) {
        if (!isAdded() || WordPress.getCurrentBlog() == null || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            setRefreshing(false);
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        // if user has local changes, don't refresh
        if (WordPress.wpDB.findLocalChanges(WordPress.getCurrentBlog().getLocalTableBlogId(), mIsPage)) {
            return;
        }

        int postCount;
        if (loadMore) {
            postCount = getPostListAdapter().getRemotePostCount() + POSTS_REQUEST_COUNT;
            showLoadMoreProgress();
        } else {
            mCanLoadMorePosts = true;
            postCount = POSTS_REQUEST_COUNT;
            setRefreshing(true);
            updateEmptyView(EmptyViewMessageType.LOADING);
        }

        List<Object> apiArgs = new Vector<>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(mIsPage);
        apiArgs.add(postCount);
        apiArgs.add(loadMore);

        mCurrentFetchPostsTask = new ApiHelper.FetchPostsTask(new ApiHelper.FetchPostsTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded()) {
                    return;
                }

                setRefreshing(false);
                hideLoadMoreProgress();

                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    mCanLoadMorePosts = false;
                }

                loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded()) {
                    return;
                }

                setRefreshing(false);
                hideLoadMoreProgress();

                if (errorType != ErrorType.TASK_CANCELLED && errorType != ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                            break;
                        default:
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

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaInfoUpdated event) {
        // PostMediaService has downloaded the media info for a post's featured image, tell
        // the adapter so it can show the featured image now that we have its URL
        if (isAdded()) {
            getPostListAdapter().mediaUpdated(event.getMediaId(), event.getMediaUrl());
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

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            setRefreshing(false);
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
                            setRefreshing(false);
                            loadPosts();
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
                            setRefreshing(false);
                        }
                    });

            setRefreshing(true);
            mIsFetchingPosts = true;
            mCurrentFetchSinglePostTask.execute(apiArgs);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostUploadFailed event) {
        if (!isAdded()) {
            return;
        }

        // If the user switched to a different blog while uploading his post, don't reload posts and refresh the view
        if (WordPress.getCurrentBlog() == null || WordPress.getCurrentBlog().getLocalTableBlogId() != event.mLocalId) {
            return;
        }

        setRefreshing(false);

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        // Refresh the posts list to revert post status back to local draft or local changes
        loadPosts();
    }

    private void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        int stringId;
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
            default:
                return;
        }

        mEmptyViewTitle.setText(getText(stringId));
        mEmptyViewImage.setVisibility(emptyViewMessageType == EmptyViewMessageType.NO_CONTENT ? View.VISIBLE : View.GONE);
        mEmptyView.setVisibility(isPostAdapterEmpty() ? View.VISIBLE : View.GONE);
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

    /*
     * called by the adapter after posts have been loaded
     */
    @Override
    public void onPostsLoaded(int postCount) {
        if (!isAdded()) {
            return;
        }

        if (postCount == 0 && !mIsFetchingPosts) {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
            } else {
                updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            }
        } else if (postCount > 0) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    /*
     * called by the adapter to load more posts when the user scrolls towards the last post
     */
    @Override
    public void onLoadMore() {
        if (mCanLoadMorePosts && !mIsFetchingPosts)
            requestPosts(true);
    }

    /*
     * called by the adapter when the user clicks a post
     */
    @Override
    public void onPostSelected(PostsListPost post) {
        onPostButtonClicked(PostListButton.BUTTON_PREVIEW, post);
    }

    /*
     * called by the adapter when the user clicks the edit/view/stats/trash button for a post
     */
    @Override
    public void onPostButtonClicked(int buttonType, PostsListPost post) {
        if (!isAdded()) return;

        Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(post.getPostId());
        if (fullPost == null) {
            ToastUtils.showToast(getActivity(), R.string.post_not_found);
            return;
        }

        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                ActivityLauncher.editBlogPostOrPageForResult(getActivity(), post.getPostId(), mIsPage);
                break;
            case PostListButton.BUTTON_PUBLISH:
                PostUploadService.addPostToUpload(fullPost);
                getActivity().startService(new Intent(getActivity(), PostUploadService.class));
                break;
            case PostListButton.BUTTON_VIEW:
                ActivityLauncher.browsePostOrPage(getActivity(), WordPress.getCurrentBlog(), fullPost);
                break;
            case PostListButton.BUTTON_PREVIEW:
                ActivityLauncher.viewPostPreviewForResult(getActivity(), fullPost, mIsPage);
                break;
            case PostListButton.BUTTON_STATS:
                ActivityLauncher.viewStatsSinglePostDetails(getActivity(), fullPost, mIsPage);
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                trashPost(post);
                break;
        }
    }

    /*
     * send the passed post to the trash with undo
     */
    private void trashPost(final PostsListPost post) {
        if (!isAdded() || !NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        final Post fullPost = WordPress.wpDB.getPostForLocalTablePostId(post.getPostId());
        if (fullPost == null) {
            ToastUtils.showToast(getActivity(), R.string.post_not_found);
            return;
        }

        // remove post from the list and add it to the list of trashed posts
        getPostListAdapter().hidePost(post);
        mTrashedPosts.add(post);

        View.OnClickListener undoListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // user undid the trash, so unhide the post and remove it from the list of trashed posts
                mTrashedPosts.remove(post);
                getPostListAdapter().unhidePost(post);
            }
        };

        // different undo text if this is a local draft since it will be deleted rather than trashed
        String text;
        if (post.isLocalDraft()) {
            text = mIsPage ? getString(R.string.page_deleted) : getString(R.string.post_deleted);
        } else {
            text = mIsPage ? getString(R.string.page_trashed) : getString(R.string.post_trashed);
        }

        Snackbar.make(getView().findViewById(R.id.coordinator), text, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, undoListener)
                .show();

        // wait for the undo snackbar to disappear before actually deleting the post
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // if the post no longer exists in the list of trashed posts it's because the
                // user undid the trash, so don't perform the deletion
                if (!mTrashedPosts.contains(post)) {
                    AppLog.d(AppLog.T.POSTS, "user undid trashing");
                    return;
                }

                WordPress.wpDB.deletePost(fullPost);

                if (!post.isLocalDraft()) {
                    List<Object> apiArgs = new Vector<>();
                    apiArgs.add(WordPress.getCurrentBlog());
                    apiArgs.add(fullPost.getRemotePostId());
                    apiArgs.add(mIsPage);
                    new ApiHelper.DeleteSinglePostTask().execute(apiArgs);
                }
            }
        }, Constants.SNACKBAR_LONG_DURATION_MS);
    }
}
