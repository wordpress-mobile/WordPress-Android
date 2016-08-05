package org.wordpress.android.fluxc.network.rest.wpcom.post;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.PostsModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPCOMREST;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse.PostsResponse;
import org.wordpress.android.fluxc.store.PostStore.FetchPostsResponsePayload;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PostRestClient extends BaseWPComRestClient {
    @Inject
    public PostRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                          UserAgent userAgent) {
        super(dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchPosts(final SiteModel site, final boolean getPages, final int offset) {
        String url = WPCOMREST.POSTS.getUrlV1WithSiteId(site.getSiteId());
        // TODO: Add offset to request params
        if (getPages) {
            url += "?type=page";
        }

        final WPComGsonRequest<PostsResponse> request = new WPComGsonRequest<>(Request.Method.GET,
                url, null, PostsResponse.class,
                new Response.Listener<PostsResponse>() {
                    @Override
                    public void onResponse(PostsResponse response) {
                        PostsModel posts = new PostsModel();
                        for (PostWPComRestResponse postResponse : response.posts) {
                            PostModel post = postResponseToPostModel(postResponse);
                            post.setLocalSiteId(site.getId());
                            posts.add(post);
                        }
                        // TODO: report canLoadMore properly once offset is implemented
                        FetchPostsResponsePayload payload = new FetchPostsResponsePayload(posts, site, getPages,
                                offset > 0, false);
                        mDispatcher.dispatch(PostActionBuilder.newFetchedPostsAction(payload));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    private PostModel postResponseToPostModel(PostWPComRestResponse from) {
        // TODO: Are author, short_URL, guid, or sticky needed?
        PostModel post = new PostModel();
        post.setRemotePostId(from.ID);
        post.setRemoteSiteId(from.site_ID);
        post.setLink(from.URL); // Is this right?
        post.setDateCreated(from.date);
        post.setTitle(from.title);
        post.setContent(from.content);
        post.setExcerpt(from.excerpt);
        post.setSlug(from.slug);
        post.setStatus(from.status);
        post.setPassword(from.password);
        post.setIsPage(from.type.equals("page"));

        if (from.post_thumbnail != null) {
            post.setFeaturedImageId(from.post_thumbnail.id);
        }
        post.setPostFormat(from.format);
        if (from.geo != null) {
            post.setLatitude(from.geo.latitude);
            post.setLongitude(from.geo.longitude);
        }

        // TODO: Add CategoryModel for tags and categories, and null check
        //post.setKeywords(from.tags);
        //post.setCategories(from.categories);

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
}
