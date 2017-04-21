package org.wordpress.android.fluxc.network.rest.wpcom.site;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

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
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse;
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteError;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SiteRestClient extends BaseWPComRestClient {
    public static final int NEW_SITE_TIMEOUT_MS = 90000;

    private final AppSecrets mAppSecrets;

    public static class NewSiteResponsePayload extends Payload {
        public NewSiteResponsePayload() {
        }
        public long newSiteRemoteId;
        public NewSiteError error;
        public boolean dryRun;
    }

    public static class DeleteSiteResponsePayload extends Payload {
        public DeleteSiteResponsePayload() {
        }
        public SiteModel site;
        public DeleteSiteError error;
    }

    public static class ExportSiteResponsePayload extends Payload {
        public ExportSiteResponsePayload() {
        }
    }

    public static class IsWPComResponsePayload extends Payload {
        public IsWPComResponsePayload() {
        }
        public String url;
        public boolean isWPCom;
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

    public void checkUrlIsWPCom(@NonNull final String testedUrl) {
        String url = WPCOMREST.sites.getUrlV1_1() + testedUrl;
        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        IsWPComResponsePayload payload = new IsWPComResponsePayload();
                        payload.url = testedUrl;
                        payload.isWPCom = true;
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        IsWPComResponsePayload payload = new IsWPComResponsePayload();
                        payload.url = testedUrl;
                        // "unauthorized" and "unknown_blog" errors expected if the site is not accessible via
                        // the WPCom REST API.
                        if (error instanceof WPComGsonNetworkError
                            && ("unauthorized".equals(((WPComGsonNetworkError) error).apiError)
                                || "unknown_blog".equals(((WPComGsonNetworkError) error).apiError))) {
                            payload.isWPCom = false;
                        } else {
                            payload.error = error;
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload));
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
        body.put("blog_name", siteName);
        body.put("blog_title", siteTitle);
        body.put("lang_id", language);
        body.put("public", visibility.toString());
        body.put("validate", dryRun ? "1" : "0");
        body.put("client_id", mAppSecrets.getAppId());
        body.put("client_secret", mAppSecrets.getAppSecret());

        WPComGsonRequest<NewSiteResponse> request = WPComGsonRequest.buildPostRequest(url, body,
                NewSiteResponse.class,
                new Listener<NewSiteResponse>() {
                    @Override
                    public void onResponse(NewSiteResponse response) {
                        NewSiteResponsePayload payload = new NewSiteResponsePayload();
                        payload.dryRun = dryRun;
                        long siteId = 0;
                        if (response.blog_details != null) {
                            try {
                                siteId = Long.valueOf(response.blog_details.blogid);
                            } catch (NumberFormatException e) {
                                // No op: In dry run mode, returned newSiteRemoteId is "Array"
                            }
                        }
                        payload.newSiteRemoteId = siteId;
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

        // Disable retries and increase timeout for site creation (it can sometimes take a long time to complete)
        request.setRetryPolicy(new DefaultRetryPolicy(NEW_SITE_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
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

    public void deleteSite(final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).delete.getUrlV1_1();
        WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload();
                        payload.site = site;
                        mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload();
                        WPComGsonNetworkError networkError = ((WPComGsonNetworkError) error);
                        payload.error = new DeleteSiteError(networkError.apiError, networkError.message);
                        payload.site = site;
                        mDispatcher.dispatch(SiteActionBuilder.newDeletedSiteAction(payload));
                    }
                }
        );
        add(request);
    }

    public void exportSite(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).exports.start.getUrlV1_1();
        final WPComGsonRequest<ExportSiteResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                ExportSiteResponse.class,
                new Listener<ExportSiteResponse>() {
                    @Override
                    public void onResponse(ExportSiteResponse response) {
                        ExportSiteResponsePayload payload = new ExportSiteResponsePayload();
                        mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        ExportSiteResponsePayload payload = new ExportSiteResponsePayload();
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload));
                    }
                }
        );
        add(request);
    }

    public void suggestDomains(@NonNull final String query, final boolean includeWordpressCom,
                               final boolean includeDotBlogSubdomain, final int quantity) {
        String url = WPCOMREST.domains.suggestions.getUrlV1_1();
        Map<String, String> params = new HashMap<>(4);
        params.put("query", query);
        // ugly trick to bypass checkstyle and its dot com rule
        params.put("include_wordpressdot" + "com", String.valueOf(includeWordpressCom));
        params.put("include_dotblogsubdomain", String.valueOf(includeDotBlogSubdomain));
        params.put("quantity", String.valueOf(quantity));
        final WPComGsonRequest<ArrayList<DomainSuggestionResponse>> request =
                WPComGsonRequest.buildGetRequest(url, params,
                        new TypeToken<ArrayList<DomainSuggestionResponse>>(){}.getType(),
                new Listener<ArrayList<DomainSuggestionResponse>>() {
                    @Override
                    public void onResponse(ArrayList<DomainSuggestionResponse> response) {
                        SuggestDomainsResponsePayload payload = new SuggestDomainsResponsePayload(query, response);
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SuggestDomainsResponsePayload payload = new SuggestDomainsResponsePayload(query, error);
                        mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
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
        site.setIsJetpackConnected(from.jetpack);
        site.setIsJetpackInstalled(from.jetpack);
        site.setIsVisible(from.visible);
        site.setIsPrivate(from.is_private);
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read site options.
        if (from.options != null) {
            site.setIsFeaturedImageSupported(from.options.featured_images_enabled);
            site.setIsVideoPressSupported(from.options.videopress_enabled);
            site.setIsAutomatedTransfer(from.options.is_automated_transfer);
            site.setAdminUrl(from.options.admin_url);
            site.setLoginUrl(from.options.login_url);
            site.setTimezone(from.options.gmt_offset);
            site.setFrameNonce(from.options.frame_nonce);
            site.setUnmappedUrl(from.options.unmapped_url);
        }
        if (from.plan != null) {
            try {
                site.setPlanId(Long.valueOf(from.plan.product_id));
            } catch (NumberFormatException e) {
                // VIP sites return a String plan ID ('vip') rather than a number
                if (from.plan.product_id.equals("vip")) {
                    site.setPlanId(SiteModel.VIP_PLAN_ID);
                }
            }
            site.setPlanShortName(from.plan.product_name_short);
            site.setHasFreePlan(from.plan.is_free);
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
        if (from.icon != null) {
            site.setIconUrl(from.icon.img);
        }
        if (from.meta != null) {
            if (from.meta.links != null) {
                site.setXmlRpcUrl(from.meta.links.xmlrpc);
            }
        }
        // Only set the isWPCom flag for "pure" WPCom sites
        if (!from.jetpack) {
            site.setIsWPCom(true);
        }
        site.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
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
