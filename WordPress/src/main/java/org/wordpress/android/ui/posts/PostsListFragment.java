package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostUpdateService;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.ErrorType;

import de.greenrobot.event.EventBus;

public class PostsListFragment extends Fragment
        implements PostsListAdapter.OnPostsLoadedListener,
        PostsListAdapter.OnLoadMoreListener,
        PostsListAdapter.OnPostSelectedListener,
        PostsListAdapter.OnPostButtonClickListener {

    public static final int POSTS_REQUEST_COUNT = 20;

    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private PostsListAdapter mPostsListAdapter;
    private View mFabView;

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
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_list_fragment, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mProgressLoadMore = (ProgressBar) view.findViewById(R.id.progress);
        mFabView = view.findViewById(R.id.fab_button);

        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewTitle = (TextView) mEmptyView.findViewById(R.id.title_empty);
        mEmptyViewImage = (ImageView) mEmptyView.findViewById(R.id.image_empty);

        Context context = getActivity();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        int spacingVertical = mIsPage ? 0 : context.getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        int spacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.content_margin);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        // hide the fab so we can animate it in - note that we only do this on Lollipop and higher
        // due to a bug in the current implementation which prevents it from being hidden
        // correctly on pre-L devices (which makes animating it in/out ugly)
        // https://code.google.com/p/android/issues/detail?id=175331
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mFabView.setVisibility(View.GONE);
        }

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPost();
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
                        requestPosts(false);
                    }
                });
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

        // since setRetainInstance(true) is used, we only need to request latest
        // posts the first time this is called (ie: not after device rotation)
        if (bundle == null && NetworkUtils.checkConnection(getActivity())) {
            requestPosts(false);
        }
    }

    private void newPost() {
        if (!isAdded()) return;

        if (WordPress.getCurrentBlog() != null) {
            ActivityLauncher.addNewBlogPostOrPageForResult(getActivity(), WordPress.getCurrentBlog(), mIsPage);
        } else {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found);
        }
    }

    public void onResume() {
        super.onResume();

        if (WordPress.getCurrentBlog() != null && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostListAdapter());
        }

        if (WordPress.getCurrentBlog() != null) {
            // always (re)load when resumed to reflect changes made elsewhere
            loadPosts();
        }

        // scale in the fab after a brief delay if it's not already showing
        if (mFabView.getVisibility() != View.VISIBLE) {
            long delayMs = getResources().getInteger(R.integer.fab_animation_delay);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        AniUtils.scaleIn(mFabView, AniUtils.Duration.MEDIUM);
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
        if (!isAdded() || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        mIsFetchingPosts = true;
        if (loadMore) {
            showLoadMoreProgress();
        }
        PostUpdateService.startServiceForBlog(getActivity(), WordPress.getCurrentLocalTableBlogId(), mIsPage, loadMore);
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

    /*
     * PostMediaService has downloaded the media info for a post's featured image, tell
     * the adapter so it can show the featured image now that we have its URL
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostMediaInfoUpdated event) {
        if (isAdded() && WordPress.getCurrentBlog() != null) {
            getPostListAdapter().mediaUpdated(event.getMediaId(), event.getMediaUrl());
        }
    }

    /*
     * upload start, reload so correct status on uploading post appears
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadStarted event) {
        if (isAdded() && WordPress.getCurrentLocalTableBlogId() == event.mLocalBlogId) {
            loadPosts();
        }
    }

    /*
     * upload ended, reload regardless of success/fail so correct status of uploaded post appears
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadEnded event) {
        if (isAdded() && WordPress.getCurrentLocalTableBlogId() == event.mLocalBlogId) {
            loadPosts();
        }
    }

    /*
     * PostUpdateService finished a request to retrieve new posts
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.RequestPosts event) {
        mIsFetchingPosts = false;
        if (isAdded() && event.getBlogId() == WordPress.getCurrentLocalTableBlogId()) {
            setRefreshing(false);
            hideLoadMoreProgress();
            if (!event.getFailed()) {
                mCanLoadMorePosts = event.canLoadMore();
                loadPosts();
            } else {
                ApiHelper.ErrorType errorType = event.getErrorType();
                if (errorType != null && errorType != ErrorType.TASK_CANCELLED && errorType != ErrorType.NO_ERROR) {
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
        }
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

    private void hideEmptyView() {
        if (isAdded() && mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
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
            hideEmptyView();
        }
    }

    /*
     * called by the adapter to load more posts when the user scrolls towards the last post
     */
    @Override
    public void onLoadMore() {
        if (mCanLoadMorePosts && !mIsFetchingPosts) {
            requestPosts(true);
        }
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
                publishPost(fullPost);
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
                // prevent deleting post while it's being uploaded
                if (!post.isUploading()) {
                    trashPost(post);
                }
                break;
        }
    }

    private void publishPost(final Post post) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            ToastUtils.showToast(getActivity(), R.string.error_publish_no_network, ToastUtils.Duration.SHORT);
            return;
        }

        // If the post is empty, don't publish
        if (!post.isPublishable()) {
            ToastUtils.showToast(getActivity(), R.string.error_publish_empty_post, ToastUtils.Duration.SHORT);
            return;
        }

        post.setPostStatus(PostStatus.toString(PostStatus.PUBLISHED));
        post.setChangedFromDraftToPublished(true);

        PostUploadService.addPostToUpload(post);
        getActivity().startService(new Intent(getActivity(), PostUploadService.class));

        PostUtils.trackSavePostAnalytics(post);
    }

    /*
     * send the passed post to the trash with undo
     */
    private void trashPost(final PostsListPost post) {
        //only check if network is available in case this is not a local draft - local drafts have not yet
        //been posted to the server so they can be trashed w/o further care
        if (!isAdded() || (!post.isLocalDraft() && !NetworkUtils.checkConnection(getActivity()))) {
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

        // make sure empty view shows if user deleted the only post
        if (getPostListAdapter().getItemCount() == 0) {
            updateEmptyView(EmptyViewMessageType.NO_CONTENT);
        }

        View.OnClickListener undoListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // user undid the trash, so unhide the post and remove it from the list of trashed posts
                mTrashedPosts.remove(post);
                getPostListAdapter().unhidePost(post);
                hideEmptyView();
            }
        };

        // different undo text if this is a local draft since it will be deleted rather than trashed
        String text;
        if (post.isLocalDraft()) {
            text = mIsPage ? getString(R.string.page_deleted) : getString(R.string.post_deleted);
        } else {
            text = mIsPage ? getString(R.string.page_trashed) : getString(R.string.post_trashed);
        }

        Snackbar snackbar = Snackbar.make(getView().findViewById(R.id.coordinator), text, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, undoListener);

        // wait for the undo snackbar to disappear before actually deleting the post
        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);

                // if the post no longer exists in the list of trashed posts it's because the
                // user undid the trash, so don't perform the deletion
                if (!mTrashedPosts.contains(post)) {
                    return;
                }

                // remove from the list of trashed posts in case onDismissed is called multiple
                // times - this way the above check prevents us making the call to delete it twice
                // https://code.google.com/p/android/issues/detail?id=190529
                mTrashedPosts.remove(post);

                WordPress.wpDB.deletePost(fullPost);

                if (!post.isLocalDraft()) {
                    new ApiHelper.DeleteSinglePostTask().execute(WordPress.getCurrentBlog(),
                            fullPost.getRemotePostId(), mIsPage);
                }
            }
        });

        snackbar.show();
    }
}
