package org.wordpress.android.fluxc.network.rest.wpcom.post;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.generated.UploadActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.list.AuthorFilter;
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.model.revisions.Diff;
import org.wordpress.android.fluxc.model.revisions.DiffOperations;
import org.wordpress.android.fluxc.model.revisions.RevisionModel;
import org.wordpress.android.fluxc.model.revisions.RevisionsModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostMeta.PostData.PostAutoSave;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostsResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.revisions.RevisionsResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.revisions.RevisionsResponse.DiffResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.revisions.RevisionsResponse.DiffResponsePart;
import org.wordpress.android.fluxc.network.rest.wpcom.revisions.RevisionsResponse.RevisionResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.taxonomy.TermWPComRestResponse;
import org.wordpress.android.fluxc.store.PostStore.DeletedPostPayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostListResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostStatusResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsResponsePayload;
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType;
import org.wordpress.android.fluxc.store.PostStore.PostError;
import org.wordpress.android.fluxc.store.PostStore.PostListItem;
import org.wordpress.android.fluxc.store.PostStore.RemoteAutoSavePostPayload;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        params.put("meta", "autosave");

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

    public void fetchPostStatus(final PostModel post, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).getUrlV1_1();

        Map<String, String> params = new HashMap<>();
        params.put("fields", "status");
        final WPComGsonRequest<PostWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        FetchPostStatusResponsePayload payload = new FetchPostStatusResponsePayload(post, site);
                        payload.remotePostStatus = response.getStatus();
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostStatusAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        FetchPostStatusResponsePayload payload = new FetchPostStatusResponsePayload(post, site);
                        payload.error = new PostError(error.apiError, error.message);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostStatusAction(payload));
                    }
                }
                                                                                                );
        add(request);
    }

    public void fetchPostList(final PostListDescriptorForRestSite listDescriptor, final long offset) {
        String url = WPCOMREST.sites.site(listDescriptor.getSite().getSiteId()).posts.getUrlV1_1();

        final int pageSize = listDescriptor.getConfig().getNetworkPageSize();
        String fields = TextUtils.join(",", Arrays.asList("ID", "modified", "status", "meta"));
        Map<String, String> params =
                createFetchPostListParameters(false, offset, pageSize, listDescriptor.getStatusList(),
                        listDescriptor.getAuthor(), fields, listDescriptor.getOrder().getValue(),
                        listDescriptor.getOrderBy().getValue(), listDescriptor.getSearchQuery());

        // We want to fetch only the minimal data required in order to save users' data
        params.put("meta_fields", "autosave.modified");

        final boolean loadedMore = offset > 0;

        final WPComGsonRequest<PostsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PostsResponse.class,
                new Listener<PostsResponse>() {
                    @Override
                    public void onResponse(PostsResponse response) {
                        List<PostListItem> postListItems = new ArrayList<>(response.getPosts().size());
                        for (PostWPComRestResponse postResponse : response.getPosts()) {
                            String autoSaveModified = null;
                            if (postResponse.getPostAutoSave() != null) {
                                autoSaveModified = postResponse.getPostAutoSave().getModified();
                            }
                            postListItems
                                    .add(new PostListItem(postResponse.getRemotePostId(), postResponse.getModified(),
                                            postResponse.getStatus(), autoSaveModified));
                        }
                        // The API sometimes return wrong number of posts "found", so we also check if we get an empty
                        // list in which case there would be no more posts to be fetched.
                        boolean canLoadMore = postListItems.size() > 0
                                              && response.getFound() > offset + postListItems.size();
                        FetchPostListResponsePayload responsePayload =
                                new FetchPostListResponsePayload(listDescriptor, postListItems, loadedMore,
                                        canLoadMore, null);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostListAction(responsePayload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        PostError postError = new PostError(error.apiError, error.message);
                        FetchPostListResponsePayload responsePayload =
                                new FetchPostListResponsePayload(listDescriptor, Collections.<PostListItem>emptyList(),
                                        loadedMore, false, postError);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostListAction(responsePayload));
                    }
                });
        add(request);
    }

    public void fetchPosts(final SiteModel site, final boolean getPages, final List<PostStatus> statusList,
                           final int offset, final int number) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.getUrlV1_1();

        Map<String, String> params =
                createFetchPostListParameters(getPages, offset, number, statusList, null, null, null, null, null);

        final WPComGsonRequest<PostsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PostsResponse.class,
                new Listener<PostsResponse>() {
                    @Override
                    public void onResponse(PostsResponse response) {
                        List<PostModel> postArray = new ArrayList<>();
                        PostModel post;
                        for (PostWPComRestResponse postResponse : response.getPosts()) {
                            post = postResponseToPostModel(postResponse);
                            post.setLocalSiteId(site.getId());
                            postArray.add(post);
                        }

                        boolean canLoadMore = postArray.size() == number;

                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(new PostsModel(postArray),
                                site, getPages, offset > 0, canLoadMore);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post_type (invalid post type, shouldn't happen)
                        PostError postError = new PostError(error.apiError, error.message);
                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(postError, getPages);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                });
        add(request);
    }

    public void pushPost(final PostModel post, final SiteModel site) {
        String url;

        if (post.isLocalDraft()) {
            url = WPCOMREST.sites.site(site.getSiteId()).posts.new_.getUrlV1_2();
        } else {
            url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).getUrlV1_2();
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

    public void remoteAutoSavePost(final @NonNull PostModel post, final @NonNull SiteModel site) {
        String url =
                WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).autosave.getUrlV1_1();

        Map<String, Object> body = postModelToAutoSaveParams(post);

        final WPComGsonRequest<PostRemoteAutoSaveModel> request = WPComGsonRequest.buildPostRequest(url, body,
                PostRemoteAutoSaveModel.class,
                new Listener<PostRemoteAutoSaveModel>() {
                    @Override
                    public void onResponse(PostRemoteAutoSaveModel response) {
                        RemoteAutoSavePostPayload payload =
                                new RemoteAutoSavePostPayload(post.getId(), post.getRemotePostId(), response, site);
                        mDispatcher.dispatch(UploadActionBuilder.newRemoteAutoSavedPostAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post (invalid post ID)
                        PostError postError = new PostError(error.apiError, error.message);
                        RemoteAutoSavePostPayload payload =
                                new RemoteAutoSavePostPayload(post.getId(), post.getRemotePostId(), postError);
                        mDispatcher.dispatch(UploadActionBuilder.newRemoteAutoSavedPostAction(payload));
                    }
                }
                                                                                                   );
        add(request);
    }

    public void deletePost(final @NonNull PostModel post, final @NonNull SiteModel site,
                           final @NonNull PostDeleteActionType postDeleteActionType) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).delete.getUrlV1_1();

        final WPComGsonRequest<PostWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel deletedPost = postResponseToPostModel(response);
                        deletedPost.setId(post.getId());
                        deletedPost.setLocalSiteId(post.getLocalSiteId());

                        DeletedPostPayload payload =
                                new DeletedPostPayload(post, site, postDeleteActionType, deletedPost);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post (invalid post ID)
                        PostError deletePostError = new PostError(error.apiError, error.message);
                        DeletedPostPayload payload =
                                new DeletedPostPayload(post, site, postDeleteActionType, deletePostError);
                        mDispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload));
                    }
                }
        );

        request.addQueryParameter("context", "edit");

        request.disableRetries();
        add(request);
    }

    public void restorePost(final PostModel post, final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).posts.post(post.getRemotePostId()).restore.getUrlV1_1();

        final WPComGsonRequest<PostWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PostWPComRestResponse.class,
                new Listener<PostWPComRestResponse>() {
                    @Override
                    public void onResponse(PostWPComRestResponse response) {
                        PostModel restoredPost = postResponseToPostModel(response);
                        restoredPost.setId(post.getId());
                        restoredPost.setLocalSiteId(post.getLocalSiteId());

                        RemotePostPayload payload = new RemotePostPayload(restoredPost, site);
                        mDispatcher.dispatch(PostActionBuilder.newRestoredPostAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        // Possible non-generic errors: 404 unknown_post (invalid post ID)
                        RemotePostPayload payload = new RemotePostPayload(post, site);
                        payload.error = new PostError(error.apiError, error.message);
                        mDispatcher.dispatch(PostActionBuilder.newRestoredPostAction(payload));
                    }
                }
        );

        request.addQueryParameter("context", "edit");

        request.disableRetries();
        add(request);
    }

    public void fetchRevisions(final PostModel post, final SiteModel site) {
        String url;
        if (post.isPage()) {
            url = WPCOMREST.sites.site(site.getSiteId()).page.post(post.getRemotePostId()).diffs.getUrlV1_1();
        } else {
            url = WPCOMREST.sites.site(site.getSiteId()).post.item(post.getRemotePostId()).diffs.getUrlV1_1();
        }

        final WPComGsonRequest<RevisionsResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                RevisionsResponse.class,
                new Listener<RevisionsResponse>() {
                    @Override
                    public void onResponse(RevisionsResponse response) {
                        mDispatcher.dispatch(PostActionBuilder.newFetchedRevisionsAction(
                                new FetchRevisionsResponsePayload(post, revisionsResponseToRevisionsModel(response))));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        FetchRevisionsResponsePayload payload = new FetchRevisionsResponsePayload(post, null);
                        payload.error = error;
                        mDispatcher.dispatch(PostActionBuilder.newFetchedRevisionsAction(payload));
                    }
                }
        );
        add(request);
    }

    private PostModel postResponseToPostModel(PostWPComRestResponse from) {
        PostModel post = new PostModel();
        post.setRemotePostId(from.getRemotePostId());
        post.setRemoteSiteId(from.getRemoteSiteId());
        post.setLink(from.getUrl()); // Is this right?
        post.setDateCreated(from.getDate());
        post.setLastModified(from.getModified());
        post.setRemoteLastModified(from.getModified());
        post.setTitle(from.getTitle());
        post.setContent(from.getContent());
        post.setExcerpt(from.getExcerpt());
        post.setSlug(from.getSlug());
        post.setStatus(from.getStatus());
        post.setPassword(from.getPassword());
        post.setIsPage(from.getType().equals("page"));

        if (from.getAuthor() != null) {
            post.setAuthorId(from.getAuthor().getId());
            post.setAuthorDisplayName(StringEscapeUtils.unescapeHtml4(from.getAuthor().getName()));
        }

        if (from.getPostThumbnail() != null) {
            post.setFeaturedImageId(from.getPostThumbnail().getId());
        }
        post.setPostFormat(from.getFormat());
        if (from.getGeo() != null) {
            post.setLatitude(from.getGeo().latitude);
            post.setLongitude(from.getGeo().longitude);
        }

        if (from.getCategories() != null) {
            List<Long> categoryIds = new ArrayList<>();
            for (TermWPComRestResponse value : from.getCategories().values()) {
                categoryIds.add(value.ID);
            }
            post.setCategoryIdList(categoryIds);
        }

        if (from.getTags() != null) {
            List<String> tagNames = new ArrayList<>();
            for (TermWPComRestResponse value : from.getTags().values()) {
                tagNames.add(value.name);
            }
            post.setTagNameList(tagNames);
        }

        if (from.getPostAutoSave() != null) {
            PostAutoSave autoSave = from.getPostAutoSave();
            post.setAutoSaveRevisionId(autoSave.getRevisionId());
            post.setAutoSaveModified(autoSave.getModified());
            post.setRemoteAutoSaveModified(autoSave.getModified());
            post.setAutoSavePreviewUrl(autoSave.getPreviewUrl());
            post.setAutoSaveTitle(autoSave.getTitle());
            post.setAutoSaveContent(autoSave.getContent());
            post.setAutoSaveExcerpt(autoSave.getExcerpt());
        }

        if (from.getCapabilities() != null) {
            post.setHasCapabilityPublishPost(from.getCapabilities().getPublishPost());
            post.setHasCapabilityEditPost(from.getCapabilities().getEditPost());
            post.setHasCapabilityDeletePost(from.getCapabilities().getDeletePost());
        }

        if (from.getParent() != null) {
            post.setParentId(from.getParent().ID);
            post.setParentTitle(from.getParent().title);
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
            params.put("parent", post.getParentId());
        }

        params.put("password", StringUtils.notNullStr(post.getPassword()));

        // construct a json object with a `category` field holding a json array with the tags
        JsonObject termsById = new JsonObject();
        JsonArray categoryIds = new JsonArray();
        for (Long categoryId : post.getCategoryIdList()) {
            categoryIds.add(categoryId);
        }
        termsById.add("category", categoryIds);
        // categories are transmitted via the `term_by_id.categories` field
        params.put("terms_by_id", termsById);

        // construct a json object with a `post_tag` field holding a json array with the tags
        JsonArray tags = new JsonArray();
        for (String tag : post.getTagNameList()) {
            tags.add(tag);
        }
        JsonObject terms = new JsonObject();
        terms.add("post_tag", tags);
        // categories are transmitted via the `terms.post_tag` field
        params.put("terms", terms);

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

    private Map<String, Object> postModelToAutoSaveParams(PostModel post) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", StringUtils.notNullStr(post.getTitle()));
        params.put("content", StringUtils.notNullStr(post.getContent()));
        params.put("excerpt", StringUtils.notNullStr(post.getExcerpt()));
        return params;
    }

    private RevisionsModel revisionsResponseToRevisionsModel(RevisionsResponse response) {
        ArrayList<RevisionModel> revisions = new ArrayList<>();
        for (DiffResponse diffResponse : response.getDiffs()) {
            RevisionResponse revision = response.getRevisions().get(Integer.toString(diffResponse.getTo()));

            ArrayList<Diff> titleDiffs = new ArrayList<>();
            for (DiffResponsePart titleDiffPart : diffResponse.getDiff().getPost_title()) {
                Diff diff = new Diff(DiffOperations.fromString(titleDiffPart.getOp()),
                        titleDiffPart.getValue());
                titleDiffs.add(diff);
            }

            ArrayList<Diff> contentDiffs = new ArrayList<>();
            for (DiffResponsePart contentDiffPart : diffResponse.getDiff().getPost_content()) {
                Diff diff = new Diff(DiffOperations.fromString(contentDiffPart.getOp()),
                        contentDiffPart.getValue());
                contentDiffs.add(diff);
            }

            RevisionModel revisionModel =
                    new RevisionModel(
                            revision.getId(),
                            diffResponse.getFrom(),
                            diffResponse.getDiff().getTotals().getAdd(),
                            diffResponse.getDiff().getTotals().getDel(),
                            revision.getPost_content(),
                            revision.getPost_excerpt(),
                            revision.getPost_title(),
                            revision.getPost_date_gmt(),
                            revision.getPost_modified_gmt(),
                            revision.getPost_author(),
                            titleDiffs,
                            contentDiffs
                    );
            revisions.add(revisionModel);
        }

        return new RevisionsModel(revisions);
    }

    private Map<String, String> createFetchPostListParameters(final boolean getPages,
                                                              final long offset,
                                                              final int number,
                                                              @Nullable final List<PostStatus> statusList,
                                                              @Nullable AuthorFilter authorFilter,
                                                              @Nullable final String fields,
                                                              @Nullable final String order,
                                                              @Nullable final String orderBy,
                                                              @Nullable final String searchQuery) {
        Map<String, String> params = new HashMap<>();

        params.put("context", "edit");
        params.put("meta", "autosave");
        params.put("number", String.valueOf(number));

        if (getPages) {
            params.put("type", "page");
        }

        if (!TextUtils.isEmpty(order)) {
            params.put("order", order);
        }
        if (!TextUtils.isEmpty(orderBy)) {
            params.put("order_by", orderBy);
        }
        if (statusList != null && statusList.size() > 0) {
            params.put("status", PostStatus.postStatusListToString(statusList));
        }
        if (!TextUtils.isEmpty(searchQuery)) {
            params.put("search", searchQuery);
        }
        if (offset > 0) {
            params.put("offset", String.valueOf(offset));
        }

        if (!TextUtils.isEmpty(fields)) {
            params.put("fields", fields);
        }

        if (authorFilter instanceof AuthorFilter.SpecificAuthor) {
            AuthorFilter.SpecificAuthor specificAuthor = (AuthorFilter.SpecificAuthor) authorFilter;
            params.put("author", String.valueOf(specificAuthor.getAuthorId()));
        }

        return params;
    }
}
