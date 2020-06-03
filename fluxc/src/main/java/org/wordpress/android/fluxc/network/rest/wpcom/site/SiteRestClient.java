package org.wordpress.android.fluxc.network.rest.wpcom.site;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.SiteAction;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;
import org.wordpress.android.fluxc.model.PlanModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets;
import org.wordpress.android.fluxc.network.rest.wpcom.site.AutomatedTransferEligibilityCheckResponse.EligibilityError;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteWPComRestResponse.SitesResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.UserRoleWPComRestResponse.UserRolesResponse;
import org.wordpress.android.fluxc.store.SiteStore.PrivateAtomicCookieError;
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType;
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferEligibilityResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferError;
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferStatusResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.DeleteSiteError;
import org.wordpress.android.fluxc.store.SiteStore.DesignateMobileEditorForAllSitesResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainError;
import org.wordpress.android.fluxc.store.SiteStore.DesignatePrimaryDomainErrorType;
import org.wordpress.android.fluxc.store.SiteStore.DesignatedPrimaryDomainPayload;
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityError;
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityErrorType;
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityStatus;
import org.wordpress.android.fluxc.store.SiteStore.DomainMappabilityStatus;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedCountriesError;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedCountriesErrorType;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedCountriesResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesError;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesErrorType;
import org.wordpress.android.fluxc.store.SiteStore.DomainSupportedStatesResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPrivateAtomicCookiePayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedEditorsPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPlansPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.FetchedUserRolesPayload;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteError;
import org.wordpress.android.fluxc.store.SiteStore.NewSiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.PlansError;
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsError;
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType;
import org.wordpress.android.fluxc.store.SiteStore.QuickStartCompletedResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.QuickStartError;
import org.wordpress.android.fluxc.store.SiteStore.QuickStartErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteEditorsError;
import org.wordpress.android.fluxc.store.SiteStore.SiteEditorsErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteError;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteVisibility;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainError;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsResponsePayload;
import org.wordpress.android.fluxc.store.SiteStore.UserRolesError;
import org.wordpress.android.fluxc.store.SiteStore.UserRolesErrorType;
import org.wordpress.android.fluxc.utils.SiteUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class SiteRestClient extends BaseWPComRestClient {
    public static final int NEW_SITE_TIMEOUT_MS = 90000;
    private static final String SITE_FIELDS = "ID,URL,name,description,jetpack,visible,is_private,options,plan,"
        + "capabilities,quota,icon,meta";

    private final AppSecrets mAppSecrets;

    public static class NewSiteResponsePayload extends Payload<NewSiteError> {
        public NewSiteResponsePayload() {}
        public long newSiteRemoteId;
        public boolean dryRun;
    }

    public static class DeleteSiteResponsePayload extends Payload<DeleteSiteError> {
        public DeleteSiteResponsePayload() {}
        public SiteModel site;
    }

    public static class ExportSiteResponsePayload extends Payload<BaseNetworkError> {
        public ExportSiteResponsePayload() {}
    }

    public static class IsWPComResponsePayload extends Payload<BaseNetworkError> {
        public IsWPComResponsePayload() {}
        public String url;
        public boolean isWPCom;
    }

    public static class FetchWPComSiteResponsePayload extends Payload<SiteError> {
        public FetchWPComSiteResponsePayload() {}
        public String checkedUrl;
        public SiteModel site;
    }

    public SiteRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets appSecrets,
                          AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
        mAppSecrets = appSecrets;
    }

    public void fetchSites() {
        Map<String, String> params = new HashMap<>();
        params.put("fields", SITE_FIELDS);
        String url = WPCOMREST.me.sites.getUrlV1_1();
        final WPComGsonRequest<SitesResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                SitesResponse.class,
                new Listener<SitesResponse>() {
                    @Override
                    public void onResponse(SitesResponse response) {
                        if (response != null) {
                            List<SiteModel> siteArray = new ArrayList<>();

                            for (SiteWPComRestResponse siteResponse : response.sites) {
                                siteArray.add(siteResponseToSiteModel(siteResponse));
                            }
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesAction(new SitesModel(siteArray)));
                        } else {
                            AppLog.e(T.API, "Received empty response to /me/sites/");
                            SitesModel payload = new SitesModel(Collections.<SiteModel>emptyList());
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SitesModel payload = new SitesModel(Collections.<SiteModel>emptyList());
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchSite(final SiteModel site) {
        Map<String, String> params = new HashMap<>();
        params.put("fields", SITE_FIELDS);
        String url = WPCOMREST.sites.getUrlV1_1() + site.getSiteId();
        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        if (response != null) {
                            SiteModel newSite = siteResponseToSiteModel(response);
                            // local ID is not copied into the new model, let's make sure it is
                            // otherwise the call that updates the DB can add a new row?
                            if (site.getId() > 0) {
                                newSite.setId(site.getId());
                            }
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(newSite));
                        } else {
                            AppLog.e(T.API, "Received empty response to /sites/$site/ for " + site.getUrl());
                            SiteModel payload = new SiteModel();
                            payload.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SiteModel payload = new SiteModel();
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(payload));
                    }
                }
        );
        add(request);
    }

    public void newSite(@NonNull String siteName, @NonNull String language,
                        @NonNull SiteVisibility visibility, @Nullable Long segmentId,
                        final boolean dryRun) {
        String url = WPCOMREST.sites.new_.getUrlV1_1();
        Map<String, Object> body = new HashMap<>();
        body.put("blog_name", siteName);
        body.put("lang_id", language);
        body.put("public", visibility.toString());
        body.put("validate", dryRun ? "1" : "0");
        body.put("client_id", mAppSecrets.getAppId());
        body.put("client_secret", mAppSecrets.getAppSecret());

        // Add site options if available
        Map<String, Object> options = new HashMap<>();
        if (segmentId != null) {
            options.put("site_segment", segmentId);
        }
        if (options.size() > 0) {
            body.put("options", options);
        }

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
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
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

    public void fetchSiteEditors(final SiteModel site) {
        Map<String, String> params = new HashMap<>();
        String url = WPCOMV2.sites.site(site.getSiteId()).gutenberg.getUrl();
        final WPComGsonRequest<SiteEditorsResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                SiteEditorsResponse.class,
                new Listener<SiteEditorsResponse>() {
                    @Override
                    public void onResponse(SiteEditorsResponse response) {
                        if (response != null) {
                            FetchedEditorsPayload payload;
                            payload = new FetchedEditorsPayload(site, response.editor_web, response.editor_mobile);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload));
                        } else {
                            AppLog.e(T.API, "Received empty response to /sites/$site/gutenberg for " + site.getUrl());
                            FetchedEditorsPayload payload = new FetchedEditorsPayload(site, "", "");
                            payload.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        FetchedEditorsPayload payload = new FetchedEditorsPayload(site, "", "");
                        payload.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload));
                    }
                }
                );
        add(request);
    }

    public void designateMobileEditor(final SiteModel site, final String mobileEditorName) {
        Map<String, Object> params = new HashMap<>();
        String url = WPCOMV2.sites.site(site.getSiteId()).gutenberg.getUrl();
        params.put("editor", mobileEditorName);
        params.put("platform", "mobile");
        final WPComGsonRequest<SiteEditorsResponse> request = WPComGsonRequest
                .buildPostRequest(url, params, SiteEditorsResponse.class,
                        new Listener<SiteEditorsResponse>() {
                            @Override
                            public void onResponse(SiteEditorsResponse response) {
                                FetchedEditorsPayload payload;
                                payload = new FetchedEditorsPayload(site, response.editor_web, response.editor_mobile);
                                mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                FetchedEditorsPayload payload = new FetchedEditorsPayload(site, "", "");
                                payload.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
                                mDispatcher.dispatch(SiteActionBuilder.newFetchedSiteEditorsAction(payload));
                            }
                        });
        add(request);
    }

    public void designateMobileEditorForAllSites(final String mobileEditorName, final boolean setOnlyIfEmpty) {
        Map<String, Object> params = new HashMap<>();
        String url = WPCOMV2.me.gutenberg.getUrl();
        params.put("editor", mobileEditorName);
        params.put("platform", "mobile");
        if (setOnlyIfEmpty) {
            params.put("set_only_if_empty", "true");
        }
        // Else, omit the "set_only_if_empty" parameters.
        // There is an issue in the API implementation. It only checks
        // for "set_only_if_empty" presence but don't check for its value.

        add(WPComGsonRequest
                .buildPostRequest(url, params, Map.class,
                        new Listener<Map<String, String>>() {
                            @Override
                            public void onResponse(Map<String, String> response) {
                                DesignateMobileEditorForAllSitesResponsePayload payload =
                                        new DesignateMobileEditorForAllSitesResponsePayload(response);
                                mDispatcher.dispatch(
                                        SiteActionBuilder.newDesignatedMobileEditorForAllSitesAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                DesignateMobileEditorForAllSitesResponsePayload payload =
                                        new DesignateMobileEditorForAllSitesResponsePayload(null);
                                payload.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
                                mDispatcher.dispatch(
                                        SiteActionBuilder.newDesignatedMobileEditorForAllSitesAction(payload));
                            }
                        })
           );
    }

    public void fetchPostFormats(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).post_formats.getUrlV1_1();
        final WPComGsonRequest<PostFormatsResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                PostFormatsResponse.class,
                new Listener<PostFormatsResponse>() {
                    @Override
                    public void onResponse(PostFormatsResponse response) {
                        List<PostFormatModel> postFormats = SiteUtils.getValidPostFormatsOrNull(response.formats);

                        if (postFormats != null) {
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(new
                                    FetchedPostFormatsPayload(site, postFormats)));
                        } else {
                            FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(site,
                                    Collections.<PostFormatModel>emptyList());
                            payload.error = new PostFormatsError(PostFormatsErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(site,
                                Collections.<PostFormatModel>emptyList());
                        // TODO: what other kind of error could we get here?
                        payload.error = new PostFormatsError(PostFormatsErrorType.GENERIC_ERROR);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchUserRoles(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).roles.getUrlV1_1();
        final WPComGsonRequest<UserRolesResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                UserRolesResponse.class,
                new Listener<UserRolesResponse>() {
                    @Override
                    public void onResponse(UserRolesResponse response) {
                        List<RoleModel> roleArray = new ArrayList<>();
                        for (UserRoleWPComRestResponse roleResponse : response.roles) {
                            RoleModel roleModel = new RoleModel();
                            roleModel.setName(roleResponse.name);
                            roleModel.setDisplayName(StringEscapeUtils.unescapeHtml4(roleResponse.display_name));
                            roleArray.add(roleModel);
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedUserRolesAction(new
                                FetchedUserRolesPayload(site, roleArray)));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        FetchedUserRolesPayload payload = new FetchedUserRolesPayload(site,
                                Collections.<RoleModel>emptyList());
                        // TODO: what other kind of error could we get here?
                        payload.error = new UserRolesError(UserRolesErrorType.GENERIC_ERROR);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedUserRolesAction(payload));
                    }
                }
        );
        add(request);
    }

    public void fetchPlans(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plans.getUrlV1_3();
        final WPComGsonRequest<PlansResponse> request =
                WPComGsonRequest.buildGetRequest(url, null, PlansResponse.class,
                        new Listener<PlansResponse>() {
                            @Override
                            public void onResponse(PlansResponse response) {
                                List<PlanModel> plans = response.getPlansList();
                                mDispatcher.dispatch(
                                        SiteActionBuilder.newFetchedPlansAction(new FetchedPlansPayload(site, plans)));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                PlansError plansError = new PlansError(error.apiError, error.message);
                                FetchedPlansPayload payload = new FetchedPlansPayload(site, plansError);
                                mDispatcher.dispatch(SiteActionBuilder.newFetchedPlansAction(payload));
                            }
                        });
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
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        DeleteSiteResponsePayload payload = new DeleteSiteResponsePayload();
                        payload.error = new DeleteSiteError(error.apiError, error.message);
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
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        ExportSiteResponsePayload payload = new ExportSiteResponsePayload();
                        payload.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newExportedSiteAction(payload));
                    }
                }
        );
        add(request);
    }

    public void suggestDomains(@NonNull final String query, final Boolean onlyWordpressCom,
                               final Boolean includeWordpressCom, final Boolean includeDotBlogSubdomain,
                               final Long segmentId, final int quantity, final boolean includeVendorDot,
                               final String tlds) {
        String url = WPCOMREST.domains.suggestions.getUrlV1_1();
        Map<String, String> params = new HashMap<>(4);
        params.put("query", query);
        if (onlyWordpressCom != null) {
            params.put("only_wordpressdotcom", String.valueOf(onlyWordpressCom)); // CHECKSTYLE IGNORE
        }
        if (includeWordpressCom != null) {
            params.put("include_wordpressdotcom", String.valueOf(includeWordpressCom)); // CHECKSTYLE IGNORE
        }
        if (includeDotBlogSubdomain != null) {
            params.put("include_dotblogsubdomain", String.valueOf(includeDotBlogSubdomain));
        }
        if (segmentId != null) {
            params.put("segment_id", String.valueOf(segmentId));
        }
        if (tlds != null) {
            params.put("tlds", tlds);
        }
        params.put("quantity", String.valueOf(quantity));
        if (includeVendorDot) {
            params.put("vendor", "dot");
        }
        final WPComGsonRequest<ArrayList<DomainSuggestionResponse>> request =
                WPComGsonRequest.buildGetRequest(url, params,
                        new TypeToken<ArrayList<DomainSuggestionResponse>>() {
                        }.getType(),
                        new Listener<ArrayList<DomainSuggestionResponse>>() {
                            @Override
                            public void onResponse(ArrayList<DomainSuggestionResponse> response) {
                                SuggestDomainsResponsePayload payload = new SuggestDomainsResponsePayload(query,
                                        response);
                                mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                SuggestDomainError suggestDomainError =
                                        new SuggestDomainError(error.apiError, error.message);
                                if (suggestDomainError.type == SuggestDomainErrorType.EMPTY_RESULTS) {
                                    // Empty results is not an actual error, the API should return 200 for it
                                    SuggestDomainsResponsePayload payload = new SuggestDomainsResponsePayload(query,
                                            Collections.<DomainSuggestionResponse>emptyList());
                                    mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
                                } else {
                                    SuggestDomainsResponsePayload payload =
                                            new SuggestDomainsResponsePayload(query, suggestDomainError);
                                    mDispatcher.dispatch(SiteActionBuilder.newSuggestedDomainsAction(payload));
                                }
                            }
                        }
                                                );
        add(request);
    }

    //
    // Unauthenticated network calls
    //

    public void fetchConnectSiteInfo(@NonNull final String siteUrl) {
        // Get a proper URI to reliably retrieve the scheme.
        URI uri;
        try {
            uri = URI.create(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false));
        } catch (IllegalArgumentException e) {
            SiteError siteError = new SiteError(SiteErrorType.INVALID_SITE);
            ConnectSiteInfoPayload payload = new ConnectSiteInfoPayload(siteUrl, siteError);
            mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(payload));
            return;
        }

        Map<String, String> params = new HashMap<>(1);
        params.put("url", uri.toString());

        // Make the call.
        String url = WPCOMREST.connect.site_info.getUrlV1_1();
        final WPComGsonRequest<ConnectSiteInfoResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                ConnectSiteInfoResponse.class,
                new Listener<ConnectSiteInfoResponse>() {
                    @Override
                    public void onResponse(ConnectSiteInfoResponse response) {
                        ConnectSiteInfoPayload info = connectSiteInfoFromResponse(siteUrl, response);
                        info.url = siteUrl;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(info));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        SiteError siteError = new SiteError(SiteErrorType.INVALID_SITE);
                        ConnectSiteInfoPayload info = new ConnectSiteInfoPayload(siteUrl, siteError);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedConnectSiteInfoAction(info));
                    }
                }
        );
        addUnauthedRequest(request);
    }

    public void fetchWPComSiteByUrl(@NonNull final String siteUrl) {
        String sanitizedUrl;
        try {
            URI uri = URI.create(UrlUtils.addUrlSchemeIfNeeded(siteUrl, false));
            sanitizedUrl = URLEncoder.encode(UrlUtils.removeScheme(uri.toString()), "UTF-8");
        } catch (IllegalArgumentException e) {
            FetchWPComSiteResponsePayload payload = new FetchWPComSiteResponsePayload();
            payload.checkedUrl = siteUrl;
            payload.error = new SiteError(SiteErrorType.INVALID_SITE);
            mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload));
            return;
        } catch (UnsupportedEncodingException e) {
            // This should be impossible (it means an Android device without UTF-8 support)
            throw new IllegalStateException(e);
        }

        String requestUrl = WPCOMREST.sites.siteUrl(sanitizedUrl).getUrlV1_1();

        final WPComGsonRequest<SiteWPComRestResponse> request = WPComGsonRequest.buildGetRequest(requestUrl, null,
                SiteWPComRestResponse.class,
                new Listener<SiteWPComRestResponse>() {
                    @Override
                    public void onResponse(SiteWPComRestResponse response) {
                        FetchWPComSiteResponsePayload payload = new FetchWPComSiteResponsePayload();
                        payload.checkedUrl = siteUrl;
                        payload.site = siteResponseToSiteModel(response);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        FetchWPComSiteResponsePayload payload = new FetchWPComSiteResponsePayload();
                        payload.checkedUrl = siteUrl;

                        SiteErrorType siteErrorType = SiteErrorType.GENERIC_ERROR;
                        switch (error.apiError) {
                            case "unauthorized":
                                siteErrorType = SiteErrorType.UNAUTHORIZED;
                                break;
                            case "unknown_blog":
                                siteErrorType = SiteErrorType.UNKNOWN_SITE;
                                break;
                        }
                        payload.error = new SiteError(siteErrorType);

                        mDispatcher.dispatch(SiteActionBuilder.newFetchedWpcomSiteByUrlAction(payload));
                    }
                }
        );
        addUnauthedRequest(request);
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
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        IsWPComResponsePayload payload = new IsWPComResponsePayload();
                        payload.url = testedUrl;
                        // "unauthorized" and "unknown_blog" errors expected if the site is not accessible via
                        // the WPCom REST API.
                        if ("unauthorized".equals(error.apiError) || "unknown_blog".equals(error.apiError)) {
                            payload.isWPCom = false;
                        } else {
                            payload.error = error;
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedIsWpcomUrlAction(payload));
                    }
                }
        );
        addUnauthedRequest(request);
    }

    /**
     * Performs an HTTP GET call to v1.3 /domains/$domainName/is-available/ endpoint. Upon receiving a response
     * (success or error) a {@link SiteAction#CHECKED_DOMAIN_AVAILABILITY} action is dispatched with a
     * payload of type {@link DomainAvailabilityResponsePayload}.
     *
     * {@link DomainAvailabilityResponsePayload#isError()} can be used to check the request result.
     */
    public void checkDomainAvailability(@NonNull final String domainName) {
        String url = WPCOMREST.domains.domainName(domainName).is_available.getUrlV1_3();
        final WPComGsonRequest<DomainAvailabilityResponse> request =
                WPComGsonRequest.buildGetRequest(url, null, DomainAvailabilityResponse.class,
                        new Listener<DomainAvailabilityResponse>() {
                            @Override
                            public void onResponse(DomainAvailabilityResponse response) {
                                DomainAvailabilityResponsePayload payload =
                                        responseToDomainAvailabilityPayload(response);
                                mDispatcher.dispatch(SiteActionBuilder.newCheckedDomainAvailabilityAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                // Domain availability API should always return a response for a valid,
                                // authenticated user. Therefore, only GENERIC_ERROR is identified here.
                                DomainAvailabilityError domainAvailabilityError = new DomainAvailabilityError(
                                        DomainAvailabilityErrorType.GENERIC_ERROR, error.message);
                                DomainAvailabilityResponsePayload payload =
                                        new DomainAvailabilityResponsePayload(domainAvailabilityError);
                                mDispatcher.dispatch(SiteActionBuilder.newCheckedDomainAvailabilityAction(payload));
                            }
                        });
        add(request);
    }

    /**
     * Performs an HTTP GET call to v1.1 /domains/supported-states/$countryCode endpoint. Upon receiving a response
     * (success or error) a {@link SiteAction#FETCHED_DOMAIN_SUPPORTED_STATES} action is dispatched with a
     * payload of type {@link DomainSupportedStatesResponsePayload}.
     *
     * {@link DomainSupportedStatesResponsePayload#isError()} can be used to check the request result.
     */
    public void fetchSupportedStates(@NonNull final String countryCode) {
        String url = WPCOMREST.domains.supported_states.countryCode(countryCode).getUrlV1_1();
        final WPComGsonRequest<List<SupportedStateResponse>> request =
                WPComGsonRequest.buildGetRequest(url, null,
                        new TypeToken<ArrayList<SupportedStateResponse>>() {}.getType(),
                        new Listener<List<SupportedStateResponse>>() {
                            @Override
                            public void onResponse(List<SupportedStateResponse> response) {
                                DomainSupportedStatesResponsePayload payload =
                                        new DomainSupportedStatesResponsePayload(response);
                                mDispatcher.dispatch(SiteActionBuilder.newFetchedDomainSupportedStatesAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                DomainSupportedStatesError domainSupportedStatesError = new DomainSupportedStatesError(
                                        DomainSupportedStatesErrorType.fromString(error.apiError), error.message);
                                DomainSupportedStatesResponsePayload payload =
                                        new DomainSupportedStatesResponsePayload(domainSupportedStatesError);
                                mDispatcher.dispatch(SiteActionBuilder.newFetchedDomainSupportedStatesAction(payload));
                            }
                        });
        add(request);
    }

    /**
     * Performs an HTTP GET call to v1.1 /domains/supported-countries/ endpoint. Upon receiving a response
     * (success or error) a {@link SiteAction#FETCHED_DOMAIN_SUPPORTED_COUNTRIES} action is dispatched with a
     * payload of type {@link DomainSupportedCountriesResponsePayload}.
     *
     * {@link DomainSupportedCountriesResponsePayload#isError()} can be used to check the request result.
     */
    public void fetchSupportedCountries() {
        String url = WPCOMREST.domains.supported_countries.getUrlV1_1();
        final WPComGsonRequest<ArrayList<SupportedCountryResponse>> request =
                WPComGsonRequest.buildGetRequest(url, null,
                        new TypeToken<ArrayList<SupportedCountryResponse>>() {}.getType(),
                        new Listener<ArrayList<SupportedCountryResponse>>() {
                            @Override
                            public void onResponse(ArrayList<SupportedCountryResponse> response) {
                                DomainSupportedCountriesResponsePayload payload =
                                        new DomainSupportedCountriesResponsePayload(response);
                                mDispatcher.dispatch(
                                        SiteActionBuilder.newFetchedDomainSupportedCountriesAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                                // Supported Countries API should always return a response for a valid,
                                // authenticated user. Therefore, only GENERIC_ERROR is identified here.
                                DomainSupportedCountriesError domainSupportedCountriesError =
                                        new DomainSupportedCountriesError(
                                                DomainSupportedCountriesErrorType.GENERIC_ERROR,
                                                error.message);
                                DomainSupportedCountriesResponsePayload payload =
                                        new DomainSupportedCountriesResponsePayload(domainSupportedCountriesError);
                                mDispatcher.dispatch(
                                        SiteActionBuilder.newFetchedDomainSupportedCountriesAction(payload));
                            }
                        });
        add(request);
    }

    public void designatePrimaryDomain(@NonNull final SiteModel site, String domain) {
        String url = WPCOMREST.sites.site(site.getSiteId()).domains.primary.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("domain", domain);
        final WPComGsonRequest<DesignatePrimaryDomainResponse> request = WPComGsonRequest
                .buildPostRequest(url, params, DesignatePrimaryDomainResponse.class,
                        new Listener<DesignatePrimaryDomainResponse>() {
                            @Override
                            public void onResponse(DesignatePrimaryDomainResponse response) {
                                mDispatcher.dispatch(SiteActionBuilder.newDesignatedPrimaryDomainAction(
                                        new DesignatedPrimaryDomainPayload(site, response.getSuccess())));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                                DesignatePrimaryDomainError error = new DesignatePrimaryDomainError(
                                        DesignatePrimaryDomainErrorType.GENERIC_ERROR, networkError.message);

                                DesignatedPrimaryDomainPayload payload =
                                        new DesignatedPrimaryDomainPayload(site, false);
                                payload.error = error;

                                mDispatcher.dispatch(SiteActionBuilder.newDesignatedPrimaryDomainAction(payload));
                            }
                        });
        add(request);
    }

    // Automated Transfers

    public void checkAutomatedTransferEligibility(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).automated_transfers.eligibility.getUrlV1_1();
        final WPComGsonRequest<AutomatedTransferEligibilityCheckResponse> request = WPComGsonRequest
                .buildGetRequest(url, null, AutomatedTransferEligibilityCheckResponse.class,
                new Listener<AutomatedTransferEligibilityCheckResponse>() {
                    @Override
                    public void onResponse(AutomatedTransferEligibilityCheckResponse response) {
                        List<String> strErrorCodes = new ArrayList<>();
                        if (response.errors != null) {
                            for (EligibilityError eligibilityError : response.errors) {
                                strErrorCodes.add(eligibilityError.code);
                            }
                        }
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedAutomatedTransferEligibilityAction(
                                new AutomatedTransferEligibilityResponsePayload(site, response.isEligible,
                                        strErrorCodes)));
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                        AutomatedTransferError payloadError = new AutomatedTransferError(
                                networkError.apiError, networkError.message);
                        mDispatcher.dispatch(SiteActionBuilder.newCheckedAutomatedTransferEligibilityAction(
                                new AutomatedTransferEligibilityResponsePayload(site, payloadError)));
                    }
                });
        add(request);
    }

    public void initiateAutomatedTransfer(@NonNull final SiteModel site, @NonNull final String pluginSlugToInstall) {
        String url = WPCOMREST.sites.site(site.getSiteId()).automated_transfers.initiate.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("plugin", pluginSlugToInstall);
        final WPComGsonRequest<InitiateAutomatedTransferResponse> request = WPComGsonRequest
                .buildPostRequest(url, params, InitiateAutomatedTransferResponse.class,
                        new Listener<InitiateAutomatedTransferResponse>() {
                            @Override
                            public void onResponse(InitiateAutomatedTransferResponse response) {
                                InitiateAutomatedTransferResponsePayload payload =
                                        new InitiateAutomatedTransferResponsePayload(site, pluginSlugToInstall);
                                payload.success = response.success;
                                mDispatcher.dispatch(SiteActionBuilder.newInitiatedAutomatedTransferAction(payload));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                                InitiateAutomatedTransferResponsePayload payload =
                                        new InitiateAutomatedTransferResponsePayload(site, pluginSlugToInstall);
                                payload.error = new AutomatedTransferError(networkError.apiError, networkError.message);
                                mDispatcher.dispatch(SiteActionBuilder.newInitiatedAutomatedTransferAction(payload));
                            }
                        });
        add(request);
    }

    public void checkAutomatedTransferStatus(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).automated_transfers.status.getUrlV1_1();
        final WPComGsonRequest<AutomatedTransferStatusResponse> request = WPComGsonRequest
                .buildGetRequest(url, null, AutomatedTransferStatusResponse.class,
                        new Listener<AutomatedTransferStatusResponse>() {
                            @Override
                            public void onResponse(AutomatedTransferStatusResponse response) {
                                mDispatcher.dispatch(SiteActionBuilder.newCheckedAutomatedTransferStatusAction(
                                        new AutomatedTransferStatusResponsePayload(site, response.status,
                                                response.currentStep, response.totalSteps)));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                                AutomatedTransferError error = new AutomatedTransferError(
                                        networkError.apiError, networkError.message);
                                mDispatcher.dispatch(SiteActionBuilder.newCheckedAutomatedTransferStatusAction(
                                        new AutomatedTransferStatusResponsePayload(site, error)));
                            }
                        });
        add(request);
    }

    public void completeQuickStart(@NonNull final SiteModel site, String variant) {
        String url = WPCOMREST.sites.site(site.getSiteId()).mobile_quick_start.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("variant", variant);
        final WPComGsonRequest<QuickStartCompletedResponse> request = WPComGsonRequest
                .buildPostRequest(url, params, QuickStartCompletedResponse.class,
                        new Listener<QuickStartCompletedResponse>() {
                            @Override
                            public void onResponse(QuickStartCompletedResponse response) {
                                mDispatcher.dispatch(SiteActionBuilder.newCompletedQuickStartAction(
                                         new QuickStartCompletedResponsePayload(site, response.success)));
                            }
                        },
                        new WPComErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull WPComGsonNetworkError networkError) {
                                QuickStartError error = new QuickStartError(
                                        QuickStartErrorType.GENERIC_ERROR, networkError.message);

                                QuickStartCompletedResponsePayload payload =
                                        new QuickStartCompletedResponsePayload(site, false);
                                payload.error = error;

                                mDispatcher.dispatch(SiteActionBuilder.newCompletedQuickStartAction(payload));
                            }
                        });
        add(request);
    }

    public void fetchAccessCookie(final SiteModel site) {
        Map<String, String> params = new HashMap<>();
        String url = WPCOMV2.sites.site(site.getSiteId()).atomic_auth_proxy.read_access_cookies.getUrl();
        final WPComGsonRequest<PrivateAtomicCookieResponse> request = WPComGsonRequest.buildGetRequest(url, params,
                PrivateAtomicCookieResponse.class,
                new Listener<PrivateAtomicCookieResponse>() {
                    @Override
                    public void onResponse(PrivateAtomicCookieResponse response) {
                        if (response != null) {
                            mDispatcher.dispatch(SiteActionBuilder
                                    .newFetchedPrivateAtomicCookieAction(
                                            new FetchedPrivateAtomicCookiePayload(site, response)));
                        } else {
                            AppLog.e(T.API, "Failed to fetch private atomic cookie for " + site.getUrl());
                            FetchedPrivateAtomicCookiePayload payload = new FetchedPrivateAtomicCookiePayload(
                                    site, null);
                            payload.error = new PrivateAtomicCookieError(
                                    AccessCookieErrorType.INVALID_RESPONSE, "Empty response");
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedPrivateAtomicCookieAction(payload));
                        }
                    }
                },
                new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        PrivateAtomicCookieError cookieError = new PrivateAtomicCookieError(
                                AccessCookieErrorType.GENERIC_ERROR, error.message);
                        FetchedPrivateAtomicCookiePayload payload = new FetchedPrivateAtomicCookiePayload(site, null);
                        payload.error = cookieError;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPrivateAtomicCookieAction(payload));
                    }
                }
                                                                                                      );
        add(request);
    }

    // Utils

    private SiteModel siteResponseToSiteModel(SiteWPComRestResponse from) {
        SiteModel site = new SiteModel();
        site.setSiteId(from.ID);
        site.setUrl(from.URL);
        site.setName(StringEscapeUtils.unescapeHtml4(from.name));
        site.setDescription(StringEscapeUtils.unescapeHtml4(from.description));
        site.setIsJetpackConnected(from.jetpack);
        site.setIsJetpackInstalled(from.jetpack);
        site.setIsVisible(from.visible);
        site.setIsPrivate(from.is_private);
        site.setIsComingSoon(from.is_coming_soon);
        // Depending of user's role, options could be "hidden", for instance an "Author" can't read site options.
        if (from.options != null) {
            site.setIsFeaturedImageSupported(from.options.featured_images_enabled);
            site.setIsVideoPressSupported(from.options.videopress_enabled);
            site.setIsAutomatedTransfer(from.options.is_automated_transfer);
            site.setIsWpComStore(from.options.is_wpcom_store);
            site.setHasWooCommerce(from.options.woocommerce_is_active);
            site.setAdminUrl(from.options.admin_url);
            site.setLoginUrl(from.options.login_url);
            site.setTimezone(from.options.gmt_offset);
            site.setFrameNonce(from.options.frame_nonce);
            site.setUnmappedUrl(from.options.unmapped_url);
            site.setJetpackVersion(from.options.jetpack_version);
            site.setSoftwareVersion(from.options.software_version);
            site.setIsWPComAtomic(from.options.is_wpcom_atomic);
            site.setShowOnFront(from.options.show_on_front);
            site.setPageOnFront(from.options.page_on_front);
            site.setPageForPosts(from.options.page_for_posts);

            try {
                site.setMaxUploadSize(Long.valueOf(from.options.max_upload_size));
            } catch (NumberFormatException e) {
                // Do nothing - the value probably wasn't set ('false'), but we don't want to overwrite any existing
                // value we stored earlier, as /me/sites/ and /sites/$site/ can return different responses for this
            }

            // Set the memory limit for media uploads on the site. Normally, this is just WP_MAX_MEMORY_LIMIT,
            // but it's possible for a site to have its php memory_limit > WP_MAX_MEMORY_LIMIT, and have
            // WP_MEMORY_LIMIT == memory_limit, in which WP_MEMORY_LIMIT reflects the real limit for media uploads.
            long wpMemoryLimit = StringUtils.stringToLong(from.options.wp_memory_limit);
            long wpMaxMemoryLimit = StringUtils.stringToLong(from.options.wp_max_memory_limit);
            if (wpMemoryLimit > 0 || wpMaxMemoryLimit > 0) {
                // Only update the value if we received one from the server - otherwise, the original value was
                // probably not set ('false'), but we don't want to overwrite any existing value we stored earlier,
                // as /me/sites/ and /sites/$site/ can return different responses for this
                site.setMemoryLimit(Math.max(wpMemoryLimit, wpMaxMemoryLimit));
            }
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
        if (from.quota != null) {
            site.setSpaceAvailable(from.quota.space_available);
            site.setSpaceAllowed(from.quota.space_allowed);
            site.setSpaceUsed(from.quota.space_used);
            site.setSpacePercentUsed(from.quota.percent_used);
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

    private ConnectSiteInfoPayload connectSiteInfoFromResponse(String url, ConnectSiteInfoResponse response) {
        ConnectSiteInfoPayload info = new ConnectSiteInfoPayload(url, null);
        info.url = url;
        info.exists = response.exists;
        info.hasJetpack = response.hasJetpack;
        info.isJetpackActive = response.isJetpackActive;
        info.isJetpackConnected = response.isJetpackConnected;
        info.isWordPress = response.isWordPress;
        info.isWPCom = response.isWordPressDotCom; // CHECKSTYLE IGNORE
        info.urlAfterRedirects = response.urlAfterRedirects;
        return info;
    }

    private DomainAvailabilityResponsePayload responseToDomainAvailabilityPayload(DomainAvailabilityResponse response) {
        DomainAvailabilityStatus status = DomainAvailabilityStatus.fromString(response.getStatus());
        DomainMappabilityStatus mappable = DomainMappabilityStatus.fromString(response.getMappable());
        boolean supportsPrivacy = response.getSupports_privacy();
        return new DomainAvailabilityResponsePayload(status, mappable, supportsPrivacy);
    }
}
