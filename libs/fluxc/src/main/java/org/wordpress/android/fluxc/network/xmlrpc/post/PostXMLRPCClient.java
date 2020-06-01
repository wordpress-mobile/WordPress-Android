package org.wordpress.android.fluxc.network.xmlrpc.post;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.DeletedPostPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostListResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostStatusResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostErrorType;
import org.wordpress.android.fluxc.store.PostStore.PostListItem;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class PostXMLRPCClient extends BaseXMLRPCClient {
    public PostXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent,
                            HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    public void fetchPost(final PostModel post, final SiteModel site) {
        fetchPost(post, site, PostAction.FETCH_POST);
    }

    public void fetchPost(final PostModel post, final SiteModel site, final PostAction origin) {
        List<Object> params = createfetchPostParams(post, site);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_POST, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        if (response instanceof Map) {
                            PostModel postModel = postResponseObjectToPostModel((Map) response, site);
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
                FetchPostResponsePayload payload = new FetchPostResponsePayload(post, site);
                payload.error = createPostErrorFromBaseNetworkError(error);
                payload.origin = origin;
                mDispatcher.dispatch(PostActionBuilder.newFetchedPostAction(payload));
            }
        });

        add(request);
    }

    public void fetchPostStatus(final PostModel post, final SiteModel site) {
        final String postStatusField = "post_status";
        List<Object> params = createfetchPostParams(post, site);
        // If we only request the status, we get an empty response
        params.add(Arrays.asList("post_id", postStatusField));

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_POST, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        String remotePostStatus = null;
                        if (response instanceof Map) {
                            remotePostStatus = MapUtils.getMapStr((Map) response, postStatusField);
                        }
                        FetchPostStatusResponsePayload payload = new FetchPostStatusResponsePayload(post, site);
                        if (remotePostStatus != null) {
                            payload.remotePostStatus = remotePostStatus;
                        } else {
                            payload.error = new PostError(PostErrorType.INVALID_RESPONSE);
                        }
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostStatusAction(payload));
                    }
                }, new BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseNetworkError error) {
                FetchPostStatusResponsePayload payload = new FetchPostStatusResponsePayload(post, site);
                payload.error = createPostErrorFromBaseNetworkError(error);
                mDispatcher.dispatch(PostActionBuilder.newFetchedPostStatusAction(payload));
            }
        });

        add(request);
    }

    public void fetchPostList(final PostListDescriptorForXmlRpcSite listDescriptor, final long offset) {
        SiteModel site = listDescriptor.getSite();
        List<String> fields = Arrays.asList("post_id", "post_modified_gmt", "post_status");
        final int pageSize = listDescriptor.getConfig().getNetworkPageSize();
        List<Object> params =
                createFetchPostListParameters(site.getSelfHostedSiteId(), site.getUsername(), site.getPassword(), false,
                        offset, pageSize, listDescriptor.getStatusList(), fields,
                        listDescriptor.getOrderBy().getValue(), listDescriptor.getOrder().getValue());
        final boolean loadedMore = offset > 0;

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_POSTS, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        boolean canLoadMore =
                                response != null && response.length == pageSize;
                        List<PostListItem> postListItems = postListItemsFromPostsResponse(response);
                        PostError postError = response == null ? new PostError(PostErrorType.INVALID_RESPONSE) : null;
                        FetchPostListResponsePayload responsePayload =
                                new FetchPostListResponsePayload(listDescriptor, postListItems, loadedMore,
                                        canLoadMore, postError);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostListAction(responsePayload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        PostError postError = createPostErrorFromBaseNetworkError(error);
                        FetchPostListResponsePayload responsePayload =
                                new FetchPostListResponsePayload(listDescriptor, Collections.<PostListItem>emptyList(),
                                        loadedMore, false, postError);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostListAction(responsePayload));
                    }
                });

        add(request);
    }

    public void fetchPosts(final SiteModel site, final boolean getPages, List<PostStatus> statusList,
                           final int offset) {
        List<Object> params =
                createFetchPostListParameters(site.getSelfHostedSiteId(), site.getUsername(), site.getPassword(),
                        getPages, offset, PostStore.NUM_POSTS_PER_FETCH, statusList, null, null, null);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_POSTS, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        boolean canLoadMore = false;
                        if (response != null && response.length == PostStore.NUM_POSTS_PER_FETCH) {
                            canLoadMore = true;
                        }

                        PostsModel posts = postsResponseToPostsModel(response, site);

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
                        PostError postError = createPostErrorFromBaseNetworkError(error);
                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(postError, getPages);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                });

        add(request);
    }

    public void pushPost(final PostModel post, final SiteModel site) {
        pushPostInternal(post, site, false);
    }

    public void restorePost(final PostModel post, final SiteModel site) {
        pushPostInternal(post, site, true);
    }

    private void pushPostInternal(final PostModel post, final SiteModel site, final boolean isRestoringPost) {
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
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        if (method.equals(XMLRPC.NEW_POST) && response instanceof String) {
                            post.setRemotePostId(Long.valueOf((String) response));
                        }
                        post.setIsLocalDraft(false);
                        post.setIsLocallyChanged(false);

                        RemotePostPayload payload = new RemotePostPayload(post, site);

                        Action resultAction = isRestoringPost ? PostActionBuilder.newRestoredPostAction(payload)
                                : UploadActionBuilder.newPushedPostAction(payload);
                        mDispatcher.dispatch(resultAction);
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        payload.error = createPostErrorFromBaseNetworkError(error);
                        Action resultAction = isRestoringPost ? PostActionBuilder.newRestoredPostAction(payload)
                                : UploadActionBuilder.newPushedPostAction(payload);
                        mDispatcher.dispatch(resultAction);
                    }
                });

        request.disableRetries();
        add(request);
    }

    public void deletePost(final @NonNull PostModel post, final @NonNull SiteModel site,
                           final @NonNull PostDeleteActionType postDeleteActionType) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.DELETE_POST, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        // XML-RPC response doesn't contain the deleted post object
                        DeletedPostPayload payload =
                                new DeletedPostPayload(post, site, postDeleteActionType, (PostModel) null);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        PostError deletePostError = createPostErrorFromBaseNetworkError(error);
                        DeletedPostPayload payload =
                                new DeletedPostPayload(post, site, postDeleteActionType, deletePostError);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                });

        request.disableRetries();
        add(request);
    }

    private @NotNull List<PostListItem> postListItemsFromPostsResponse(@Nullable Object[] response) {
        if (response == null) {
            return Collections.emptyList();
        }
        List<PostListItem> postListItems = new ArrayList<>();
        for (Object responseObject : response) {
            Map<?, ?> postMap = (Map<?, ?>) responseObject;
            String postID = MapUtils.getMapStr(postMap, "post_id");
            String postStatus = MapUtils.getMapStr(postMap, "post_status");
            Date lastModifiedGmt = MapUtils.getMapDate(postMap, "post_modified_gmt");
            String lastModifiedAsIso8601 = DateTimeUtils.iso8601UTCFromDate(lastModifiedGmt);

            postListItems.add(new PostListItem(Long.parseLong(postID), lastModifiedAsIso8601, postStatus, null));
        }
        return postListItems;
    }

    private PostsModel postsResponseToPostsModel(@Nullable Object[] response, SiteModel site) {
        List<PostModel> postArray = new ArrayList<>();
        if (response == null) {
            return null;
        }
        if (response.length == 0) {
            return new PostsModel(postArray);
        }
        for (Object responseObject : response) {
            Map<?, ?> postMap = (Map<?, ?>) responseObject;
            PostModel post = postResponseObjectToPostModel(postMap, site);
            if (post != null) {
                postArray.add(post);
            }
        }

        if (postArray.isEmpty()) {
            return null;
        }

        return new PostsModel(postArray);
    }

    private static PostModel postResponseObjectToPostModel(@NonNull Map postObject, SiteModel site) {
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
        String dateCreatedAsIso8601 = DateTimeUtils.iso8601UTCFromDate(dateCreatedGmt);
        post.setDateCreated(dateCreatedAsIso8601);

        Date lastModifiedGmt = MapUtils.getMapDate(postMap, "post_modified_gmt");
        String lastModifiedAsIso8601 = DateTimeUtils.iso8601UTCFromDate(lastModifiedGmt);
        post.setLastModified(lastModifiedAsIso8601);
        post.setRemoteLastModified(lastModifiedAsIso8601);

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
            Double latitude = null;
            Double longitude = null;
            for (Object customField : customFields) {
                jsonCustomFieldsArray.put(customField.toString());
                // Update geo_long and geo_lat from custom fields
                if (!(customField instanceof Map)) {
                    continue;
                }
                Map<?, ?> customFieldMap = (Map<?, ?>) customField;
                Object key = customFieldMap.get("key");
                if (key != null && customFieldMap.get("value") != null) {
                    if (key.equals("geo_longitude")) {
                        longitude = XMLRPCUtils.safeGetMapValue(customFieldMap, 0.0);
                    }
                    if (key.equals("geo_latitude")) {
                        latitude = XMLRPCUtils.safeGetMapValue(customFieldMap, 0.0);
                    }
                }
            }
            if (latitude != null && longitude != null) {
                PostLocation postLocation = new PostLocation(latitude, longitude);
                if (postLocation.isValid()) {
                    post.setLocation(postLocation);
                }
            }
        }
        post.setCustomFields(jsonCustomFieldsArray.toString());

        post.setExcerpt(MapUtils.getMapStr(postMap, "post_excerpt"));
        post.setSlug(MapUtils.getMapStr(postMap, "post_name"));

        post.setPassword(MapUtils.getMapStr(postMap, "post_password"));
        post.setStatus(MapUtils.getMapStr(postMap, "post_status"));

        if ("page".equals(MapUtils.getMapStr(postMap, "post_type"))) {
            post.setIsPage(true);
        }

        if (post.isPage()) {
            post.setParentId(MapUtils.getMapLong(postMap, "post_parent"));
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
        } else {
            contentStruct.put("post_parent", post.getParentId());
        }

        contentStruct.put("post_type", post.isPage() ? "page" : "post");
        contentStruct.put("post_title", post.getTitle());

        String dateCreated = post.getDateCreated();
        Date date = DateTimeUtils.dateUTCFromIso8601(dateCreated);
        if (date != null) {
            contentStruct.put("post_date", date);
            // Redundant, but left in just in case
            // Note: XML-RPC sends the same value for dateCreated and date_created_gmt in the first place
            contentStruct.put("post_date_gmt", date);
        }

        // We are not adding `lastModified` date to the params because that should be updated by the server when there
        // is a change in the post. This is tested for on 08/21/2018 and verified that it's working as expected.
        // I am only adding this note here to avoid a possible confusion about it in the future.

        String content = post.getContent();

        // gets rid of the weird character android inserts after images
        content = content.replaceAll("\uFFFC", "");

        contentStruct.put("post_content", content);

        if (!post.isPage()) {
            // Handle taxonomies

            if (post.isLocalDraft()) {
                // When first time publishing, we only want to send the category and tag arrays if they contain info
                // For tags it doesn't matter if we send an empty array, but for categories we want WordPress to give
                // the post the site's default category, and that won't happen if we send an empty category array
                // (we should send nothing instead)
                if (!post.getCategoryIdList().isEmpty()) {
                    // Add categories by ID to the 'terms' param
                    Map<Object, Object> terms = new HashMap<>();
                    terms.put("category", post.getCategoryIdList().toArray());
                    contentStruct.put("terms", terms);
                }

                if (!post.getTagNameList().isEmpty()) {
                    // Add tags by name to the 'terms_names' param
                    Map<Object, Object> termsNames = new HashMap<>();
                    termsNames.put("post_tag", post.getTagNameList().toArray());
                    contentStruct.put("terms_names", termsNames);
                }
            } else {
                // When editing existing posts, we want to explicitly tell the server that tags or categories are now
                // empty, as it might be because the user removed them from the post

                // Add categories by ID to the 'terms' param
                Map<Object, Object> terms = new HashMap<>();
                if (post.getCategoryIdList().size() > 0 || !post.isLocalDraft()) {
                    terms.put("category", post.getCategoryIdList().toArray());
                }

                if (!post.getTagNameList().isEmpty()) {
                    // Add tags by name to the 'terms_names' param
                    Map<Object, Object> termsNames = new HashMap<>();
                    termsNames.put("post_tag", post.getTagNameList().toArray());
                    contentStruct.put("terms_names", termsNames);
                } else {
                    // To clear any existing tags, we must pass an empty 'post_tag' array in the 'terms' param
                    // (this won't work in the 'terms_names' param)
                    terms.put("post_tag", post.getTagNameList().toArray());
                }

                contentStruct.put("terms", terms);
            }
        }

        contentStruct.put("post_excerpt", post.getExcerpt());
        contentStruct.put("post_name", post.getSlug());
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

        contentStruct.put("post_thumbnail", post.getFeaturedImageId());

        contentStruct.put("post_password", post.getPassword());

        return contentStruct;
    }

    private List<Object> createFetchPostListParameters(
            final Long selfHostedSiteId,
            final String username,
            final String password,
            final boolean getPages,
            final long offset,
            final int number,
            @Nullable final List<PostStatus> statusList,
            @Nullable final List<String> fields,
            @Nullable final String orderBy,
            @Nullable final String order) {
        Map<String, Object> contentStruct = new HashMap<>();
        contentStruct.put("number", number);
        contentStruct.put("offset", offset);
        if (!TextUtils.isEmpty(orderBy)) {
            contentStruct.put("orderby", orderBy);
        }
        if (!TextUtils.isEmpty(order)) {
            contentStruct.put("order", order);
        }
        if (statusList != null && statusList.size() > 0) {
            contentStruct.put("post_status", PostStatus.postStatusListToString(statusList));
        }

        if (getPages) {
            contentStruct.put("post_type", "page");
        }

        List<Object> params = new ArrayList<>(4);
        params.add(selfHostedSiteId);
        params.add(username);
        params.add(password);
        params.add(contentStruct);
        if (fields != null && fields.size() > 0) {
            params.add(fields);
        }
        return params;
    }

    private List<Object> createfetchPostParams(final PostModel post, final SiteModel site) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());
        return params;
    }

    private PostError createPostErrorFromBaseNetworkError(@NonNull BaseNetworkError error) {
        // Possible non-generic errors:
        // 403 - "Invalid post type"
        // 403 - "Invalid term ID" (invalid category or tag id)
        // 404 - "Invalid post ID." (editing only)
        // 404 - "Invalid attachment ID." (invalid featured image)
        // TODO: Check the error message and flag this as UNKNOWN_POST if applicable
        // Convert GenericErrorType to PostErrorType where applicable
        switch (error.type) {
            case AUTHORIZATION_REQUIRED:
                return new PostError(PostErrorType.UNAUTHORIZED, error.message);
            default:
                return new PostError(PostErrorType.GENERIC_ERROR, error.message);
        }
    }
}
