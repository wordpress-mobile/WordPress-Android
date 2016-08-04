package org.wordpress.android.fluxc.network.xmlrpc.post;

import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostLocation;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostStatus;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.fluxc.utils.DateTimeUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostXMLRPCClient extends BaseXMLRPCClient {
    public static final int NUM_POSTS_TO_REQUEST = 20;

    public PostXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void fetchPost(final PostModel post, final SiteModel site) {
        List<Object> params;

        if (post.isPage()) {
            params = new ArrayList<>(4);
            params.add(site.getDotOrgSiteId());
            params.add(post.getRemotePostId());
            params.add(site.getUsername());
            params.add(site.getPassword());
        } else {
            params = new ArrayList<>(3);
            params.add(post.getRemotePostId());
            params.add(site.getUsername());
            params.add(site.getPassword());
        }

        params.add(site.getDotOrgSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());

        XMLRPC method = (post.isPage() ? XMLRPC.GET_PAGE : XMLRPC.GET_POST);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        if (response != null && response instanceof Map) {
                            PostModel postModel = postResponseObjectToPostModel(response, site, post.isPage());
                            if (postModel != null) {
                                mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(postModel));
                            } else {
                                // TODO: do nothing or dispatch error?
                            }
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Implement lower-level catching in BaseXMLRPCClient
                    }
                });

        add(request);
    }

    public void fetchPosts(final SiteModel site, final boolean getPages, final int offset) {
        int numPostsToRequest = offset + NUM_POSTS_TO_REQUEST;

        List<Object> params = new ArrayList<>(4);
        params.add(site.getDotOrgSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(numPostsToRequest);

        XMLRPC method = (getPages ? XMLRPC.GET_PAGES : XMLRPC.GET_POSTS);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        boolean canLoadMore;
                        int startPosition = 0;
                        if (response != null && response.length > 0) {
                            canLoadMore = true;

                            // If we're loading more posts, only save the posts at the end of the array.
                            // NOTE: Switching to wp.getPosts wouldn't require janky solutions like this
                            // since it allows for an offset parameter.
                            if (offset > 0 && response.length > NUM_POSTS_TO_REQUEST) {
                                startPosition = response.length - NUM_POSTS_TO_REQUEST;
                            }
                        } else {
                            canLoadMore = false;
                        }

                        PostsModel posts = postsResponseToPostsModel(response, site, getPages, startPosition);

                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(posts, site, getPages,
                                offset > 0, canLoadMore);

                        if (posts != null) {
                            mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                        } else {
                            // TODO: do nothing or dispatch error?
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Implement lower-level catching in BaseXMLRPCClient
                    }
                }
        );

        add(request);
    }

    public void pushPost(final PostModel post, final SiteModel site) {
        if (TextUtils.isEmpty(post.getStatus())) {
            post.setStatus(PostStatus.toString(PostStatus.PUBLISHED));
        }

        String descriptionContent = post.getDescription();
        String moreContent = post.getMoreText();

        JSONArray categoriesJsonArray = post.getJSONCategories();
        String[] postCategories = null;
        if (categoriesJsonArray != null) {
            postCategories = new String[categoriesJsonArray.length()];
            for (int i = 0; i < categoriesJsonArray.length(); i++) {
                try {
                    postCategories[i] = TextUtils.htmlEncode(categoriesJsonArray.getString(i));
                } catch (JSONException e) {
                    AppLog.e(T.POSTS, e);
                }
            }
        }

        Map<String, Object> contentStruct = new HashMap<>();

        // Post format
        if (!post.isPage()) {
            if (!TextUtils.isEmpty(post.getPostFormat())) {
                contentStruct.put("wp_post_format", post.getPostFormat());
            }
        }

        contentStruct.put("post_type", (post.isPage()) ? "page" : "post");
        contentStruct.put("title", post.getTitle());

        if (post.getDateCreated() != null) {
            String dateCreated = post.getDateCreated();
            Date date = DateTimeUtils.dateFromIso8601(dateCreated);
            if (date != null) {
                contentStruct.put("dateCreated", date);
                // Redundant, but left in just in case
                // Note: XML-RPC sends the same value for dateCreated and date_created_gmt in the first place
                contentStruct.put("date_created_gmt", date);
            }
        }

        if (!TextUtils.isEmpty(moreContent)) {
            descriptionContent = descriptionContent.trim() + "<!--more-->" + moreContent;
            post.setMoreText("");
        }

        // get rid of the p and br tags that the editor adds.
        if (post.isLocalDraft()) {
            descriptionContent = descriptionContent.replace("<p>", "").replace("</p>", "\n").replace("<br>", "");
        }

        // gets rid of the weird character android inserts after images
        descriptionContent = descriptionContent.replaceAll("\uFFFC", "");

        contentStruct.put("description", descriptionContent);
        if (!post.isPage()) {
            contentStruct.put("mt_keywords", post.getKeywords());

            if (postCategories != null && postCategories.length > 0) {
                contentStruct.put("categories", postCategories);
            }
        }

        contentStruct.put("mt_excerpt", post.getExcerpt());
        contentStruct.put((post.isPage()) ? "page_status" : "post_status", post.getStatus());

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
                    PostLocation location = post.getPostLocation();
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
                contentStruct.put("wp_post_thumbnail", "");
            } else {
                contentStruct.put("wp_post_thumbnail", post.getFeaturedImageId());
            }
        }

        contentStruct.put("wp_password", post.getPassword());

        List<Object> params = new ArrayList<>(5);
        if (post.isLocalDraft()) {
            params.add(site.getDotOrgSiteId());
        } else {
            params.add(post.getRemotePostId());
        }
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(contentStruct);
        params.add(false);

        final XMLRPC method = (post.isLocalDraft() ? XMLRPC.NEW_POST : XMLRPC.EDIT_POST);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params, new Listener() {
            @Override
            public void onResponse(Object response) {
                if (method.equals(XMLRPC.NEW_POST) && response instanceof String) {
                    post.setRemotePostId(Integer.valueOf((String) response));
                }
                post.setIsLocalDraft(false);
                post.setIsLocallyChanged(false);

                RemotePostPayload payload = new RemotePostPayload(post, site);
                mDispatcher.dispatch(PostActionBuilder.newPushedPostAction(payload));
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Implement lower-level catching in BaseXMLRPCClient
            }
        });

        add(request);
    }

    public void deletePost(final PostModel post, final SiteModel site) {
        List<Object> params = new ArrayList<>(4);
        params.add(site.getDotOrgSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(post.getRemotePostId());

        XMLRPC method = (post.isPage() ? XMLRPC.DELETE_PAGE : XMLRPC.DELETE_POST);

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), method, params, new Listener() {
            @Override
            public void onResponse(Object response) {}
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Implement lower-level catching in BaseXMLRPCClient
            }
        });

        add(request);
    }

    private PostsModel postsResponseToPostsModel(Object[] response, SiteModel site, boolean isPage, int startPosition) {
        List<Map<?, ?>> postsList = new ArrayList<>();
        for (int ctr = startPosition; ctr < response.length; ctr++) {
            Map<?, ?> postMap = (Map<?, ?>) response[ctr];
            postsList.add(postMap);
        }

        PostsModel posts = new PostsModel();
        PostModel post;

        for (Object postObject : postsList) {
            post = postResponseObjectToPostModel(postObject, site, isPage);
            if (post != null) {
                posts.add(post);
            }
        }

        if (posts.isEmpty()) {
            return null;
        }

        return posts;
    }

    private PostModel postResponseObjectToPostModel(Object postObject, SiteModel site, boolean isPage) {
        // Sanity checks
        if (!(postObject instanceof Map)) {
            return null;
        }

        Map<?, ?> postMap = (Map<?, ?>) postObject;
        PostModel post = new PostModel();

        String postID = MapUtils.getMapStr(postMap, (isPage) ? "page_id" : "postid");
        if (TextUtils.isEmpty(postID)) {
            // If we don't have a post or page ID, move on
            return null;
        }

        post.setLocalSiteId(site.getId());
        post.setRemotePostId(Integer.valueOf(postID));
        post.setTitle(MapUtils.getMapStr(postMap, "title"));

        Date dateCreatedGmt = MapUtils.getMapDate(postMap, "date_created_gmt");
        String timeAsIso8601 = DateTimeUtils.iso8601UTCFromDate(dateCreatedGmt);
        post.setDateCreated(timeAsIso8601);

        post.setDescription(MapUtils.getMapStr(postMap, "description"));
        post.setLink(MapUtils.getMapStr(postMap, "link"));
        post.setPermaLink(MapUtils.getMapStr(postMap, "permaLink"));

        Object[] postCategories = (Object[]) postMap.get("categories");
        JSONArray jsonCategoriesArray = new JSONArray();
        if (postCategories != null) {
            for (Object postCategory : postCategories) {
                jsonCategoriesArray.put(postCategory.toString());
            }
        }
        post.setCategories(jsonCategoriesArray.toString());

        Object[] custom_fields = (Object[]) postMap.get("custom_fields");
        JSONArray jsonCustomFieldsArray = new JSONArray();
        if (custom_fields != null) {
            PostLocation postLocation = new PostLocation();
            for (Object custom_field : custom_fields) {
                jsonCustomFieldsArray.put(custom_field.toString());
                // Update geo_long and geo_lat from custom fields
                if (!(custom_field instanceof Map))
                    continue;
                Map<?, ?> customField = (Map<?, ?>) custom_field;
                if (customField.get("key") != null && customField.get("value") != null) {
                    if (customField.get("key").equals("geo_longitude"))
                        postLocation.setLongitude(Long.valueOf(customField.get("value").toString()));
                    if (customField.get("key").equals("geo_latitude"))
                        postLocation.setLatitude(Long.valueOf(customField.get("value").toString()));
                }
            }
            post.setPostLocation(postLocation);
        }
        post.setCustomFields(jsonCustomFieldsArray.toString());

        post.setExcerpt(MapUtils.getMapStr(postMap, (isPage) ? "excerpt" : "mt_excerpt"));
        post.setMoreText(MapUtils.getMapStr(postMap, (isPage) ? "text_more" : "mt_text_more"));

        post.setSlug(MapUtils.getMapStr(postMap, "wp_slug"));
        post.setPassword(MapUtils.getMapStr(postMap, "wp_password"));
        post.setFeaturedImageId(MapUtils.getMapInt(postMap, "wp_post_thumbnail"));
        post.setStatus(MapUtils.getMapStr(postMap, (isPage) ? "page_status" : "post_status"));

        if (isPage) {
            post.setIsPage(true);
            post.setParentId(MapUtils.getMapLong(postMap, "wp_page_parent_id"));
            post.setParentTitle(MapUtils.getMapStr(postMap, "wp_page_parent_title"));
        } else {
            post.setKeywords(MapUtils.getMapStr(postMap, "mt_keywords"));
            post.setPostFormat(MapUtils.getMapStr(postMap, "wp_post_format"));
        }

        return post;
    }
}
