package org.wordpress.android.fluxc.network.rest.wpcom.post;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.ListItemModel;
import org.wordpress.android.fluxc.model.ListModel.ListType;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostsResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.store.PostStore.SearchPostsResponsePayload;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class PostRestClient extends BaseWPComRestClient {
    public PostRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                          UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchPost(final PostModel post, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).getUrlV1_1();

        Map<String, String> params = new HashMap<>();

        params.put("context", "edit");

        final WPComGsonRequest<PostWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel fetchedPost = postResponseToPostModel(response);
                        fetchedPost.setId(post.getId());
                        fetchedPost.setLocalSiteId(site.getId());

                        FetchPostResponsePayload payload = new FetchPostResponsePayload(fetchedPost, site);
                        payload.post = fetchedPost;

                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post (invalid post ID)
                        FetchPostResponsePayload payload = new FetchPostResponsePayload(post, site);
                        payload.error = new PostError(error.apiError, error.message);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchPosts(final SiteModel site, final ListType listType, final boolean getPages,
                           final List<PostStatus> statusList, final int offset) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.getUrlV1_1();

        Map<String, String> params = new HashMap<>();

        params.put("context", "edit");
        params.put("number", String.valueOf(PostStore.NUM_POSTS_PER_FETCH));
        params.put("fields", "ID,modified");

        if (getPages) {
            params.put("type", "page");
        }

        if (statusList.size() > 0) {
            params.put("status", PostStatus.postStatusListToString(statusList));
        }

        if (offset > 0) {
            params.put("offset", String.valueOf(offset));
        }

        final WPComGsonRequest<PostsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PostsResponse.class,
                new Listener<PostsResponse>() {
                    @Override
                    public void onResponse(PostsResponse response) {
                        List<ListItemModel> listItems = new ArrayList<>();
                        for (PostWPComRestResponse postResponse : response.posts) {
                            ListItemModel listItemModel = new ListItemModel();
                            listItemModel.setRemoteItemId(postResponse.ID);
                            listItemModel.setLastModified(postResponse.modified);
                            listItems.add(listItemModel);
                        }

                        boolean canLoadMore = listItems.size() == PostStore.NUM_POSTS_PER_FETCH;

                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(listItems,
                                site, listType, getPages, offset > 0, canLoadMore);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post_type (invalid post type, shouldn't happen)
                        PostError postError = new PostError(error.apiError, error.message);
                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(postError);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                }
        );
        add(request);
    }

    public void pushPost(final PostModel post, final SiteModel site) {
        String url;

        if (post.isLocalDraft()) {
            url = WPCOMREST.sites.site(site.getSiteId()).posts.new_.getUrlV1_1();
        } else {
            url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).getUrlV1_1();
        }

        Map<String, Object> body = postModelToParams(post);

        final WPComGsonRequest<PostWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel uploadedPost = postResponseToPostModel(response);

                        uploadedPost.setIsLocalDraft(false);
                        uploadedPost.setIsLocallyChanged(false);
                        uploadedPost.setId(post.getId());
                        uploadedPost.setLocalSiteId(site.getId());

                        RemotePostPayload payload = new RemotePostPayload(uploadedPost, site);
                        mDispatcher.dispatch(UploadActionBuilder.newPushedPostAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post (invalid post ID)
                        // Note: Unlike XML-RPC, if an invalid term (category or tag) ID is specified, the server just
                        // ignores it and creates/updates the post normally
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        payload.error = new PostError(error.apiError, error.message);
                        mDispatcher.dispatch(UploadActionBuilder.newPushedPostAction(payload));
                    }
                }
        );

        request.addQueryParameter("context", "edit");

        request.disableRetries();
        add(request);
    }

    public void deletePost(final PostModel post, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).delete.getUrlV1_1();

        final WPComGsonRequest<PostWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel deletedPost = postResponseToPostModel(response);
                        deletedPost.setId(post.getId());
                        deletedPost.setLocalSiteId(post.getLocalSiteId());

                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post (invalid post ID)
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        payload.error = new PostError(error.apiError, error.message);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                }
        );

        request.addQueryParameter("context", "edit");

        request.disableRetries();
        add(request);
    }

    public void searchPosts(final SiteModel site, final String searchTerm, final boolean pages, final int offset) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.getUrlV1_1();

        Map<String, String> params = new HashMap<>();

        if (pages) {
            params.put("type", "page");
        }
        params.put("number", String.valueOf(PostStore.NUM_POSTS_PER_FETCH));
        params.put("offset", String.valueOf(offset));
        params.put("search", searchTerm);
        params.put("status", "any");

        final WPComGsonRequest<PostsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PostsResponse.class,
                new Listener<PostsResponse>() {
                    @Override
                    public void onResponse(PostsResponse response) {
                        List<PostModel> postList = new ArrayList<>();
                        PostModel post;
                        for (PostWPComRestResponse postResponse : response.posts) {
                            post = postResponseToPostModel(postResponse);
                            post.setLocalSiteId(site.getId());
                            postList.add(post);
                        }

                        boolean loadedMore = offset > 0;
                        boolean canLoadMore = postList.size() == PostStore.NUM_POSTS_PER_FETCH;

                        SearchPostsResponsePayload payload = new SearchPostsResponsePayload(
                                postList, site, searchTerm, pages, loadedMore, canLoadMore);
                        mDispatcher.dispatch(PostActionBuilder.newSearchedPostsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        PostError postError = new PostError(error.apiError, error.message);
                        SearchPostsResponsePayload payload =
                                new SearchPostsResponsePayload(site, searchTerm, pages, postError);
                        mDispatcher.dispatch(PostActionBuilder.newSearchedPostsAction(payload));
                    }
                }
        );

        add(request);
    }

    private PostModel postResponseToPostModel(PostWPComRestResponse from) {
        PostModel post = new PostModel();
        post.setRemotePostId(from.ID);
        post.setRemoteSiteId(from.site_ID);
        post.setLink(from.URL); // Is this right?
        post.setDateCreated(from.date);
        post.setLastModified(from.modified);
        post.setTitle(from.title);
        post.setContent(from.content);
        post.setExcerpt(from.excerpt);
        post.setSlug(from.slug);
        post.setStatus(from.status);
        post.setPassword(from.password);
        post.setIsPage(from.type.equals("page"));

        if (from.post_thumbnail != null) {
            post.setFeaturedImageId(from.post_thumbnail.ID);
        }
        post.setPostFormat(from.format);
        if (from.geo != null) {
            post.setLatitude(from.geo.latitude);
            post.setLongitude(from.geo.longitude);
        }

        if (from.categories != null) {
            List<Long> categoryIds = new ArrayList<>();
            for (TermWPComRestResponse value : from.categories.values()) {
                categoryIds.add(value.ID);
            }
            post.setCategoryIdList(categoryIds);
        }

        if (from.tags != null) {
            List<String> tagNames = new ArrayList<>();
            for (TermWPComRestResponse value : from.tags.values()) {
                tagNames.add(value.name);
            }
            post.setTagNameList(tagNames);
        }

        if (from.capabilities != null) {
            post.setHasCapabilityPublishPost(from.capabilities.publish_post);
            post.setHasCapabilityEditPost(from.capabilities.edit_post);
            post.setHasCapabilityDeletePost(from.capabilities.delete_post);
        }

        if (from.parent != null) {
            post.setParentId(from.parent.ID);
            post.setParentTitle(from.parent.title);
        }

        return post;
    }

    private Map<String, Object> postModelToParams(PostModel post) {
        Map<String, Object> params = new HashMap<>();

        params.put("status", StringUtils.notNullStr(post.getStatus()));
        params.put("title", StringUtils.notNullStr(post.getTitle()));
        params.put("content", StringUtils.notNullStr(post.getContent()));
        params.put("excerpt", StringUtils.notNullStr(post.getExcerpt()));
        params.put("slug", StringUtils.notNullStr(post.getSlug()));

        if (!TextUtils.isEmpty(post.getDateCreated())) {
            params.put("date", post.getDateCreated());
        }

        // We are not adding `lastModified` date to the params because that should be updated by the server when there
        // is a change in the post. This is tested for both Calypso and WPAndroid on 08/21/2018 and verified that it's
        // working as expected. I am only adding this note here to avoid a possible confusion about it in the future.

        if (!post.isPage()) {
            if (!TextUtils.isEmpty(post.getPostFormat())) {
                params.put("format", post.getPostFormat());
            }
        } else {
            params.put("type", "page");
        }

        params.put("password", StringUtils.notNullStr(post.getPassword()));

        params.put("categories", TextUtils.join(",", post.getCategoryIdList()));
        params.put("tags", TextUtils.join(",", post.getTagNameList()));

        if (post.hasFeaturedImage()) {
            params.put("featured_image", post.getFeaturedImageId());
        } else {
            params.put("featured_image", "");
        }

        if (post.hasLocation()) {
            // Location data was added to the post
            List<Map<String, Object>> metadata = new ArrayList<>();
            PostLocation location = post.getLocation();

            Map<String, Object> latitudeParams = new HashMap<>();
            latitudeParams.put("key", "geo_latitude");
            latitudeParams.put("value", location.getLatitude());
            latitudeParams.put("operation", "update");

            Map<String, Object> longitudeParams = new HashMap<>();
            longitudeParams.put("key", "geo_longitude");
            longitudeParams.put("value", location.getLongitude());
            latitudeParams.put("operation", "update");

            metadata.add(latitudeParams);
            metadata.add(longitudeParams);
            params.put("metadata", metadata);
        } else if (post.shouldDeleteLatitude() || post.shouldDeleteLongitude()) {
            // The post used to have location data, but the user deleted it - clear location data on the server
            List<Map<String, Object>> metadata = new ArrayList<>();

            if (post.shouldDeleteLatitude()) {
                Map<String, Object> latitudeParams = new HashMap<>();
                latitudeParams.put("key", "geo_latitude");
                latitudeParams.put("operation", "delete");
                metadata.add(latitudeParams);
            }

            if (post.shouldDeleteLongitude()) {
                Map<String, Object> longitudeParams = new HashMap<>();
                longitudeParams.put("key", "geo_longitude");
                longitudeParams.put("operation", "delete");
                metadata.add(longitudeParams);
            }

            params.put("metadata", metadata);
        }

        return params;
    }
}
