package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.ListModel;
import org.wordpress.android.fluxc.model.ListModel.ListType;
import org.wordpress.android.fluxc.model.PostListModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.ListSqlUtils;
import org.wordpress.android.fluxc.persistence.PostListSqlUtils;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    public static class FetchPostsPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public ListType listType;
        public boolean loadMore;

        public FetchPostsPayload(SiteModel site, ListType listType) {
            this.site = site;
            this.listType = listType;
        }

        public FetchPostsPayload(SiteModel site, ListType listType, boolean loadMore) {
            this.site = site;
            this.listType = listType;
            this.loadMore = loadMore;
        }
    }

    public static class SearchPostsPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String searchTerm;
        public int offset;

        public SearchPostsPayload(SiteModel site, String searchTerm) {
            this.site = site;
            this.searchTerm = searchTerm;
        }

        public SearchPostsPayload(SiteModel site, String searchTerm, int offset) {
            this(site, searchTerm);
            this.offset = offset;
        }
    }

    public static class SearchPostsResponsePayload extends Payload<PostError> {
        public PostsModel posts;
        public SiteModel site;
        public String searchTerm;
        public boolean isPages;
        public boolean loadedMore;
        public boolean canLoadMore;

        public SearchPostsResponsePayload(PostsModel posts, SiteModel site, String searchTerm, boolean isPages,
                                          boolean loadedMore, boolean canLoadMore) {
            this.posts = posts;
            this.site = site;
            this.searchTerm = searchTerm;
            this.isPages = isPages;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
        }

        public SearchPostsResponsePayload(SiteModel site, String searchTerm, boolean isPages, PostError error) {
            this.site = site;
            this.searchTerm = searchTerm;
            this.isPages = isPages;
            this.error = error;
        }
    }

    public static class FetchPostsResponsePayload extends Payload<PostError> {
        public List<Long> postIds;
        public SiteModel site;
        public ListType listType;
        public boolean isPages;
        public boolean loadedMore;
        public boolean canLoadMore;

        public FetchPostsResponsePayload(List<Long> postIds, SiteModel site, ListType listType, boolean isPages,
                                         boolean loadedMore, boolean canLoadMore) {
            this.postIds = postIds;
            this.site = site;
            this.listType = listType;
            this.isPages = isPages;
            this.loadedMore = loadedMore;
            this.canLoadMore = canLoadMore;
        }

        public FetchPostsResponsePayload(PostError error) {
            this.error = error;
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

    public static class OnSinglePostFetched extends OnChanged<PostError> {
        public int localSiteId;
        public Long remotePostId;

        public OnSinglePostFetched(int localSiteId, Long remotePostId) {
            this.localSiteId = localSiteId;
            this.remotePostId = remotePostId;
        }
    }

    public static class OnPostListChanged extends OnChanged<PostError> {
        public SiteModel site;
        public ListType listType;

        public OnPostListChanged(SiteModel site, ListType listType, @Nullable PostError postError) {
            this.site = site;
            this.listType = listType;
            this.error = postError;
        }
    }

    public static class OnPostUploaded extends OnChanged<PostError> {
        public PostModel post;

        public OnPostUploaded(PostModel post) {
            this.post = post;
        }
    }

    public static class OnPostsSearched extends OnChanged<PostError> {
        public String searchTerm;
        public PostsModel searchResults;
        public boolean canLoadMore;

        public OnPostsSearched(String searchTerm, PostsModel searchResults, boolean canLoadMore) {
            this.searchTerm = searchTerm;
            this.searchResults = searchResults;
            this.canLoadMore = canLoadMore;
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
    private final ListSqlUtils mListSqlUtils;
    private final PostListSqlUtils mPostListSqlUtils;
    // Ensures that the UploadStore is initialized whenever the PostStore is,
    // to ensure actions are shadowed and repeated by the UploadStore
    @SuppressWarnings("unused")
    @Inject UploadStore mUploadStore;

    @Inject
    public PostStore(Dispatcher dispatcher, PostRestClient postRestClient, PostXMLRPCClient postXMLRPCClient,
                     ListSqlUtils listSqlUtils, PostListSqlUtils postListSqlUtils) {
        super(dispatcher);
        mPostRestClient = postRestClient;
        mPostXMLRPCClient = postXMLRPCClient;
        mListSqlUtils = listSqlUtils;
        mPostListSqlUtils = postListSqlUtils;
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
            case SEARCH_POSTS:
                searchPosts((SearchPostsPayload) action.getPayload(), false);
                break;
            case SEARCH_PAGES:
                searchPosts((SearchPostsPayload) action.getPayload(), true);
                break;
            case SEARCHED_POSTS:
                handleSearchPostsCompleted((SearchPostsResponsePayload) action.getPayload());
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

    private void fetchPosts(FetchPostsPayload payload, boolean pages) {
        int offset = 0;
        if (payload.loadMore) {
            offset = PostSqlUtils.getUploadedPostsForSite(payload.site, pages).size();
        }

        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.fetchPosts(payload.site, payload.listType, pages, DEFAULT_POST_STATUS_LIST, offset);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPosts(payload.site, payload.listType, pages, offset);
        }
    }

    private void searchPosts(SearchPostsPayload payload, boolean pages) {
        if (payload.site.isUsingWpComRestApi()) {
            mPostRestClient.searchPosts(payload.site, payload.searchTerm, pages, payload.offset);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            PostError error =
                    new PostError(PostErrorType.UNSUPPORTED_ACTION, "Search only supported on .com/Jetpack sites");
            OnPostsSearched onPostsSearched = new OnPostsSearched(payload.searchTerm, null, false);
            onPostsSearched.error = error;
            emitChange(onPostsSearched);
        }
    }

    private void handleDeletePostCompleted(RemotePostPayload payload) {
        OnPostChanged event = new OnPostChanged(0);
        event.causeOfChange = PostAction.DELETE_POST;

        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PostSqlUtils.deletePost(payload.post);
        }

        emitChange(event);
    }

    public List<PostListModel> getPostList(SiteModel site, ListType listType) {
        ListModel listModel = mListSqlUtils.getList(site.getId(), listType);
        if (listModel != null) {
            return mPostListSqlUtils.getPostList(listModel.getId());
        }
        return Collections.emptyList();
    }

    private void handleFetchPostsCompleted(FetchPostsResponsePayload payload) {
        OnPostListChanged onPostListChanged = new OnPostListChanged(payload.site, payload.listType, payload.error);
        if (!payload.isError()) {
            if (!payload.loadedMore) {
                mListSqlUtils.deleteList(payload.site.getId(), payload.listType);
            }
            mListSqlUtils.insertOrUpdateList(payload.site.getId(), payload.listType);
            ListModel listModel = mListSqlUtils.getList(payload.site.getId(), payload.listType);
            if (listModel != null) { // Sanity check
                List<PostListModel> postList = new ArrayList<>(payload.postIds.size());
                for (Long postId : payload.postIds) {
                    PostListModel postListModel = new PostListModel();
                    postListModel.setListId(listModel.getId());
                    postListModel.setRemotePostId(postId);
                    postList.add(postListModel);
                }
                mPostListSqlUtils.insertPostList(postList);
            }
        }
        emitChange(onPostListChanged);
    }

    private void handleSearchPostsCompleted(SearchPostsResponsePayload payload) {
        OnPostsSearched onPostsSearched = new OnPostsSearched(payload.searchTerm, payload.posts, payload.canLoadMore);

        if (payload.isError()) {
            onPostsSearched.error = payload.error;
        } else if (payload.posts.getPosts() != null && payload.posts.getPosts().size() > 0) {
            for (PostModel post : payload.posts.getPosts()) {
                PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(post);
            }
        }

        emitChange(onPostsSearched);
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
        OnSinglePostFetched onSinglePostFetched =
                new OnSinglePostFetched(post.getLocalSiteId(), post.getRemotePostId());
        emitChange(onSinglePostFetched);
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
}
