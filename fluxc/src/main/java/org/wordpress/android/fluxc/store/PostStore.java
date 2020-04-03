package org.wordpress.android.fluxc.store;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.SelectQuery;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.fluxc.BuildConfig;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.generated.ListActionBuilder;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.FetchPages;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.FetchPosts;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.RemoveAllPosts;
import org.wordpress.android.fluxc.model.LocalOrRemoteId;
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.list.ListOrder;
import org.wordpress.android.fluxc.model.list.PostListDescriptor;
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite;
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.model.revisions.Diff;
import org.wordpress.android.fluxc.model.revisions.LocalDiffModel;
import org.wordpress.android.fluxc.model.revisions.LocalDiffType;
import org.wordpress.android.fluxc.model.revisions.LocalRevisionModel;
import org.wordpress.android.fluxc.model.revisions.RevisionModel;
import org.wordpress.android.fluxc.model.revisions.RevisionsModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRemoteAutoSaveModel;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload;
import org.wordpress.android.fluxc.store.ListStore.ListError;
import org.wordpress.android.fluxc.store.ListStore.ListErrorType;
import org.wordpress.android.fluxc.store.ListStore.ListItemsRemovedPayload;
import org.wordpress.android.fluxc.utils.ObjectsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostStore extends Store {
    public static final int NUM_POSTS_PER_FETCH = 20;

    public static final List<PostStatus> DEFAULT_POST_STATUS_LIST = Collections.unmodifiableList(Arrays.asList(
            PostStatus.DRAFT,
            PostStatus.PENDING,
            PostStatus.PRIVATE,
            PostStatus.PUBLISHED,
            PostStatus.SCHEDULED));

    public static class FetchPostListPayload extends Payload<BaseNetworkError> {
        public PostListDescriptor listDescriptor;
        public long offset;

        public FetchPostListPayload(PostListDescriptor listDescriptor, long offset) {
            this.listDescriptor = listDescriptor;
            this.offset = offset;
        }
    }

    public static class PostListItem {
        public Long remotePostId;
        public String lastModified;
        public String status;
        public String autoSaveModified;

        public PostListItem(Long remotePostId, String lastModified, String status, String autoSaveModified) {
            this.remotePostId = remotePostId;
            this.lastModified = lastModified;
            this.status = status;
            this.autoSaveModified = autoSaveModified;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchPostListResponsePayload extends Payload<PostError> {
        @NotNull public PostListDescriptor listDescriptor;
        @NotNull public List<PostListItem> postListItems;
        public boolean loadedMore;
        public boolean canLoadMore;

        public FetchPostListResponsePayload(@NonNull PostListDescriptor listDescriptor,
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
            this(site, false);
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

    @SuppressWarnings("WeakerAccess")
    public static class DeletedPostPayload extends Payload<PostError> {
        @NonNull public PostModel postToBeDeleted;
        @NonNull public SiteModel site;
        @NonNull public PostDeleteActionType postDeleteActionType;
        @Nullable public PostModel deletedPostResponse;

        public DeletedPostPayload(@NonNull PostModel postToBeDeleted, @NonNull SiteModel site,
                                  @NonNull PostDeleteActionType postDeleteActionType,
                                  @Nullable PostModel deletedPostResponse) {
            this.postToBeDeleted = postToBeDeleted;
            this.site = site;
            this.postDeleteActionType = postDeleteActionType;
            this.deletedPostResponse = deletedPostResponse;
        }

        public DeletedPostPayload(@NonNull PostModel postToBeDeleted, @NonNull SiteModel site,
                                  @NonNull PostDeleteActionType postDeleteActionType,
                                  @NonNull PostError deletePostError) {
            this.postToBeDeleted = postToBeDeleted;
            this.site = site;
            this.postDeleteActionType = postDeleteActionType;
            this.deletedPostResponse = null;
            this.error = deletePostError;
        }
    }

    public static class FetchRevisionsPayload extends Payload<BaseNetworkError> {
        public PostModel post;
        public SiteModel site;

        public FetchRevisionsPayload(PostModel post, SiteModel site) {
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

    public static class FetchRevisionsResponsePayload extends Payload<BaseNetworkError> {
        public PostModel post;
        public RevisionsModel revisionsModel;

        public FetchRevisionsResponsePayload(PostModel post, RevisionsModel revisionsModel) {
            this.post = post;
            this.revisionsModel = revisionsModel;
        }
    }

    public static class RemoteAutoSavePostPayload extends Payload<PostError> {
        public PostRemoteAutoSaveModel autoSaveModel;
        public int localPostId;
        public long remotePostId;
        public SiteModel site;

        public RemoteAutoSavePostPayload(int localPostId, long remotePostId,
                                         @NonNull PostRemoteAutoSaveModel autoSaveModel, @NonNull SiteModel site) {
            this.localPostId = localPostId;
            this.remotePostId = remotePostId;
            this.autoSaveModel = autoSaveModel;
            this.site = site;
        }

        public RemoteAutoSavePostPayload(int localPostId, long remotePostId, @NonNull PostError error) {
            this.localPostId = localPostId;
            this.remotePostId = remotePostId;
            this.error = error;
        }
    }

    public static class FetchPostStatusResponsePayload extends Payload<PostError> {
        public PostModel post;
        public SiteModel site;
        public String remotePostStatus;

        public FetchPostStatusResponsePayload(PostModel post, SiteModel site) {
            this.post = post;
            this.site = site;
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

    public static class RevisionError implements OnChangedError {
        @NonNull public RevisionsErrorType type;
        @Nullable public String message;

        public RevisionError(@NonNull RevisionsErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }
    }

    // OnChanged events
    public static class OnPostChanged extends OnChanged<PostError> {
        public final int rowsAffected;
        public final boolean canLoadMore;
        public final CauseOfOnPostChanged causeOfChange;

        public OnPostChanged(CauseOfOnPostChanged causeOfChange, int rowsAffected) {
            this.causeOfChange = causeOfChange;
            this.rowsAffected = rowsAffected;
            this.canLoadMore = false;
        }

        public OnPostChanged(CauseOfOnPostChanged causeOfChange, int rowsAffected, boolean canLoadMore) {
            this.causeOfChange = causeOfChange;
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

    public static class OnRevisionsFetched extends OnChanged<RevisionError> {
        public PostModel post;
        public RevisionsModel revisionsModel;

        OnRevisionsFetched(PostModel post, RevisionsModel revisionsModel) {
            this.post = post;
            this.revisionsModel = revisionsModel;
        }
    }

    public static class OnPostStatusFetched extends OnChanged<PostError> {
        public PostModel post;
        public String remotePostStatus;

        OnPostStatusFetched(PostModel post, String remotePostStatus, PostError error) {
            this.post = post;
            this.remotePostStatus = remotePostStatus;
            this.error = error;
        }
    }

    public enum PostDeleteActionType {
        TRASH,
        DELETE
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

    public enum RevisionsErrorType {
        GENERIC_ERROR
    }

    private final PostRestClient mPostRestClient;
    private final PostXMLRPCClient mPostXMLRPCClient;
    private final PostSqlUtils mPostSqlUtils;
    // Ensures that the UploadStore is initialized whenever the PostStore is,
    // to ensure actions are shadowed and repeated by the UploadStore
    @SuppressWarnings("unused")
    @Inject UploadStore mUploadStore;

    @Inject
    public PostStore(Dispatcher dispatcher, PostRestClient postRestClient, PostXMLRPCClient postXMLRPCClient,
                     PostSqlUtils postSqlUtils) {
        super(dispatcher);
        mPostRestClient = postRestClient;
        mPostXMLRPCClient = postXMLRPCClient;
        mPostSqlUtils = postSqlUtils;
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
        post.setDateLocallyChanged((DateTimeUtils.iso8601UTCFromDate(new Date())));
        if (categoryIds != null && !categoryIds.isEmpty()) {
            post.setCategoryIdList(categoryIds);
        }
        post.setPostFormat(postFormat);

        // Insert the post into the db, updating the object to include the local ID
        post = mPostSqlUtils.insertPostForResult(post);

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
        return mPostSqlUtils.getPostsForSite(site, false);
    }

    /**
     * Returns posts with given format in the store for the given site as a {@link PostModel} list.
     */
    public List<PostModel> getPostsForSiteWithFormat(SiteModel site, List<String> postFormat) {
        return mPostSqlUtils.getPostsForSiteWithFormat(site, postFormat, false);
    }

    /**
     * Returns all pages in the store for the given site as a {@link PostModel} list.
     */
    public List<PostModel> getPagesForSite(SiteModel site) {
        return mPostSqlUtils.getPostsForSite(site, true);
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
        return mPostSqlUtils.getUploadedPostsForSite(site, false);
    }

    /**
     * Returns all uploaded pages in the store for the given site.
     */
    public List<PostModel> getUploadedPagesForSite(SiteModel site) {
        return mPostSqlUtils.getUploadedPostsForSite(site, true);
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
     * Returns all posts that are local drafts for the given site.
     */
    public List<PostModel> getLocalDraftPosts(@NonNull SiteModel site) {
        return mPostSqlUtils.getLocalDrafts(site.getId(), false);
    }

    /**
     * Returns all posts that are local drafts or has been locally changed.
     */
    public List<PostModel> getPostsWithLocalChanges(@NonNull SiteModel site) {
        return mPostSqlUtils.getPostsWithLocalChanges(site.getId(), false);
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

    public List<PostModel> getPostsByLocalOrRemotePostIds(List<? extends LocalOrRemoteId> localOrRemoteIds,
                                                          SiteModel site) {
        if (localOrRemoteIds == null || site == null) {
            return Collections.emptyList();
        }
        return mPostSqlUtils.getPostsByLocalOrRemotePostIds(localOrRemoteIds, site.getId());
    }

    /**
     * Given a list of remote IDs for a post and the site to which it belongs, returns the posts as map where the
     * key is the remote post ID and the value is the {@link PostModel}.
     */
    private Map<Long, PostModel> getPostsByRemotePostIds(List<Long> remoteIds, SiteModel site) {
        if (site == null) {
            return Collections.emptyMap();
        }
        List<PostModel> postList = mPostSqlUtils.getPostsByRemoteIds(remoteIds, site.getId());
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
     * Returns the local posts for the given post list descriptor.
     */
    public @NonNull List<LocalId> getLocalPostIdsForDescriptor(PostListDescriptor postListDescriptor) {
        String searchQuery = null;
        if (postListDescriptor instanceof PostListDescriptorForRestSite) {
            PostListDescriptorForRestSite descriptor = (PostListDescriptorForRestSite) postListDescriptor;
            searchQuery = descriptor.getSearchQuery();
            if (!(descriptor.getStatusList().contains(PostStatus.DRAFT))) {
                // Drafts should not be included
                return Collections.emptyList();
            }
        }
        String orderBy = null;
        switch (postListDescriptor.getOrderBy()) {
            case DATE:
                orderBy = PostModelTable.DATE_CREATED;
                break;
            case LAST_MODIFIED:
                orderBy = PostModelTable.DATE_LOCALLY_CHANGED;
                break;
            case TITLE:
                orderBy = PostModelTable.TITLE;
                break;
            case COMMENT_COUNT:
                // Local drafts can't have comments
                orderBy = PostModelTable.DATE_CREATED;
                break;
            case ID:
                orderBy = PostModelTable.ID;
                break;
        }
        int order;
        if (postListDescriptor.getOrder() == ListOrder.ASC) {
            order = SelectQuery.ORDER_ASCENDING;
        } else {
            order = SelectQuery.ORDER_DESCENDING;
        }
        return mPostSqlUtils.getLocalPostIdsForFilter(postListDescriptor.getSite(), false, searchQuery, orderBy, order);
    }

    /**
     * returns the total number of posts with local changes across all sites
     */
    public int getNumLocalChanges() {
        return mPostSqlUtils.getNumLocalChanges();
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
                handleFetchPostList((FetchPostListPayload) action.getPayload());
                break;
            case FETCHED_POST_LIST:
                handleFetchedPostList((FetchPostListResponsePayload) action.getPayload());
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
            case FETCH_POST_STATUS:
                fetchPostStatus((RemotePostPayload) action.getPayload());
                break;
            case FETCHED_POST:
                handleFetchSinglePostCompleted((FetchPostResponsePayload) action.getPayload());
                break;
            case FETCHED_POST_STATUS:
                handleFetchPostStatusCompleted((FetchPostStatusResponsePayload) action.getPayload());
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
                handleDeletePostCompleted((DeletedPostPayload) action.getPayload());
                break;
            case RESTORE_POST:
                handleRestorePost((RemotePostPayload) action.getPayload());
                break;
            case RESTORED_POST:
                handleRestorePostCompleted((RemotePostPayload) action.getPayload(), false);
                break;
            case REMOVE_POST:
                removePost((PostModel) action.getPayload());
                break;
            case REMOVE_ALL_POSTS:
                removeAllPosts();
                break;
            case REMOTE_AUTO_SAVE_POST:
                remoteAutoSavePost((RemotePostPayload) action.getPayload());
                break;
            case REMOTE_AUTO_SAVED_POST:
                handleRemoteAutoSavedPost((RemoteAutoSavePostPayload) action.getPayload());
                break;
            case FETCH_REVISIONS:
                fetchRevisions((FetchRevisionsPayload) action.getPayload());
                break;
            case FETCHED_REVISIONS:
                handleFetchedRevisions((FetchRevisionsResponsePayload) action.getPayload());
                break;
        }
    }

    private void deletePost(RemotePostPayload payload) {
        PostDeleteActionType postDeleteActionType = PostStatus.fromPost(payload.post) == PostStatus.TRASHED
                ? PostDeleteActionType.DELETE : PostDeleteActionType.TRASH;
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.deletePost(payload.post, payload.site, postDeleteActionType);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.deletePost(payload.post, payload.site, postDeleteActionType);
        }
    }

    private void handleRestorePost(RemotePostPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.restorePost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            PostModel postToRestore = payload.post;
            if (PostStatus.fromPost(postToRestore) == PostStatus.TRASHED) {
                postToRestore.setStatus(PostStatus.PUBLISHED.toString());
            }
            mPostXMLRPCClient.restorePost(postToRestore, payload.site);
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

    private void fetchPostStatus(RemotePostPayload payload) {
        if (payload.post.isLocalDraft()) {
            // If the post is a local draft, it won't have a remote post status
            FetchPostStatusResponsePayload responsePayload =
                    new FetchPostStatusResponsePayload(payload.post, payload.site);
            responsePayload.error = new PostError(PostErrorType.UNKNOWN_POST);
            mDispatcher.dispatch(PostActionBuilder.newFetchedPostStatusAction(responsePayload));
            return;
        }
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.fetchPostStatus(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPostStatus(payload.post, payload.site);
        }
    }

    private void handleFetchPostList(FetchPostListPayload payload) {
        if (payload.listDescriptor instanceof PostListDescriptorForRestSite) {
            PostListDescriptorForRestSite descriptor = (PostListDescriptorForRestSite) payload.listDescriptor;
            mPostRestClient.fetchPostList(descriptor, payload.offset);
        } else if (payload.listDescriptor instanceof PostListDescriptorForXmlRpcSite) {
            PostListDescriptorForXmlRpcSite descriptor = (PostListDescriptorForXmlRpcSite) payload.listDescriptor;
            mPostXMLRPCClient.fetchPostList(descriptor, payload.offset);
        }
    }

    private void handleFetchedPostList(FetchPostListResponsePayload payload) {
        ListError fetchedListItemsError = null;
        List<Long> postIds;
        if (payload.isError()) {
            ListErrorType errorType = payload.error.type == PostErrorType.UNAUTHORIZED ? ListErrorType.PERMISSION_ERROR
                    : ListErrorType.GENERIC_ERROR;
            fetchedListItemsError = new ListError(errorType, payload.error.message);
            postIds = Collections.emptyList();
        } else {
            postIds = new ArrayList<>(payload.postListItems.size());
            SiteModel site = payload.listDescriptor.getSite();
            for (PostListItem item : payload.postListItems) {
                postIds.add(item.remotePostId);
            }
            Map<Long, PostModel> posts = getPostsByRemotePostIds(postIds, site);
            for (PostListItem item : payload.postListItems) {
                PostModel post = posts.get(item.remotePostId);
                if (post == null) {
                    // Post doesn't exist in the DB, nothing to do.
                    continue;
                }

                boolean isAutoSaveChanged = !ObjectsUtils.equals(post.getAutoSaveModified(), item.autoSaveModified);

                // Check if the post's last modified date or status have changed.
                // We need to check status separately because when a scheduled post is published, its modified date
                // will not be updated.
                boolean isPostChanged =
                        !post.getLastModified().equals(item.lastModified)
                        || !post.getStatus().equals(item.status);

                /*
                 * This is a hacky workaround. When `/autosave` endpoint is invoked on a draft, the server
                 * automatically updates the post content and clears autosave object instead of just updating the
                 * autosave object. This results in a false-positive conflict as the PostModel.lastModified date field
                 * gets updated and on the next post list fetch the app thinks the post has been changed both in remote
                 * and locally.
                 *
                 * Since the app doesn't know the current status in the remote, it can't assume what
                  * was updated. However, if we know the last modified date is equal to the date we have in local
                  * autosave object we are sure that our invocation of /autosave updated the post directly.
                 */
                if (isPostChanged && item.lastModified.equals(post.getAutoSaveModified())
                    && item.autoSaveModified == null) {
                    isPostChanged = false;
                    isAutoSaveChanged = false;
                }

                if (isPostChanged || isAutoSaveChanged) {
                    // Dispatch a fetch action for the posts that are changed, but not for posts with local changes
                    // as we'd otherwise overwrite and lose these local changes forever
                    if (!post.isLocallyChanged()) {
                        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(new RemotePostPayload(post, site)));
                    } else if (isPostChanged) {
                        // at this point we know there's a potential version conflict (the post has been modified
                        // both locally and on the remote), so flag the local version of the Post so the
                        // hosting app can inform the user and the user can decide and take action
                        post.setRemoteLastModified(item.lastModified);
                        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post));
                    } else if (isAutoSaveChanged) {
                        // We currently don't want to do anything - we can't fetch the post from the remote as we'd
                        // override the local changes. The plan is to introduce improved conflict resolution on the
                        // UI and handle even the scenario for cases when the only thing that has changed is the
                        // autosave object. We'll probably need to introduce something like `remoteAutoSaveModified`
                        // field.
                        // Btw we'll also need to add `else if (isPostChanged && isAutoSaveChanged) case in front of
                        // `else if (isPostChanged)` in v2.
                    }
                }
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
            offset = mPostSqlUtils.getUploadedPostsForSite(payload.site, pages).size();
        }

        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.fetchPosts(payload.site, pages, payload.statusTypes, offset, NUM_POSTS_PER_FETCH);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPosts(payload.site, pages, payload.statusTypes, offset);
        }
    }

    private void fetchRevisions(FetchRevisionsPayload payload) {
        mPostRestClient.fetchRevisions(payload.post, payload.site);
    }

    private void handleFetchedRevisions(FetchRevisionsResponsePayload payload) {
        OnRevisionsFetched onRevisionsFetched = new OnRevisionsFetched(payload.post, payload.revisionsModel);

        if (payload.isError()) {
            onRevisionsFetched.error = new RevisionError(RevisionsErrorType.GENERIC_ERROR, payload.error.message);
        }

        emitChange(onRevisionsFetched);
    }

    private void handleDeletePostCompleted(DeletedPostPayload payload) {
        CauseOfOnPostChanged causeOfChange = new CauseOfOnPostChanged.DeletePost(payload.postToBeDeleted.getId(),
                payload.postToBeDeleted.getRemotePostId(), payload.postDeleteActionType);
        OnPostChanged event = new OnPostChanged(causeOfChange, 0);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            if (payload.postDeleteActionType == PostDeleteActionType.TRASH) {
                handlePostSuccessfullyTrashed(payload);
            } else {
                // If the post is completely removed from the server, remove it from the local DB as well
                mDispatcher.dispatch(PostActionBuilder.newRemovePostAction(payload.postToBeDeleted));
            }
        }
        emitChange(event);
    }

    /**
     * Saves the changes for the trashed post in the DB, lets ListStore know about updated lists.
     * <p>
     * If the trashed post is for an XML-RPC site, it'll also fetch the updated post from remote since XML-RPC delete
     * call doesn't return the updated post.
     */
    private void handlePostSuccessfullyTrashed(DeletedPostPayload payload) {
        mDispatcher.dispatch(ListActionBuilder.newListRequiresRefreshAction(
                PostListDescriptor.calculateTypeIdentifier(payload.postToBeDeleted.getLocalSiteId())));

        PostModel postToSave;
        if (payload.deletedPostResponse != null) {
            postToSave = payload.deletedPostResponse;
        } else {
            /*
             * XML-RPC delete request doesn't return the updated post, so we need to manually change the status
             * and then fetch the post from remote to ensure post is properly synced.
             */
            postToSave = payload.postToBeDeleted;
            postToSave.setStatus(PostStatus.TRASHED.toString());
            mDispatcher.dispatch(
                    PostActionBuilder.newFetchPostAction(new RemotePostPayload(postToSave, payload.site)));
        }
        mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postToSave);
    }

    private void handleFetchPostsCompleted(FetchPostsResponsePayload payload) {
        OnPostChanged onPostChanged;

        CauseOfOnPostChanged causeOfChange;
        if (payload.isPages) {
            causeOfChange = FetchPages.INSTANCE;
        } else {
            causeOfChange = FetchPosts.INSTANCE;
        }

        if (payload.isError()) {
            onPostChanged = new OnPostChanged(causeOfChange, 0);
            onPostChanged.error = payload.error;
        } else {
            // Clear existing uploading posts if this is a fresh fetch (loadMore = false in the original request)
            // This is the simplest way of keeping our local posts in sync with remote posts (in case of deletions,
            // or if the user manual changed some post IDs)
            if (!payload.loadedMore) {
                mPostSqlUtils.deleteUploadedPostsForSite(payload.site, payload.isPages);
            }

            int rowsAffected = 0;
            for (PostModel post : payload.posts.getPosts()) {
                rowsAffected += mPostSqlUtils.insertOrUpdatePostKeepingLocalChanges(post);
            }

            onPostChanged = new OnPostChanged(causeOfChange, rowsAffected, payload.canLoadMore);
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
        } else if (payload.origin == PostAction.RESTORE_POST) {
            handleRestorePostCompleted(payload, true);
            return;
        }

        if (payload.isError()) {
            OnPostChanged event = new OnPostChanged(
                    new CauseOfOnPostChanged.UpdatePost(payload.post.getId(), payload.post.getRemotePostId()), 0);
            event.error = payload.error;
            emitChange(event);
        } else {
            updatePost(payload.post, false);
        }
    }

    private void handleFetchPostStatusCompleted(FetchPostStatusResponsePayload payload) {
        emitChange(new OnPostStatusFetched(payload.post, payload.remotePostStatus, payload.error));
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
                mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(payload.post);
                mPostXMLRPCClient.fetchPost(payload.post, payload.site, PostAction.PUSH_POST);
            }
        }
    }

    private void handleRestorePostCompleted(RemotePostPayload payload, boolean syncingNonWpComPost) {
        if (payload.isError()) {
            OnPostChanged event = new OnPostChanged(
                    new CauseOfOnPostChanged.RestorePost(payload.post.getId(), payload.post.getRemotePostId()), 0);
            event.error = payload.error;
            emitChange(event);
        } else {
            if (payload.site.isUsingWpComRestApi() || syncingNonWpComPost) {
                restorePost(payload.post);
            } else {
                // XML-RPC responds to post restore request with status boolean
                // Update the post locally to reflect its published state, and request a fresh copy
                // from the server to ensure local copy matches server
                mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(payload.post);
                mPostXMLRPCClient.fetchPost(payload.post, payload.site, PostAction.RESTORE_POST);
            }
        }
    }

    private void pushPost(RemotePostPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.pushPost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            PostModel postToPush = payload.post;
            // empty status indicates that the post is new
            if (TextUtils.isEmpty(postToPush.getStatus())) {
                postToPush.setStatus(PostStatus.PUBLISHED.toString());
            }
            mPostXMLRPCClient.pushPost(postToPush, payload.site);
        }
    }

    private void updatePost(PostModel post, boolean changeLocalDate) {
        if (changeLocalDate) {
            post.setDateLocallyChanged((DateTimeUtils.iso8601UTCFromDate(new Date())));
        }
        int rowsAffected = mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(post);
        CauseOfOnPostChanged causeOfChange = new CauseOfOnPostChanged.UpdatePost(post.getId(), post.getRemotePostId());
        OnPostChanged onPostChanged = new OnPostChanged(causeOfChange, rowsAffected);
        emitChange(onPostChanged);

        mDispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(
                PostListDescriptor.calculateTypeIdentifier(post.getLocalSiteId())));
    }

    private void removePost(PostModel post) {
        if (post == null) {
            return;
        }
        mDispatcher.dispatch(ListActionBuilder.newListItemsRemovedAction(
                new ListItemsRemovedPayload(PostListDescriptor.calculateTypeIdentifier(post.getLocalSiteId()),
                        Collections.singletonList(post.getRemotePostId()))));
        int rowsAffected = mPostSqlUtils.deletePost(post);

        CauseOfOnPostChanged causeOfChange = new CauseOfOnPostChanged.RemovePost(post.getId(), post.getRemotePostId());
        OnPostChanged onPostChanged = new OnPostChanged(causeOfChange, rowsAffected);
        emitChange(onPostChanged);
    }

    private void removeAllPosts() {
        int rowsAffected = mPostSqlUtils.deleteAllPosts();
        OnPostChanged event = new OnPostChanged(RemoveAllPosts.INSTANCE, rowsAffected);
        emitChange(event);
    }

    private void restorePost(PostModel postModel) {
        int rowsAffected = mPostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(postModel);
        CauseOfOnPostChanged causeOfChange =
                new CauseOfOnPostChanged.RestorePost(postModel.getId(), postModel.getRemotePostId());
        OnPostChanged onPostChanged = new OnPostChanged(causeOfChange, rowsAffected);
        emitChange(onPostChanged);

        mDispatcher.dispatch(ListActionBuilder
                .newListRequiresRefreshAction(PostListDescriptor.calculateTypeIdentifier(postModel.getLocalSiteId())));
    }

    private void remoteAutoSavePost(RemotePostPayload payload) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.remoteAutoSavePost(payload.post, payload.site);
        } else {
            PostError postError = new PostError(
                    PostErrorType.UNSUPPORTED_ACTION,
                    "Remote-auto-save not support on self-hosted sites."
            );
            RemoteAutoSavePostPayload response =
                    new RemoteAutoSavePostPayload(payload.post.getId(), payload.post.getRemotePostId(), postError);
            mDispatcher.dispatch(UploadActionBuilder.newRemoteAutoSavedPostAction(response));
        }
    }

    private void handleRemoteAutoSavedPost(RemoteAutoSavePostPayload payload) {
        CauseOfOnPostChanged causeOfChange =
                new CauseOfOnPostChanged.RemoteAutoSavePost(payload.localPostId, payload.remotePostId);
        OnPostChanged onPostChanged;

        if (payload.isError()) {
            onPostChanged = new OnPostChanged(causeOfChange, 0);
            onPostChanged.error = payload.error;
        } else {
            int rowsAffected = mPostSqlUtils.updatePostsAutoSave(payload.site, payload.autoSaveModel);
            if (rowsAffected != 1) {
                String errorMsg = "Updating fields of a single post affected: " + rowsAffected + " rows";
                AppLog.e(AppLog.T.API, errorMsg);
                if (BuildConfig.DEBUG) {
                    throw new RuntimeException(errorMsg);
                }
            }
            onPostChanged = new OnPostChanged(causeOfChange, rowsAffected);
        }
        emitChange(onPostChanged);
    }

    public void setLocalRevision(RevisionModel model, SiteModel site, PostModel post) {
        LocalRevisionModel localRevision = LocalRevisionModel.fromRevisionModel(model, site, post);

        ArrayList<LocalDiffModel> localDiffs = new ArrayList<>();

        for (Diff titleDiff : model.getTitleDiffs()) {
            localDiffs.add(LocalDiffModel.fromDiffAndLocalRevision(
                    titleDiff, LocalDiffType.TITLE, localRevision));
        }

        for (Diff contentDiff : model.getContentDiffs()) {
            localDiffs.add(LocalDiffModel.fromDiffAndLocalRevision(
                    contentDiff, LocalDiffType.CONTENT, localRevision));
        }

        mPostSqlUtils.insertOrUpdateLocalRevision(localRevision, localDiffs);
    }


    public RevisionModel getLocalRevision(SiteModel site, PostModel post) {
        List<LocalRevisionModel> localRevisions = mPostSqlUtils.getLocalRevisions(site, post);

        if (localRevisions.isEmpty()) {
            return null;
        }

        // we currently only support one local revision per post or page
        LocalRevisionModel localRevision = localRevisions.get(0);
        List<LocalDiffModel> localDiffs =
                mPostSqlUtils.getLocalRevisionDiffs(localRevision);

        return RevisionModel.fromLocalRevisionAndDiffs(localRevision, localDiffs);
    }

    public void deleteLocalRevision(RevisionModel revisionModel, SiteModel site, PostModel post) {
        mPostSqlUtils.deleteLocalRevisionAndDiffs(
                LocalRevisionModel.fromRevisionModel(revisionModel, site, post));
    }

    public void deleteLocalRevisionOfAPostOrPage(PostModel post) {
        mPostSqlUtils.deleteLocalRevisionAndDiffsOfAPostOrPage(post);
    }
}
