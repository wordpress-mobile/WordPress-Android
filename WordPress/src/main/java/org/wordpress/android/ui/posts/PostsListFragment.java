package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.push.NativeNotificationsUtils;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter.LoadMode;
import org.wordpress.android.ui.uploads.PostEvents;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.uploads.UploadUtils;
import org.wordpress.android.ui.uploads.VideoOptimizer;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.RecyclerViewScrollPositionManager;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.PostListButton;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

public class PostsListFragment extends Fragment
        implements PostsListAdapter.OnPostsLoadedListener,
        PostsListAdapter.OnLoadMoreListener,
        PostsListAdapter.OnPostSelectedListener,
        PostsListAdapter.OnPostButtonClickListener {

    public static final int POSTS_REQUEST_COUNT = 20;
    public static final String TAG = "posts_list_fragment_tag";

    private final RecyclerViewScrollPositionManager mRVScrollPositionSaver = new RecyclerViewScrollPositionManager();
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
    private PostModel mTargetPost;
    private boolean mIsFetchingPosts;
    private boolean mShouldCancelPendingDraftNotification = false;
    private int mPostIdForPostToBeDeleted = 0;

    private final List<PostModel> mTrashedPosts = new ArrayList<>();

    private SiteModel mSite;

    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject Dispatcher mDispatcher;

    public static PostsListFragment newInstance(SiteModel site, boolean isPage, @Nullable PostModel targetPost) {
        PostsListFragment fragment = new PostsListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        bundle.putBoolean(PostsListActivity.EXTRA_VIEW_PAGES, isPage);
        if (targetPost != null) {
            bundle.putInt(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID, targetPost.getId());
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mRVScrollPositionSaver.onRestoreInstanceState(savedInstanceState);
        }

        updateSiteOrFinishActivity(savedInstanceState);

        EventBus.getDefault().register(this);
        mDispatcher.register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        mDispatcher.unregister(this);

        super.onDestroy();
    }

    private void updateSiteOrFinishActivity(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
                mIsPage = getArguments().getBoolean(PostsListActivity.EXTRA_VIEW_PAGES);
                mTargetPost = mPostStore.getPostByLocalPostId(
                        getArguments().getInt(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID));
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
                mIsPage = getActivity().getIntent().getBooleanExtra(PostsListActivity.EXTRA_VIEW_PAGES, false);
                mTargetPost = mPostStore.getPostByLocalPostId(
                        getActivity().getIntent().getIntExtra(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID, 0));
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mIsPage = savedInstanceState.getBoolean(PostsListActivity.EXTRA_VIEW_PAGES);
            mTargetPost = mPostStore.getPostByLocalPostId(
                    savedInstanceState.getInt(PostsListActivity.EXTRA_TARGET_POST_LOCAL_ID));
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
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

        int spacingVertical = mIsPage ? 0 : context.getResources().getDimensionPixelSize(R.dimen.card_gutters);
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

        if (savedInstanceState == null) {
            if (UploadService.hasPendingOrInProgressPostUploads()) {
                // if there are some in-progress uploads, we'd better just load the DB Posts and reflect upload
                // changes there. Otherwise, a duplicate-post situation can happen when:
                // a FETCH_POSTS completing *after* the post has been uploaded to the server but *before*
                // the PUSH_POST action completes and a PUSHED_POST is emitted.
                loadPosts(LoadMode.IF_CHANGED);
            } else {
                // refresh normally
                requestPosts(false);
            }
        }

        initSwipeToRefreshHelper(view);

        return view;
    }

    public void handleEditPostResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || !isAdded()) {
            return;
        }

        final PostModel post = mPostStore.
                getPostByLocalPostId(data.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0));

        if (post == null) {
            ToastUtils.showToast(getActivity(), R.string.post_not_found, ToastUtils.Duration.LONG);
            return;
        }

        UploadUtils.handleEditPostResultSnackbars(getActivity(),
                getActivity().findViewById(R.id.coordinator), resultCode, data, post, mSite,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UploadUtils.publishPost(getActivity(), post, mSite, mDispatcher);
                    }
                });
    }

    private void initSwipeToRefreshHelper(View view) {
        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) view.findViewById(R.id.ptr_layout),
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
                }
        );
    }

    private @Nullable PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            mPostsListAdapter = new PostsListAdapter(getActivity(), mSite, mIsPage);
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

    private void loadPosts(LoadMode mode) {
        if (getPostListAdapter() != null) {
            getPostListAdapter().loadPosts(mode);
        }
    }

    private void newPost() {
        if (!isAdded()) return;
        ActivityLauncher.addNewPostOrPageForResult(getActivity(), mSite, mIsPage, false);
    }

    public void onResume() {
        super.onResume();

        if (getPostListAdapter() != null && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostListAdapter());
        }

        // always (re)load when resumed to reflect changes made elsewhere
        loadPosts(LoadMode.IF_CHANGED);

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

        if (getPostListAdapter() != null && getPostListAdapter().getItemCount() == 0) {
            updateEmptyView(EmptyViewMessageType.LOADING);
        }

        mIsFetchingPosts = true;
        if (loadMore) {
            showLoadMoreProgress();
        }

        FetchPostsPayload payload = new FetchPostsPayload(mSite, loadMore);

        if (mIsPage) {
            mDispatcher.dispatch(PostActionBuilder.newFetchPagesAction(payload));
        } else {
            mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(payload));
        }
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
     * Upload started, reload so correct status on uploading post appears
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadStarted event) {
        if (isAdded() && mSite != null && mSite.getId() == event.mLocalBlogId) {
            loadPosts(LoadMode.FORCED);
        }
    }

    /*
    * Upload cancelled (probably due to failed media), reload so correct status on uploading post appears
    */
    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadCanceled event) {
        if (isAdded() && mSite != null && mSite.getId() == event.localSiteId) {
            loadPosts(LoadMode.FORCED);
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
        mEmptyViewImage.setVisibility(emptyViewMessageType == EmptyViewMessageType.NO_CONTENT ? View.VISIBLE :
                View.GONE);
        mEmptyView.setVisibility(isPostAdapterEmpty() ? View.VISIBLE : View.GONE);
    }

    private void hideEmptyView() {
        if (isAdded() && mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        if (mShouldCancelPendingDraftNotification) {
            // delete the pending draft notification if available
            int pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(mPostIdForPostToBeDeleted);
            NativeNotificationsUtils.dismissNotification(pushId, getActivity());
            mShouldCancelPendingDraftNotification = false;
        }
        super.onDetach();
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
            mRVScrollPositionSaver.restoreScrollOffset(mRecyclerView);
        }

        // If the activity was given a target post, and this is the first time posts are loaded, scroll to that post
        if (mTargetPost != null) {
            if (mPostsListAdapter != null) {
                final int position = mPostsListAdapter.getPositionForPost(mTargetPost);
                if (position > -1) {
                    RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
                        private static final int SCROLL_OFFSET_DP = 23;

                        @Override
                        protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }

                        @Override
                        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd,
                                                    int snapPreference) {
                            // Assume SNAP_TO_START, and offset the scroll, so the bottom of the above post shows
                            int offsetPx = (int) TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP, SCROLL_OFFSET_DP, getResources().getDisplayMetrics());
                            return boxStart - viewStart + offsetPx;
                        }
                    };

                    smoothScroller.setTargetPosition(position);
                    mRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                }
            }
            mTargetPost = null;
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
    public void onPostSelected(PostModel post) {
        onPostButtonClicked(PostListButton.BUTTON_EDIT, post);
    }

    /*
     * called by the adapter when the user clicks the edit/view/stats/trash button for a post
     */
    @Override
    public void onPostButtonClicked(int buttonType, PostModel postClicked) {
        if (!isAdded()) return;

        // Get the latest version of the post, in case it's changed since the last time we refreshed the post list
        final PostModel post = mPostStore.getPostByLocalPostId(postClicked.getId());
        if (post == null) {
            loadPosts(LoadMode.FORCED);
            return;
        }

        switch (buttonType) {
            case PostListButton.BUTTON_EDIT:
                if (UploadService.isPostUploadingOrQueued(post)) {
                    // If the post is uploading media, allow the media to continue uploading, but don't upload the
                    // post itself when they finish (since we're about to edit it again)
                    UploadService.cancelQueuedPostUpload(post);
                }
                ActivityLauncher.editPostOrPageForResult(getActivity(), mSite, post);
                break;
            case PostListButton.BUTTON_RETRY:
                // restart the UploadService with retry parameters
                Intent intent = UploadService.getUploadPostServiceIntent(
                        getActivity(), post, PostUtils.isFirstTimePublish(post), false, true);
                getActivity().startService(intent);
                break;
            case PostListButton.BUTTON_SUBMIT:
            case PostListButton.BUTTON_SYNC:
            case PostListButton.BUTTON_PUBLISH:
                UploadUtils.publishPost(getActivity(), post, mSite, mDispatcher);
                break;
            case PostListButton.BUTTON_VIEW:
                ActivityLauncher.browsePostOrPage(getActivity(), mSite, post);
                break;
            case PostListButton.BUTTON_PREVIEW:
                ActivityLauncher.viewPostPreviewForResult(getActivity(), mSite, post);
                break;
            case PostListButton.BUTTON_STATS:
                ActivityLauncher.viewStatsSinglePostDetails(getActivity(), mSite, post, mIsPage);
                break;
            case PostListButton.BUTTON_TRASH:
            case PostListButton.BUTTON_DELETE:
                if (!UploadService.isPostUploadingOrQueued(post)) {
                    trashPost(post);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getText(R.string.delete_post))
                            .setMessage(R.string.dialog_confirm_cancel_post_media_uploading)
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    trashPost(post);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setCancelable(true);
                    builder.create().show();
                }
                break;
        }
    }

    /*
     * send the passed post to the trash with undo
     */
    private void trashPost(final PostModel post) {
        //only check if network is available in case this is not a local draft - local drafts have not yet
        //been posted to the server so they can be trashed w/o further care
        if (!isAdded() || (!post.isLocalDraft() && !NetworkUtils.checkConnection(getActivity()))
            || getPostListAdapter() == null) {
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

                // here cancel all media uploads related to this Post
                UploadService.cancelQueuedPostUploadAndRelatedMedia(WordPress.getContext(), post);

                if (post.isLocalDraft()) {
                    mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(post));

                    // delete the pending draft notification if available
                    mShouldCancelPendingDraftNotification = false;
                    int pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(post.getId());
                    NativeNotificationsUtils.dismissNotification(pushId, WordPress.getContext());
                } else {
                    mDispatcher.dispatch(PostActionBuilder.newDeletePostAction(new RemotePostPayload(post, mSite)));
                }
            }
        });

        mPostIdForPostToBeDeleted = post.getId();
        mShouldCancelPendingDraftNotification = true;
        snackbar.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(PostsListActivity.EXTRA_VIEW_PAGES, mIsPage);
        mRVScrollPositionSaver.onSaveInstanceState(outState, mRecyclerView);
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        switch (event.causeOfChange) {
            // if a Post is updated, let's refresh the whole list, because we can't really know
            // from FluxC which post has changed, or when. So to make sure, we go to the source,
            // which is the FluxC PostStore.
            case UPDATE_POST:
                if (!event.isError()) {
                    loadPosts(LoadMode.IF_CHANGED);
                }
                break;
            case FETCH_POSTS:
            case FETCH_PAGES:
                mIsFetchingPosts = false;
                if (!isAdded()) {
                    return;
                }

                setRefreshing(false);
                hideLoadMoreProgress();
                if (!event.isError()) {
                    mCanLoadMorePosts = event.canLoadMore;
                    loadPosts(LoadMode.IF_CHANGED);
                } else {
                    PostError error = event.error;
                    switch (error.type) {
                        case UNAUTHORIZED:
                            updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                            break;
                        default:
                            updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                            break;
                    }
                }
                break;
            case DELETE_POST:
                if (event.isError()) {
                    String message = getString(mIsPage ? R.string.error_deleting_page : R.string.error_deleting_post);
                    ToastUtils.showToast(getActivity(), message, ToastUtils.Duration.SHORT);
                    loadPosts(LoadMode.IF_CHANGED);
                }
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        final PostModel post = event.post;
        if (isAdded() && event.post != null && event.post.getLocalSiteId() == mSite.getId()) {
            loadPosts(LoadMode.FORCED);
            UploadUtils.onPostUploadedSnackbarHandler(getActivity(),
                    getActivity().findViewById(R.id.coordinator),
                    event.isError(), event.post, null, mSite, mDispatcher);
        }
    }

    /*
     * Media info for a post's featured image has been downloaded, tell
     * the adapter so it can show the featured image now that we have its URL
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (isAdded() && !event.isError() && mPostsListAdapter != null) {
            if (event.mediaList != null && event.mediaList.size() > 0) {
                MediaModel mediaModel = event.mediaList.get(0);
                mPostsListAdapter.mediaChanged(mediaModel);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (!isAdded() || event.isError() || event.canceled) {
            return;
        }

        if (event.media == null || event.media.getLocalPostId() == 0 || mSite.getId() != event.media.getLocalSiteId()) {
            // Not interested in media not attached to posts or not belonging to the current site
            return;
        }

        PostModel post = mPostStore.getPostByLocalPostId(event.media.getLocalPostId());
        if (post != null) {
            if ((event.media.isError() || event.canceled)){
                // if a media is cancelled or ends in error, and the post is not uploading nor queued,
                // (meaning there is no other pending media to be uploaded for this post)
                // then we should refresh it to show its new state
                if (!UploadService.isPostUploadingOrQueued(post)) {
                    // TODO: replace loadPosts for getPostListAdapter().notifyItemChanged(); kind of thing
                    loadPosts(LoadMode.FORCED);
                }
            } else {
                mPostsListAdapter.updateProgressForPost(post);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(VideoOptimizer.ProgressEvent event) {
        if (isAdded()) {
            PostModel post = mPostStore.getPostByLocalPostId(event.media.getLocalPostId());
            if (post != null) {
                mPostsListAdapter.updateProgressForPost(post);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UploadService.UploadErrorEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        if (event.post != null) {
            UploadUtils.onPostUploadedSnackbarHandler(getActivity(),
                    getActivity().findViewById(R.id.coordinator), true, event.post, event.errorMessage, mSite, mDispatcher);
        }
        else if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            UploadUtils.onMediaUploadedSnackbarHandler(getActivity(),
                    getActivity().findViewById(R.id.coordinator), true,
                    event.mediaModelList, mSite, event.errorMessage);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UploadService.UploadMediaSuccessEvent event) {
        EventBus.getDefault().removeStickyEvent(event);
        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            UploadUtils.onMediaUploadedSnackbarHandler(getActivity(),
                    getActivity().findViewById(R.id.coordinator), false,
                    event.mediaModelList, mSite, event.successMessage);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(UploadService.UploadMediaRetryEvent event) {
        if (!isAdded()) {
            return;
        }

        if (event.mediaModelList != null && !event.mediaModelList.isEmpty()) {
            // if there' a Post to which the retried media belongs, clear their status
            Set<PostModel> postsToRefresh = PostUtils.getPostsThatIncludeAnyOfTheseMedia(mPostStore, event.mediaModelList);
            // now that we know which Posts  to refresh, let's do it
            for (PostModel post : postsToRefresh) {
                int position = getPostListAdapter().getPositionForPost(post);
                if (position > -1) {
                    getPostListAdapter().notifyItemChanged(position);
                }
            }

        }
    }
}
