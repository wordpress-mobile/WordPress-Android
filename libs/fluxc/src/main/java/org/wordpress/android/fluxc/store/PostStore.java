package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import com.wellsql.generated.PostModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.util.AppLog;

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

    public static class FetchPostsPayload extends Payload {
        public SiteModel site;
        public boolean loadMore;

        public FetchPostsPayload(SiteModel site) {
            this.site = site;
        }

        public FetchPostsPayload(SiteModel site, boolean loadMore) {
            this.site = site;
            this.loadMore = loadMore;
        }
    }

    public static class FetchPostsResponsePayload extends Payload {
        public PostError error;
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

        public FetchPostsResponsePayload(PostError error) {
            this.error = error;
        }

        @Override
        public boolean isError() {
            return error != null;
        }
    }

    public static class RemotePostPayload extends Payload {
        public PostError error;
        public PostModel post;
        public SiteModel site;

        public RemotePostPayload() {}

        public RemotePostPayload(PostModel post, SiteModel site) {
            this.post = post;
            this.site = site;
        }

        @Override
        public boolean isError() {
            return error != null;
        }
    }

    public static class InstantiatePostPayload extends Payload {
        public SiteModel site;
        public boolean isPage;
        public List<Long> categoryIds;
        public String postFormat;

        public InstantiatePostPayload(SiteModel site, boolean isPage) {
            this.site = site;
            this.isPage = isPage;
        }

        /**
         * Used to initialize a post with default category and post format
         */
        public InstantiatePostPayload(SiteModel site, boolean isPage, List<Long> categoryIds, String postFormat) {
            this.site = site;
            this.isPage = isPage;
            this.categoryIds = categoryIds;
            this.postFormat = postFormat;
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
    public class OnPostChanged extends OnChanged<PostError> {
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

    public class OnPostInstantiated extends OnChanged<PostError> {
        public PostModel post;

        public OnPostInstantiated(PostModel post) {
            this.post = post;
        }
    }

    public class OnPostUploaded extends OnChanged<PostError> {
        public PostModel post;

        public OnPostUploaded(PostModel post) {
            this.post = post;
        }
    }

    public enum PostErrorType {
        UNKNOWN_POST,
        UNKNOWN_POST_TYPE,
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
    public PostModel getPostByLocalPostId(long localId) {
        List<PostModel> result = WellSql.select(PostModel.class)
                .where().equals(PostModelTable.ID, localId).endWhere()
                .getAsModel();

        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
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
                handleFetchSinglePostCompleted((RemotePostPayload) action.getPayload());
                break;
            case INSTANTIATE_POST:
                instantiatePost((InstantiatePostPayload) action.getPayload());
                break;
            case PUSH_POST:
                pushPost((RemotePostPayload) action.getPayload());
                break;
            case PUSHED_POST:
                handlePushPostCompleted((RemotePostPayload) action.getPayload());
                break;
            case UPDATE_POST:
                updatePost((PostModel) action.getPayload());
                break;
            case DELETE_POST:
                deletePost((RemotePostPayload) action.getPayload());
                break;
            case DELETED_POST:
                handleDeletePostCompleted((RemotePostPayload) action.getPayload());
                break;
            case REMOVE_POST:
                PostSqlUtils.deletePost((PostModel) action.getPayload());
                break;
        }
    }

    private void deletePost(RemotePostPayload payload) {
        if (payload.site.isWPCom()) {
            mPostRestClient.deletePost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.deletePost(payload.post, payload.site);
        }
    }

    private void fetchPost(RemotePostPayload payload) {
        if (payload.site.isWPCom()) {
            mPostRestClient.fetchPost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPost(payload.post, payload.site);
        }
    }

    private void fetchPosts(FetchPostsPayload payload, boolean pages) {
        int offset = 0;
        if (payload.loadMore) {
            offset = getUploadedPostsCountForSite(payload.site);
        }

        if (payload.site.isWPCom()) {
            mPostRestClient.fetchPosts(payload.site, pages, DEFAULT_POST_STATUS_LIST, offset);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.fetchPosts(payload.site, pages, offset);
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

    private void handleFetchSinglePostCompleted(RemotePostPayload payload) {
        OnPostChanged event;

        if (payload.isError()) {
            event = new OnPostChanged(0);
            event.error = payload.error;
            event.causeOfChange = PostAction.UPDATE_POST;
            emitChange(event);
        } else {
            updatePost(payload.post);
        }
    }

    private void handlePushPostCompleted(RemotePostPayload payload) {
        if (payload.isError()) {
            OnPostUploaded onPostUploaded = new OnPostUploaded(payload.post);
            onPostUploaded.error = payload.error;
            emitChange(onPostUploaded);
        } else {
            emitChange(new OnPostUploaded(payload.post));

            if (payload.site.isWPCom()) {
                // The WP.COM REST API response contains the modified post, so we're already in sync with the server
                // All we need to do is store it and emit OnPostChanged
                updatePost(payload.post);
            } else {
                // XML-RPC does not respond to new/edit post calls with the modified post
                // Update the post locally to reflect its uploaded status, but also request a fresh copy
                // from the server to ensure local copy matches server
                PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(payload.post);
                mPostXMLRPCClient.fetchPost(payload.post, payload.site);
            }
        }
    }

    private void instantiatePost(InstantiatePostPayload payload) {
        PostModel newPost = new PostModel();
        newPost.setLocalSiteId(payload.site.getId());
        newPost.setIsLocalDraft(true);
        newPost.setIsPage(payload.isPage);
        if (payload.categoryIds != null && !payload.categoryIds.isEmpty()) {
            newPost.setCategoryIdList(payload.categoryIds);
        }
        newPost.setPostFormat(payload.postFormat);

        // Insert the post into the db, updating the object to include the local ID
        newPost = PostSqlUtils.insertPostForResult(newPost);

        emitChange(new OnPostInstantiated(newPost));
    }

    private void pushPost(RemotePostPayload payload) {
        if (payload.site.isWPCom()) {
            mPostRestClient.pushPost(payload.post, payload.site);
        } else {
            // TODO: check for WP-REST-API plugin and use it here
            mPostXMLRPCClient.pushPost(payload.post, payload.site);
        }
    }

    private void updatePost(PostModel post) {
        int rowsAffected = PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(post);

        OnPostChanged onPostChanged = new OnPostChanged(rowsAffected);
        onPostChanged.causeOfChange = PostAction.UPDATE_POST;
        emitChange(onPostChanged);
    }
}
