package org.wordpress.android.fluxc.network.rest.wpcom.site;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.account.NewAccountResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SiteRestClient extends BaseWPComRestClient {
    //
    // New site request keys
    //
    public static final String SITE_NAME_KEY = "blog_name";
    public static final String SITE_TITLE_KEY = "blog_title";
    public static final String LANGUAGE_ID_KEY = "lang_id";
    public static final String PUBLIC_KEY = "public";
    public static final String VALIDATE_KEY = "validate";
    public static final String CLIENT_ID_KEY = "client_id";
    public static final String CLIENT_SECRET_KEY = "client_secret";

    private final AppSecrets mAppSecrets;

    public static class NewSiteResponsePayload extends Payload {
        public NewSiteResponsePayload() {
        }
        public NewSiteError error;
        public boolean dryRun;
    }

    public static class DeleteSiteResponsePayload extends Payload {
        public DeleteSiteResponsePayload() {
        }
        public String status;
    }

    @Inject
    public SiteRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets appSecrets,
                          AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mAppSecrets = appSecrets;
    }

    public void fetchSites() {
        String url = WPCOMREST.me.sites.getUrlV1_1();
        final WPComGsonRequest<SitesResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                SitesResponse.class,
                new Listener<SitesResponse>() {
                    @Override
                    public void onResponse(SitesResponse response) {
                        List<SiteModel> siteArray = new ArrayList<>();

                        for (SiteWPComRestResponse siteResponse : response.sites) {
                            siteArray.add(siteResponseToSiteModel(siteResponse));
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(new SitesModel(siteArray)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SitesModel payload = new SitesModel(new ArrayList<SiteModel>());
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchSite(final SiteModel site) {
        String url = WPCOMREST.sites.getUrlV1_1() + site.getSiteId();
        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        SiteModel site = siteResponseToSiteModel(response);
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SiteModel payload = new SiteModel();
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(payload));
                    }
                }
        );
        add(request);
    }

    public void newSite(@NonNull String siteName, @NonNull String siteTitle, @NonNull String language,
                        @NonNull SiteVisibility visibility, final boolean dryRun) {
        String url = WPCOMREST.sites.new_.getUrlV1();
        Map<String, Object> body = new HashMap<>();
        body.put(SITE_NAME_KEY, siteName);
        body.put(SITE_TITLE_KEY, siteTitle);
        body.put(LANGUAGE_ID_KEY, language);
        body.put(PUBLIC_KEY, visibility.toString());
        body.put(VALIDATE_KEY, dryRun ? "1" : "0");
        body.put(CLIENT_ID_KEY, mAppSecrets.getAppId());
        body.put(CLIENT_SECRET_KEY, mAppSecrets.getAppSecret());

        WPComGsonRequest<NewAccountResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                NewAccountResponse.class,
                new Listener<NewAccountResponse>() {
                    @Override
                    public void onResponse(NewAccountResponse response) {
                        NewSiteResponsePayload payload = new NewSiteResponsePayload();
                        payload.dryRun = dryRun;
                        mDispatcher.dispatch(SiteActionBuilder.newCreatedNewSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        NewSiteResponsePayload payload = volleyErrorToAccountResponsePayload(error.volleyError);
                        payload.dryRun = dryRun;
                        mDispatcher.dispatch(SiteActionBuilder.newCreatedNewSiteAction(payload));
                    }
                }
        );

        request.disableRetries();
        add(request);
    }

    public void fetchPostFormats(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).post_formats.getUrlV1_1();
        final WPComGsonRequest<PostFormatsResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                PostFormatsResponse.class,
                new Listener<PostFormatsResponse>() {
                    @Override
                    public void onResponse(PostFormatsResponse response) {
                        List<PostFormatModel> postFormats = new ArrayList<>();
                        if (response.formats != null) {
                            for (String key : response.formats.keySet()) {
                                PostFormatModel postFormat = new PostFormatModel();
                                postFormat.setSlug(key);
                                postFormat.setDisplayName(response.formats.get(key));
                                postFormats.add(postFormat);
                            }
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(new
                                FetchedPostFormatsPayload(site, postFormats)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(site,
                                new ArrayList<PostFormatModel>());
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                    }
                }
        );
        add(request);
    }

    public void deleteSite(SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).delete.getUrlV1_1();
        WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload();
                        payload.status = response.status;
                        mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload();
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload));
                    }
                }
        );
        add(request);
    }

    private SiteModel siteResponseToSiteModel(SiteWPComRestResponse from) {
        SiteModel site = new SiteModel();
        site.setSiteId(from.ID);
        site.setUrl(from.URL);
        site.setName(from.name);
        site.setDescription(from.description);
        site.setIsJetpack(from.jetpack);
        site.setIsVisible(from.visible);
        site.setIsPrivate(from.is_private);
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read site options.
        if (from.options != null) {
            site.setIsFeaturedImageSupported(from.options.featured_images_enabled);
            site.setIsVideoPressSupported(from.options.videopress_enabled);
            site.setAdminUrl(from.options.admin_url);
            site.setLoginUrl(from.options.login_url);
            site.setTimezone(from.options.timezone);
        }
        if (from.plan != null) {
            site.setPlanId(from.plan.product_id);
            site.setPlanShortName(from.plan.product_name_short);
        }
        if (from.capabilities != null) {
            site.setHasCapabilityEditPages(from.capabilities.edit_pages);
            site.setHasCapabilityEditPosts(from.capabilities.edit_posts);
            site.setHasCapabilityEditOthersPosts(from.capabilities.edit_others_posts);
            site.setHasCapabilityEditOthersPages(from.capabilities.edit_others_pages);
            site.setHasCapabilityDeletePosts(from.capabilities.delete_posts);
            site.setHasCapabilityDeleteOthersPosts(from.capabilities.delete_others_posts);
            site.setHasCapabilityEditThemeOptions(from.capabilities.edit_theme_options);
            site.setHasCapabilityEditUsers(from.capabilities.edit_users);
            site.setHasCapabilityListUsers(from.capabilities.list_users);
            site.setHasCapabilityManageCategories(from.capabilities.manage_categories);
            site.setHasCapabilityManageOptions(from.capabilities.manage_options);
            site.setHasCapabilityActivateWordads(from.capabilities.activate_wordads);
            site.setHasCapabilityPromoteUsers(from.capabilities.promote_users);
            site.setHasCapabilityPublishPosts(from.capabilities.publish_posts);
            site.setHasCapabilityUploadFiles(from.capabilities.upload_files);
            site.setHasCapabilityDeleteUser(from.capabilities.delete_user);
            site.setHasCapabilityRemoveUsers(from.capabilities.remove_users);
            site.setHasCapabilityViewStats(from.capabilities.view_stats);
        }
        if (from.meta != null) {
            if (from.meta.links != null) {
                site.setXmlRpcUrl(from.meta.links.xmlrpc);
            }
        }
        site.setIsWPCom(true);
        return site;
    }

    private NewSiteResponsePayload volleyErrorToAccountResponsePayload(VolleyError error) {
        NewSiteResponsePayload payload = new NewSiteResponsePayload();
        payload.error = new NewSiteError(NewSiteErrorType.GENERIC_ERROR, "");
        if (error.networkResponse != null && error.networkResponse.data != null) {
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject errorObj = new JSONObject(jsonString);
                payload.error.type = NewSiteErrorType.fromString((String) errorObj.get("error"));
                payload.error.message = (String) errorObj.get("message");
            } catch (JSONException e) {
                // Do nothing (keep default error)
            }
        }
        return payload;
    }
}
