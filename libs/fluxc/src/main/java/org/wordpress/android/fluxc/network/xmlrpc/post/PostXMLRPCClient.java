package org.wordpress.android.fluxc.network.xmlrpc.post;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostXMLRPCClient extends BaseXMLRPCClient {
    public PostXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void fetchPost(final PostModel post, final SiteModel site) {
        fetchPost(post, site, PostAction.FETCH_POST);
    }

    public void fetchPost(final PostModel post, final SiteModel site, final PostAction origin) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_POST, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        if (response != null && response instanceof Map) {
                            PostModel postModel = postResponseObjectToPostModel(response, site, post.isPage());
                            FetchPostResponsePayload payload;
                            if (postModel != null) {
                                if (origin == PostAction.PUSH_POST) {
                                    postModel.setId(post.getId());
                                }
                                payload = new FetchPostResponsePayload(postModel, site);
                            } else {
                                payload = new FetchPostResponsePayload(post, site);
                                payload.error = new PostError(PostErrorType.INVALID_RESPONSE);
                            }
                            payload.origin = origin;

                            mDispatcher.dispatch(PostActionBuilder.newFetchedPostAction(payload));
                        }
                    }
                }, new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 404 - "Invalid post ID."
                        FetchPostResponsePayload payload = new FetchPostResponsePayload(post, site);
                        // TODO: Check the error message and flag this as UNKNOWN_POST if applicable
                        // Convert GenericErrorType to PostErrorType where applicable
                        PostError postError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                postError = new PostError(PostErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                postError = new PostError(PostErrorType.GENERIC_ERROR, error.message);
                        }
                        payload.error = postError;
                        payload.origin = origin;
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostAction(payload));
                    }
                });

        add(request);
    }

    public void fetchPosts(final SiteModel site, final boolean getPages, final int offset) {
        Map<String, Object> contentStruct = new HashMap<>();

        contentStruct.put("number", PostStore.NUM_POSTS_PER_FETCH);
        contentStruct.put("offset", offset);

        if (getPages) {
            contentStruct.put("post_type", "page");
        }

        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(contentStruct);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_POSTS, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        boolean canLoadMore = false;
                        if (response != null && response.length == PostStore.NUM_POSTS_PER_FETCH) {
                            canLoadMore = true;
                        }

                        PostsModel posts = postsResponseToPostsModel(response, site, getPages);

                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(posts, site, getPages,
                                offset > 0, canLoadMore);

                        if (posts != null) {
                            mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                        } else {
                            payload.error = new PostError(PostErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 403 - "The post type specified is not valid"
                        // TODO: Check the error message and flag this as INVALID_POST_TYPE if applicable
                        // Convert GenericErrorType to PostErrorType where applicable
                        PostError postError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                postError = new PostError(PostErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                postError = new PostError(PostErrorType.GENERIC_ERROR, error.message);
                        }
                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(postError);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                }
        );

        add(request);
    }

    public void pushPost(final PostModel post, final SiteModel site) {
        if (TextUtils.isEmpty(post.getStatus())) {
            post.setStatus(PostStatus.PUBLISHED.toString());
        }

        Map<String, Object> contentStruct = postModelToContentStruct(post);

        if (post.isLocalDraft()) {
            // For first time publishing, set the comment status (open or closed) to the default value for the site
            // (respect the existing comment status when editing posts)
            contentStruct.put("comment_status", site.getDefaultCommentStatus());
        }

        List<Object> params = new ArrayList<>(5);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        if (!post.isLocalDraft()) {
            params.add(post.getRemotePostId());
        }
        params.add(contentStruct);

        final XMLRPC method = post.isLocalDraft() ? XMLRPC.NEW_POST : XMLRPC.EDIT_POST;

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params,
                new Listener() {
                    @Override
                    public void onResponse(Object response) {
                        if (method.equals(XMLRPC.NEW_POST) && response instanceof String) {
                            post.setRemotePostId(Long.valueOf((String) response));
                        }
                        post.setIsLocalDraft(false);
                        post.setIsLocallyChanged(false);

                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        mDispatcher.dispatch(PostActionBuilder.newPushedPostAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 403 - "Invalid post type"
                        // 403 - "Invalid term ID" (invalid category or tag id)
                        // 404 - "Invalid post ID." (editing only)
                        // 404 - "Invalid attachment ID." (invalid featured image)
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        // TODO: Check the error message and flag this as one of the above specific errors if applicable
                        // Convert GenericErrorType to PostErrorType where applicable
                        PostError postError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                postError = new PostError(PostErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                postError = new PostError(PostErrorType.GENERIC_ERROR, error.message);
                        }
                        payload.error = postError;
                        mDispatcher.dispatch(PostActionBuilder.newPushedPostAction(payload));
                    }
                });

        request.disableRetries();
        add(request);
    }

    public void deletePost(final PostModel post, final SiteModel site) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_POST, params,
                new Listener() {
                    @Override
                    public void onResponse(Object response) {
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        // Possible non-generic errors:
                        // 404 - "Invalid post ID."
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        // TODO: Check the error message and flag this as UNKNOWN_POST if applicable
                        // Convert GenericErrorType to PostErrorType where applicable
                        PostError postError;
                        switch (error.type) {
                            case AUTHORIZATION_REQUIRED:
                                postError = new PostError(PostErrorType.UNAUTHORIZED, error.message);
                                break;
                            default:
                                postError = new PostError(PostErrorType.GENERIC_ERROR, error.message);
                        }
                        payload.error = postError;
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                });

        request.disableRetries();
        add(request);
    }

    private PostsModel postsResponseToPostsModel(Object[] response, SiteModel site, boolean isPage) {
        List<Map<?, ?>> postsList = new ArrayList<>();
        for (Object responseObject : response) {
            Map<?, ?> postMap = (Map<?, ?>) responseObject;
            postsList.add(postMap);
        }

        List<PostModel> postArray = new ArrayList<>();
        PostModel post;

        for (Object postObject : postsList) {
            post = postResponseObjectToPostModel(postObject, site, isPage);
            if (post != null) {
                postArray.add(post);
            }
        }

        if (postArray.isEmpty()) {
            return null;
        }

        return new PostsModel(postArray);
    }

    private static PostModel postResponseObjectToPostModel(Object postObject, SiteModel site, boolean isPage) {
        // Sanity checks
        if (!(postObject instanceof Map)) {
            return null;
        }

        Map<?, ?> postMap = (Map<?, ?>) postObject;
        PostModel post = new PostModel();

        String postID = MapUtils.getMapStr(postMap, "post_id");
        if (TextUtils.isEmpty(postID)) {
            // If we don't have a post or page ID, move on
            return null;
        }

        post.setLocalSiteId(site.getId());
        post.setRemotePostId(Long.valueOf(postID));
        post.setTitle(MapUtils.getMapStr(postMap, "post_title"));

        Date dateCreatedGmt = MapUtils.getMapDate(postMap, "post_date_gmt");
        String timeAsIso8601 = DateTimeUtils.iso8601UTCFromDate(dateCreatedGmt);
        post.setDateCreated(timeAsIso8601);

        post.setContent(MapUtils.getMapStr(postMap, "post_content"));
        post.setLink(MapUtils.getMapStr(postMap, "link"));

        Object[] terms = (Object[]) postMap.get("terms");
        List<Long> categoryIds = new ArrayList<>();
        List<String> tagNames = new ArrayList<>();
        for (Object term : terms) {
            if (!(term instanceof Map)) {
                continue;
            }
            Map<?, ?> termMap = (Map<?, ?>) term;
            String taxonomy = MapUtils.getMapStr(termMap, "taxonomy");
            if (taxonomy.equals("category")) {
                categoryIds.add(MapUtils.getMapLong(termMap, "term_id"));
            } else if (taxonomy.equals("post_tag")) {
                tagNames.add(MapUtils.getMapStr(termMap, "name"));
            }
        }
        post.setCategoryIdList(categoryIds);
        post.setTagNameList(tagNames);

        Object[] customFields = (Object[]) postMap.get("custom_fields");
        JSONArray jsonCustomFieldsArray = new JSONArray();
        if (customFields != null) {
            PostLocation postLocation = new PostLocation();
            for (Object customField : customFields) {
                jsonCustomFieldsArray.put(customField.toString());
                // Update geo_long and geo_lat from custom fields
                if (!(customField instanceof Map)) {
                    continue;
                }
                Map<?, ?> customFieldMap = (Map<?, ?>) customField;
                if (customFieldMap.get("key") != null && customFieldMap.get("value") != null) {
                    if (customFieldMap.get("key").equals("geo_longitude")) {
                        postLocation.setLongitude(XMLRPCUtils.safeGetMapValue(customFieldMap, 0.0));
                    }
                    if (customFieldMap.get("key").equals("geo_latitude")) {
                        postLocation.setLatitude(XMLRPCUtils.safeGetMapValue(customFieldMap, 0.0));
                    }
                }
            }
            if (postLocation.isValid()) {
                post.setLocation(postLocation);
            }
        }
        post.setCustomFields(jsonCustomFieldsArray.toString());

        post.setExcerpt(MapUtils.getMapStr(postMap, "post_excerpt"));

        post.setPassword(MapUtils.getMapStr(postMap, "post_password"));
        post.setStatus(MapUtils.getMapStr(postMap, "post_status"));

        if (isPage) {
            post.setIsPage(true);
            post.setParentId(MapUtils.getMapLong(postMap, "wp_page_parent_id"));
            post.setParentTitle(MapUtils.getMapStr(postMap, "wp_page_parent"));
            post.setSlug(MapUtils.getMapStr(postMap, "wp_slug"));
        } else {
            // Extract featured image ID from post_thumbnail struct
            Object featuredImageObject = postMap.get("post_thumbnail");
            if (featuredImageObject instanceof Map) {
                Map<?, ?> featuredImageMap = (Map<?, ?>) featuredImageObject;
                post.setFeaturedImageId(MapUtils.getMapInt(featuredImageMap, "attachment_id"));
            }

            post.setPostFormat(MapUtils.getMapStr(postMap, "post_format"));
        }

        return post;
    }

    private static Map<String, Object> postModelToContentStruct(PostModel post) {
        Map<String, Object> contentStruct = new HashMap<>();

        // Post format
        if (!post.isPage()) {
            if (!TextUtils.isEmpty(post.getPostFormat())) {
                contentStruct.put("post_format", post.getPostFormat());
            }
        }

        contentStruct.put("post_type", "post");
        contentStruct.put("post_title", post.getTitle());

        if (post.getDateCreated() != null) {
            String dateCreated = post.getDateCreated();
            Date date = DateTimeUtils.dateUTCFromIso8601(dateCreated);
            if (date != null) {
                contentStruct.put("post_date", date);
                // Redundant, but left in just in case
                // Note: XML-RPC sends the same value for dateCreated and date_created_gmt in the first place
                contentStruct.put("post_date_gmt", date);
            }
        }

        String content = post.getContent();

        // get rid of the p and br tags that the editor adds.
        if (post.isLocalDraft()) {
            content = content.replace("<p>", "").replace("</p>", "\n").replace("<br>", "");
        }

        // gets rid of the weird character android inserts after images
        content = content.replaceAll("\uFFFC", "");

        contentStruct.put("post_content", content);

        if (!post.isPage()) {
            // Handle taxonomies

            // Add categories by ID to the 'terms' param
            Map<Object, Object> terms = new HashMap<>();
            terms.put("category", post.getCategoryIdList().toArray());
            contentStruct.put("terms", terms);

            // Add tags by name to the 'terms_names' param
            Map<Object, Object> termsNames = new HashMap<>();
            termsNames.put("post_tag", post.getTagNameList().toArray());
            contentStruct.put("terms_names", termsNames);
        }

        contentStruct.put("post_excerpt", post.getExcerpt());
        contentStruct.put("post_status", post.getStatus());

        // Geolocation
        if (post.supportsLocation()) {
            JSONObject remoteGeoLatitude = post.getCustomField("geo_latitude");
            JSONObject remoteGeoLongitude = post.getCustomField("geo_longitude");
            JSONObject remoteGeoPublic = post.getCustomField("geo_public");

            Map<Object, Object> hLatitude = new HashMap<>();
            Map<Object, Object> hLongitude = new HashMap<>();
            Map<Object, Object> hPublic = new HashMap<>();

            try {
                if (remoteGeoLatitude != null) {
                    hLatitude.put("id", remoteGeoLatitude.getInt("id"));
                }

                if (remoteGeoLongitude != null) {
                    hLongitude.put("id", remoteGeoLongitude.getInt("id"));
                }

                if (remoteGeoPublic != null) {
                    hPublic.put("id", remoteGeoPublic.getInt("id"));
                }

                if (post.hasLocation()) {
                    PostLocation location = post.getLocation();
                    hLatitude.put("key", "geo_latitude");
                    hLongitude.put("key", "geo_longitude");
                    hPublic.put("key", "geo_public");
                    hLatitude.put("value", location.getLatitude());
                    hLongitude.put("value", location.getLongitude());
                    hPublic.put("value", 1);
                }
            } catch (JSONException e) {
                AppLog.e(T.EDITOR, e);
            }

            if (!hLatitude.isEmpty() && !hLongitude.isEmpty() && !hPublic.isEmpty()) {
                Object[] geo = {hLatitude, hLongitude, hPublic};
                contentStruct.put("custom_fields", geo);
            }
        }

        // Featured images
        if (post.featuredImageHasChanged()) {
            if (post.getFeaturedImageId() < 1 && !post.isLocalDraft()) {
                // The featured image was removed from a live post
                contentStruct.put("post_thumbnail", "");
            } else {
                contentStruct.put("post_thumbnail", post.getFeaturedImageId());
            }
        }

        contentStruct.put("post_password", post.getPassword());

        return contentStruct;
    }
}
