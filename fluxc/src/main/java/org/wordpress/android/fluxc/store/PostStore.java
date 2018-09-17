package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wellsql.generated.PostModelTable;
import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.generated.ListActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.list.ListDescriptor;
import org.wordpress.android.fluxc.model.list.ListType;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsError;
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsErrorType;
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload;
import org.wordpress.android.fluxc.store.ListStore.ListItemsChangedPayload;
import org.wordpress.android.fluxc.store.ListStore.ListItemsChangedPayload.ListItemsDeletedPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostStore extends Store {
    public static final int NUM_POSTS_PER_FETCH = 20;
    public static final int NUM_POST_LIST_PER_FETCH = 100;

    public static final List<PostStatus> DEFAULT_POST_STATUS_LIST = Collections.unmodifiableList(Arrays.asList(
            PostStatus.DRAFT,
            PostStatus.PENDING,
            PostStatus.PRIVATE,
            PostStatus.PUBLISHED,
            PostStatus.SCHEDULED));

    public static class FetchPostListPayload extends Payload<BaseNetworkError> {
        public ListDescriptor listDescriptor;
        public int offset;

        public FetchPostListPayload(ListDescriptor listDescriptor, int offset) {
            this.listDescriptor = listDescriptor;
            this.offset = offset;
        }
    }

    public static class PostListItem {
        public Long remotePostId;
        public String lastModified;

        public PostListItem(Long remotePostId, String lastModified) {
            this.remotePostId = remotePostId;
            this.lastModified = lastModified;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchPostListResponsePayload extends Payload<PostError> {
        @NotNull public ListDescriptor listDescriptor;
        @NotNull public List<PostListItem> postListItems;
        public boolean loadedMore;
        public boolean canLoadMore;

        public FetchPostListResponsePayload(@NonNull ListDescriptor listDescriptor,
                                            @NonNull List<PostListItem> postListItems,
                                            boolean loadedMore,
                                            boolean canLoadMore,
                                            @Nullable PostError error) {
            this.listDescriptor = listDescriptor;
            this.postListItems = postListItems;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
            this.error = error;
        }
    }

    public static class FetchPostsPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public boolean loadMore;
        public List<PostStatus> statusTypes;

        public FetchPostsPayload(SiteModel site) {
            this.site = site;
        }

        public FetchPostsPayload(SiteModel site, boolean loadMore) {
            this(site, loadMore, DEFAULT_POST_STATUS_LIST);
        }

        public FetchPostsPayload(SiteModel site, boolean loadMore, List<PostStatus> statusTypes) {
            this.site = site;
            this.loadMore = loadMore;
            this.statusTypes = statusTypes;
        }
    }

    public static class FetchPostsResponsePayload extends Payload<PostError> {
        public PostsModel posts;
        public SiteModel site;
        public boolean isPages;
        public boolean loadedMore;
        public boolean canLoadMore;

        public FetchPostsResponsePayload(PostsModel posts, SiteModel site, boolean isPages, boolean loadedMore,
                                         boolean canLoadMore) {
            this.posts = posts;
            this.site = site;
            this.isPages = isPages;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
        }

        public FetchPostsResponsePayload(PostError error, boolean isPages) {
            this.error = error;
            this.isPages = isPages;
        }
    }

    public static class RemotePostPayload extends Payload<PostError> {
        public PostModel post;
        public SiteModel site;

        public RemotePostPayload(PostModel post, SiteModel site) {
            this.post = post;
            this.site = site;
        }
    }

    public static class FetchPostResponsePayload extends RemotePostPayload {
        public PostAction origin = PostAction.FETCH_POST; // Only used to track fetching newly uploaded XML-RPC posts

        public FetchPostResponsePayload(PostModel post, SiteModel site) {
            super(post, site);
        }
    }

    public static class PostError implements OnChangedError {
        public PostErrorType type;
        public String message;

        public PostError(PostErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }

        public PostError(@NonNull String type, @NonNull String message) {
            this.type = PostErrorType.fromString(type);
            this.message = message;
        }

        public PostError(PostErrorType type) {
            this(type, "");
        }
    }

    // OnChanged events
    public static class OnPostChanged extends OnChanged<PostError> {
        public int rowsAffected;
        public boolean canLoadMore;
        public PostAction causeOfChange;

        public OnPostChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }

        public OnPostChanged(int rowsAffected, boolean canLoadMore) {
            this.rowsAffected = rowsAffected;
            this.canLoadMore = canLoadMore;
        }
    }

    public static class OnPostUploaded extends OnChanged<PostError> {
        public PostModel post;

        public OnPostUploaded(PostModel post) {
            this.post = post;
        }
    }

    public enum PostErrorType {
        UNKNOWN_POST,
        UNKNOWN_POST_TYPE,
        UNSUPPORTED_ACTION,
        UNAUTHORIZED,
        INVALID_RESPONSE,
        GENERIC_ERROR;

        public static PostErrorType fromString(String string) {
            if (string != null) {
                for (PostErrorType v : PostErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    private final PostRestClient mPostRestClient;
    private final PostXMLRPCClient mPostXMLRPCClient;
    // Ensures that the UploadStore is initialized whenever the PostStore is,
    // to ensure actions are shadowed and repeated by the UploadStore
    @SuppressWarnings("unused")
    @Inject UploadStore mUploadStore;

    @Inject
    public PostStore(Dispatcher dispatcher, PostRestClient postRestClient, PostXMLRPCClient postXMLRPCClient) {
        super(dispatcher);
        mPostRestClient = postRestClient;
        mPostXMLRPCClient = postXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "PostStore onRegister");
    }

    public PostModel instantiatePostModel(SiteModel site, boolean isPage) {
        return instantiatePostModel(site, isPage, null, null);
    }

    public PostModel instantiatePostModel(SiteModel site, boolean isPage, List<Long> categoryIds, String postFormat) {
        PostModel post = new PostModel();
        post.setLocalSiteId(site.getId());
        post.setIsLocalDraft(true);
        post.setIsPage(isPage);
        post.setDateLocallyChanged((DateTimeUtils.iso8601FromDate(DateTimeUtils.nowUTC())));
        if (categoryIds != null && !categoryIds.isEmpty()) {
            post.setCategoryIdList(categoryIds);
        }
        post.setPostFormat(postFormat);

        // Insert the post into the db, updating the object to include the local ID
        post = PostSqlUtils.insertPostForResult(post);

        // id is set to -1 if insertion fails
        if (post.getId() == -1) {
            return null;
        }
        return post;
    }

    /**
     * Returns all posts in the store for the given site as a {@link PostModel} list.
     */
    public List<PostModel> getPostsForSite(SiteModel site) {
        return PostSqlUtils.getPostsForSite(site, false);
    }

    /**
     * Returns posts with given format in the store for the given site as a {@link PostModel} list.
     */
    public List<PostModel> getPostsForSiteWithFormat(SiteModel site, List<String> postFormat) {
        return PostSqlUtils.getPostsForSiteWithFormat(site, postFormat, false);
    }

    /**
     * Returns all pages in the store for the given site as a {@link PostModel} list.
     */
    public List<PostModel> getPagesForSite(SiteModel site) {
        return PostSqlUtils.getPostsForSite(site, true);
    }

    /**
     * Returns the number of posts in the store for the given site.
     */
    public int getPostsCountForSite(SiteModel site) {
        return getPostsForSite(site).size();
    }

    /**
     * Returns the number of pages in the store for the given site.
     */
    public int getPagesCountForSite(SiteModel site) {
        return getPagesForSite(site).size();
    }

    /**
     * Returns all uploaded posts in the store for the given site.
     */
    public List<PostModel> getUploadedPostsForSite(SiteModel site) {
        return PostSqlUtils.getUploadedPostsForSite(site, false);
    }

    /**
     * Returns all uploaded pages in the store for the given site.
     */
    public List<PostModel> getUploadedPagesForSite(SiteModel site) {
        return PostSqlUtils.getUploadedPostsForSite(site, true);
    }

    /**
     * Returns the number of uploaded posts in the store for the given site.
     */
    public int getUploadedPostsCountForSite(SiteModel site) {
        return getUploadedPostsForSite(site).size();
    }

    /**
     * Returns the number of uploaded pages in the store for the given site.
     */
    public int getUploadedPagesCountForSite(SiteModel site) {
        return getUploadedPagesForSite(site).size();
    }

    /**
     * Given a local ID for a post, returns that post as a {@link PostModel}.
     */
    public PostModel getPostByLocalPostId(int localId) {
        List<PostModel> result = WellSql.select(PostModel.class)
                .where().equals(PostModelTable.ID, localId).endWhere()
                .getAsModel();

        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * Given a list of remote IDs for a post and the site to which it belongs, returns the posts as map where the
     * key is the remote post ID and the value is the {@link PostModel}.
     */
    public Map<Long, PostModel> getPostsByRemotePostIds(List<Long> remoteIds, SiteModel site) {
        if (site == null) {
            return Collections.emptyMap();
        }
        List<PostModel> postList = PostSqlUtils.getPostsByRemoteIds(remoteIds, site.getId());
        Map<Long, PostModel> postMap = new HashMap<>(postList.size());
        for (PostModel post : postList) {
            postMap.put(post.getRemotePostId(), post);
        }
        return postMap;
    }

    /**
     * Given a remote ID for a post and the site to which it belongs, returns that post as a {@link PostModel}.
     */
    public PostModel getPostByRemotePostId(long remoteId, SiteModel site) {
        List<PostModel> result = WellSql.select(PostModel.class)
                                        .where().equals(PostModelTable.REMOTE_POST_ID, remoteId)
                                        .equals(PostModelTable.LOCAL_SITE_ID, site.getId()).endWhere()
                                        .getAsModel();

        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    /**
     * returns the total number of posts with local changes across all sites
     */
    public static int getNumLocalChanges() {
        return PostSqlUtils.getNumLocalChanges();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof PostAction)) {
            return;
        }

        switch ((PostAction) actionType) {
            case FETCH_POST_LIST:
                fetchPostList((FetchPostListPayload) action.getPayload());
                break;
            case FETCHED_POST_LIST:
                fetchedPostList((FetchPostListResponsePayload) action.getPayload());
                break;
            case FETCH_POSTS:
                fetchPosts((FetchPostsPayload) action.getPayload(), false);
                break;
            case FETCH_PAGES:
                fetchPosts((FetchPostsPayload) action.getPayload(), true);
                break;
            case FETCHED_POSTS:
                handleFetchPostsCompleted((FetchPostsResponsePayload) action.getPayload());
                break;
            case FETCH_POST:
                fetchPost((RemotePostPayload) action.getPayload());
                break;
            case FETCHED_POST:
                handleFetchSinglePostCompleted((FetchPostResponsePayload) action.getPayload());
                break;
            case PUSH_POST:
                pushPost((RemotePostPayload) action.getPayload());
                break;
            case PUSHED_POST:
                handlePushPostCompleted((RemotePostPayload) action.getPayload());
                break;
            case UPDATE_POST:
                updatePost((PostModel) action.getPayload(), true);
                break;
            case DELETE_POST:
                deletePost((RemotePostPayload) action.getPayload());
                break;
            case DELETED_POST:
                handleDeletePostCompleted((RemotePostPayload) action.getPayload());
                break;
            case REMOVE_POST:
                removePost((PostModel) action.getPayload());
                break;
            case REMOVE_ALL_POSTS:
                removeAllPosts();
                break;
        }
    }

    private void deletePost(RemotePostPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.deletePost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.deletePost(payload.post, payload.site);
        }
    }

    private void fetchPost(RemotePostPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.fetchPost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPost(payload.post, payload.site);
        }
    }

    private void fetchPostList(FetchPostListPayload payload) {
        // TODO: We shouldn't access SiteSqlUtils from here, fix this!
        SiteModel site =
                SiteSqlUtils.getSitesWith(SiteModelTable.ID, payload.listDescriptor.getLocalSiteId()).getAsModel()
                            .get(0);
        if (site.isUsingWpComRestApi()) {
            mPostRestClient.fetchPostList(site.getSiteId(), payload.listDescriptor, payload.offset,
                    NUM_POST_LIST_PER_FETCH);
        } else {
            mPostXMLRPCClient.fetchPostList(site, payload.listDescriptor, payload.offset,
                    NUM_POST_LIST_PER_FETCH);
        }
    }

    private void fetchedPostList(FetchPostListResponsePayload payload) {
        FetchedListItemsError fetchedListItemsError = null;
        List<Long> postIds;
        if (payload.isError()) {
            fetchedListItemsError =
                    new FetchedListItemsError(FetchedListItemsErrorType.GENERIC_ERROR, payload.error.message);
            postIds = Collections.emptyList();
        } else {
            postIds = new ArrayList<>(payload.postListItems.size());
            for (PostListItem item : payload.postListItems) {
                postIds.add(item.remotePostId);
                // TODO: compare lastmodified for each item
            }
        }

        FetchedListItemsPayload fetchedListItemsPayload =
                new FetchedListItemsPayload(payload.listDescriptor, postIds,
                        payload.loadedMore, payload.canLoadMore, fetchedListItemsError);
        mDispatcher.dispatch(ListActionBuilder.newFetchedListItemsAction(fetchedListItemsPayload));
    }

    private void fetchPosts(FetchPostsPayload payload, boolean pages) {
        int offset = 0;
        if (payload.loadMore) {
            offset = PostSqlUtils.getUploadedPostsForSite(payload.site, pages).size();
        }

        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.fetchPosts(payload.site, pages, payload.statusTypes, offset, NUM_POSTS_PER_FETCH);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPosts(payload.site, pages, offset, NUM_POSTS_PER_FETCH);
        }
    }

    private void handleDeletePostCompleted(RemotePostPayload payload) {
        OnPostChanged event = new OnPostChanged(0);
        event.causeOfChange = PostAction.DELETE_POST;

        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PostSqlUtils.deletePost(payload.post);
            ListItemsDeletedPayload listActionPayload =
                    new ListItemsDeletedPayload(listDescriptorsToUpdate(payload.site.getId()),
                            postIdListFromPost(payload.post));
            dispatchListItemUpdatedAction(listActionPayload);
        }

        emitChange(event);
    }

    private void handleFetchPostsCompleted(FetchPostsResponsePayload payload) {
        OnPostChanged onPostChanged;

        if (payload.isError()) {
            onPostChanged = new OnPostChanged(0);
            onPostChanged.error = payload.error;
        } else {
            // Clear existing uploading posts if this is a fresh fetch (loadMore = false in the original request)
            // This is the simplest way of keeping our local posts in sync with remote posts (in case of deletions,
            // or if the user manual changed some post IDs)
            if (!payload.loadedMore) {
                PostSqlUtils.deleteUploadedPostsForSite(payload.site, payload.isPages);
            }

            int rowsAffected = 0;
            for (PostModel post : payload.posts.getPosts()) {
                rowsAffected += PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(post);
            }

            onPostChanged = new OnPostChanged(rowsAffected, payload.canLoadMore);
        }

        if (payload.isPages) {
            onPostChanged.causeOfChange = PostAction.FETCH_PAGES;
        } else {
            onPostChanged.causeOfChange = PostAction.FETCH_POSTS;
        }

        emitChange(onPostChanged);
    }

    private void handleFetchSinglePostCompleted(FetchPostResponsePayload payload) {
        if (payload.origin == PostAction.PUSH_POST) {
            OnPostUploaded onPostUploaded = new OnPostUploaded(payload.post);
            if (payload.isError()) {
                onPostUploaded.error = payload.error;
            } else {
                updatePost(payload.post, false);
            }
            emitChange(onPostUploaded);
            return;
        }

        if (payload.isError()) {
            OnPostChanged event = new OnPostChanged(0);
            event.error = payload.error;
            event.causeOfChange = PostAction.UPDATE_POST;
            emitChange(event);
        } else {
            updatePost(payload.post, false);
        }
    }

    private void handlePushPostCompleted(RemotePostPayload payload) {
        if (payload.isError()) {
            OnPostUploaded onPostUploaded = new OnPostUploaded(payload.post);
            onPostUploaded.error = payload.error;
            emitChange(onPostUploaded);
        } else {
            if (payload.site.isUsingWpComRestApi()) {
                // The WP.COM REST API response contains the modified post, so we're already in sync with the server
                // All we need to do is store it and emit OnPostChanged
                updatePost(payload.post, false);
                emitChange(new OnPostUploaded(payload.post));
            } else {
                // XML-RPC does not respond to new/edit post calls with the modified post
                // Update the post locally to reflect its uploaded status, but also request a fresh copy
                // from the server to ensure local copy matches server
                PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(payload.post);
                mPostXMLRPCClient.fetchPost(payload.post, payload.site, PostAction.PUSH_POST);
            }
        }
    }

    private void pushPost(RemotePostPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.pushPost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.pushPost(payload.post, payload.site);
        }
    }

    private void updatePost(PostModel post, boolean changeLocalDate) {
        if (changeLocalDate) {
            post.setDateLocallyChanged((DateTimeUtils.iso8601UTCFromDate(DateTimeUtils.nowUTC())));
        }
        int rowsAffected = PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(post);
        OnPostChanged onPostChanged = new OnPostChanged(rowsAffected);
        onPostChanged.causeOfChange = PostAction.UPDATE_POST;
        emitChange(onPostChanged);
    }

    private void removePost(PostModel post) {
        int rowsAffected = PostSqlUtils.deletePost(post);

        OnPostChanged onPostChanged = new OnPostChanged(rowsAffected);
        onPostChanged.causeOfChange = PostAction.REMOVE_POST;
        emitChange(onPostChanged);
    }

    private void removeAllPosts() {
        int rowsAffected = PostSqlUtils.deleteAllPosts();
        OnPostChanged event = new OnPostChanged(rowsAffected);
        event.causeOfChange = PostAction.REMOVE_ALL_POSTS;
        emitChange(event);
    }

    private void dispatchListItemUpdatedAction(ListItemsChangedPayload payload) {
       mDispatcher.dispatch(ListActionBuilder.newListItemsChangedAction(payload));
    }

    private List<ListDescriptor> listDescriptorsToUpdate(int siteId) {
        return Collections.singletonList(new ListDescriptor(ListType.POST, siteId, null, null));
    }

    private List<Long> postIdListFromPost(PostModel post) {
        return Collections.singletonList(post.getRemotePostId());
    }
}
