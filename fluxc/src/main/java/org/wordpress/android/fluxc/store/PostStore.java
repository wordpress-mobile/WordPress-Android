package org.wordpress.android.fluxc.store;

import android.database.Cursor;

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
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostStore extends Store {
    public static final int NUM_POSTS_PER_FETCH = 20;

    public static class FetchPostsPayload implements Payload {
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

    public static class FetchPostsResponsePayload implements Payload {
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
    }

    public static class RemotePostPayload implements Payload {
        public PostModel post;
        public SiteModel site;

        public RemotePostPayload(PostModel post, SiteModel site) {
            this.post = post;
            this.site = site;
        }
    }

    public static class InstantiatePostPayload implements Payload {
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

    // OnChanged events
    public class OnPostChanged extends OnChanged {
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

    public class OnPostInstantiated extends OnChanged {
        public PostModel post;

        public OnPostInstantiated(PostModel post) {
            this.post = post;
        }
    }

    public class OnPostUploaded extends OnChanged {
        public PostModel post;

        public OnPostUploaded(PostModel post) {
            this.post = post;
        }
    }

    private PostRestClient mPostRestClient;
    private PostXMLRPCClient mPostXMLRPCClient;

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
     * Returns all posts in the store as a {@link PostModel} list.
     */
    public List<PostModel> getPosts() {
        return WellSql.select(PostModel.class).getAsModel();
    }

    /**
     * Returns all posts in the store as a {@link Cursor}.
     */
    public Cursor getPostsCursor() {
        return WellSql.select(PostModel.class).getAsCursor();
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getPostsCount() {
        return getPostsCursor().getCount();
    }

    /**
     * Returns all posts in the store as a {@link PostModel} list.
     */
    public List<PostModel> getPostsForSite(SiteModel site) {
        return PostSqlUtils.getPostsForSite(site, false);
    }

    /**
     * Returns all posts in the store as a {@link PostModel} list.
     */
    public List<PostModel> getPagesForSite(SiteModel site) {
        return PostSqlUtils.getPostsForSite(site, true);
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getPostsCountForSite(SiteModel site) {
        return getPostsForSite(site).size();
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getPagesCountForSite(SiteModel site) {
        return getPagesForSite(site).size();
    }

    /**
     * Returns the number of posts in the store.
     */
    public List<PostModel> getUploadedPostsForSite(SiteModel site) {
        return PostSqlUtils.getUploadedPostsForSite(site, false);
    }

    /**
     * Returns the number of posts in the store.
     */
    public List<PostModel> getUploadedPagesForSite(SiteModel site) {
        return PostSqlUtils.getUploadedPostsForSite(site, true);
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getUploadedPostsCountForSite(SiteModel site) {
        return getUploadedPostsForSite(site).size();
    }

    /**
     * Returns the number of posts in the store.
     */
    public int getUploadedPagesCountForSite(SiteModel site) {
        return getUploadedPagesForSite(site).size();
    }

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
        if (actionType == PostAction.FETCH_POSTS) {
            FetchPostsPayload payload = (FetchPostsPayload) action.getPayload();

            int offset = 0;
            if (payload.loadMore) {
                offset = getUploadedPostsCountForSite(payload.site);
            }

            if (payload.site.isWPCom()) {
                mPostRestClient.fetchPosts(payload.site, false, offset);
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                mPostXMLRPCClient.fetchPosts(payload.site, false, offset);
            }
        } else if (actionType == PostAction.FETCH_PAGES) {
            FetchPostsPayload payload = (FetchPostsPayload) action.getPayload();

            int offset = 0;
            if (payload.loadMore) {
                offset = getUploadedPostsCountForSite(payload.site);
            }

            if (payload.site.isWPCom()) {
                mPostRestClient.fetchPosts(payload.site, true, offset);
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                mPostXMLRPCClient.fetchPosts(payload.site, true, offset);
            }
        } else if (actionType == PostAction.FETCHED_POSTS) {
            FetchPostsResponsePayload postsResponsePayload = (FetchPostsResponsePayload) action.getPayload();

            // Clear existing uploading posts if this is a fresh fetch (loadMore = false in the original request)
            // This is the simplest way of keeping our local posts in sync with remote posts (in case of deletions,
            // or if the user manual changed some post IDs)
            if (!postsResponsePayload.loadedMore) {
                PostSqlUtils.deleteUploadedPostsForSite(postsResponsePayload.site, postsResponsePayload.isPages);
            }

            int rowsAffected = 0;
            for (PostModel post : postsResponsePayload.posts) {
                rowsAffected += PostSqlUtils.insertOrUpdatePostKeepingLocalChanges(post);
            }

            OnPostChanged onPostChanged = new OnPostChanged(rowsAffected, postsResponsePayload.canLoadMore);
            if (postsResponsePayload.isPages) {
                onPostChanged.causeOfChange = PostAction.FETCH_PAGES;
            } else {
                onPostChanged.causeOfChange = PostAction.FETCH_POSTS;
            }

            emitChange(onPostChanged);
        } else if (actionType == PostAction.FETCH_POST) {
            RemotePostPayload payload = (RemotePostPayload) action.getPayload();
            if (payload.site.isWPCom()) {
                mPostRestClient.fetchPost(payload.post, payload.site);
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                mPostXMLRPCClient.fetchPost(payload.post, payload.site);
            }
        } else if (actionType == PostAction.INSTANTIATE_POST) {
            InstantiatePostPayload payload = (InstantiatePostPayload) action.getPayload();

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
        } else if (actionType == PostAction.PUSH_POST) {
            RemotePostPayload payload = (RemotePostPayload) action.getPayload();
            if (payload.site.isWPCom()) {
                mPostRestClient.pushPost(payload.post, payload.site);
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                mPostXMLRPCClient.pushPost(payload.post, payload.site);
            }
            // TODO: Should call UPDATE_POST at this point, probably
        } else if (actionType == PostAction.PUSHED_POST) {
            RemotePostPayload payload = (RemotePostPayload) action.getPayload();
            int rowsAffected = PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges(payload.post);

            emitChange(new OnPostUploaded(payload.post));

            if (payload.site.isWPCom()) {
                // The WP.COM REST API response contains the modified post, so we're already in sync with the server
                OnPostChanged onPostChanged = new OnPostChanged(rowsAffected);
                onPostChanged.causeOfChange = PostAction.UPDATE_POST;
                emitChange(onPostChanged);
            } else {
                // XML-RPC does not respond to new/edit post calls with the modified post
                // Request a fresh copy of the uploaded post from the server to ensure local copy matches server
                mPostXMLRPCClient.fetchPost(payload.post, payload.site);
            }
        } else if (actionType == PostAction.UPDATE_POST) {
            int rowsAffected = PostSqlUtils.insertOrUpdatePostOverwritingLocalChanges((PostModel) action.getPayload());

            OnPostChanged onPostChanged = new OnPostChanged(rowsAffected);
            onPostChanged.causeOfChange = PostAction.UPDATE_POST;
            emitChange(onPostChanged);
        } else if (actionType == PostAction.DELETE_POST) {
            RemotePostPayload payload = (RemotePostPayload) action.getPayload();
            if (payload.site.isWPCom()) {
                mPostRestClient.deletePost(payload.post, payload.site);
            } else {
                // TODO: check for WP-REST-API plugin and use it here
                mPostXMLRPCClient.deletePost(payload.post, payload.site);
            }
        } else if (actionType == PostAction.DELETED_POST) {
            // Handle any necessary changes to post status in the db here
            OnPostChanged onPostChanged = new OnPostChanged(0);
            onPostChanged.causeOfChange = PostAction.DELETE_POST;
            emitChange(onPostChanged);
        } else if (actionType == PostAction.REMOVE_POST) {
            PostSqlUtils.deletePost((PostModel) action.getPayload());
        }
    }
}
