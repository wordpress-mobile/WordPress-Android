package org.wordpress.android.fluxc.network.xmlrpc.site;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.XMLRPC;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsError;
import org.wordpress.android.fluxc.store.SiteStore.PostFormatsErrorType;
import org.wordpress.android.fluxc.utils.SiteUtils;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class SiteXMLRPCClient extends BaseXMLRPCClient {
    public SiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent,
                            HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, userAgent, httpAuthManager);
    }

    public void fetchProfile(final SiteModel site) {
        List<Object> params = new ArrayList<>(3);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());

        final XMLRPCRequest request = new XMLRPCRequest(site.getXmlRpcUrl(), XMLRPC.GET_PROFILE, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SiteModel updatedSite = profileResponseToAccountModel(response, site);
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedProfileXmlRpcAction(updatedSite));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SiteModel site = new SiteModel();
                        site.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedProfileXmlRpcAction(site));
                    }
                }
        );

        add(request);
    }

    public void fetchSites(final String xmlrpcUrl, final String username, final String password) {
        List<Object> params = new ArrayList<>(2);
        params.add(username);
        params.add(password);
        final XMLRPCRequest request = new XMLRPCRequest(
                xmlrpcUrl, XMLRPC.GET_USERS_SITES, params,
                new Listener<Object[]>() {
                    @Override
                    public void onResponse(Object[] response) {
                        SitesModel sites = sitesResponseToSitesModel(response, username, password);
                        if (sites != null) {
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesXmlRpcAction(sites));
                        } else {
                            sites = new SitesModel();
                            sites.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesXmlRpcAction(sites));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SitesModel sites = new SitesModel();
                        sites.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedSitesXmlRpcAction(sites));
                    }
                }
        );
        add(request);
    }

    public void fetchSite(final SiteModel site) {
        List<Object> params = new ArrayList<>(2);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(new String[] {
                "software_version", "post_thumbnail", "default_comment_status", "jetpack_client_id",
                "blog_public", "home_url", "admin_url", "login_url", "blog_title", "time_zone", "jetpack_user_email" });
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_OPTIONS, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SiteModel updatedSite = updateSiteFromOptions(response, site);
                        if (updatedSite != null) {
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(updatedSite));
                        } else {
                            SiteModel site = new SiteModel();
                            site.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SiteModel site = new SiteModel();
                        site.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
                    }
                }
        );
        add(request);
    }

    public void fetchPostFormats(final SiteModel site) {
        List<Object> params = new ArrayList<>(2);
        params.add(site.getSelfHostedSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_POST_FORMATS, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        List<PostFormatModel> postFormats = responseToPostFormats(response, site);
                        if (postFormats != null) {
                            FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(site, postFormats);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                        } else {
                            FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(site,
                                    Collections.<PostFormatModel>emptyList());
                            payload.error = new PostFormatsError(PostFormatsErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        PostFormatsError postFormatsError;
                        switch (error.type) {
                            case INVALID_RESPONSE:
                                postFormatsError = new PostFormatsError(PostFormatsErrorType.INVALID_RESPONSE,
                                        error.message);
                                break;
                            // TODO: what other kind of error could we get here?
                            default:
                                postFormatsError = new PostFormatsError(PostFormatsErrorType.GENERIC_ERROR,
                                        error.message);
                        }
                        FetchedPostFormatsPayload payload = new FetchedPostFormatsPayload(site,
                                Collections.<PostFormatModel>emptyList());
                        payload.error = postFormatsError;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchedPostFormatsAction(payload));
                    }
                }
        );
        add(request);
    }

    private SiteModel profileResponseToAccountModel(Object response, SiteModel site) {
        if (response == null) return null;

        Map<?, ?> userMap = (Map<?, ?>) response;
        site.setEmail(MapUtils.getMapStr(userMap, "email"));
        site.setDisplayName(MapUtils.getMapStr(userMap, "display_name"));

        return site;
    }

    private SitesModel sitesResponseToSitesModel(Object[] response, String username, String password) {
        if (response == null) return null;

        List<SiteModel> siteArray = new ArrayList<>();
        for (Object siteObject : response) {
            if (!(siteObject instanceof Map)) {
                continue;
            }
            Map<?, ?> siteMap = (Map<?, ?>) siteObject;
            SiteModel site = new SiteModel();
            site.setSelfHostedSiteId(MapUtils.getMapInt(siteMap, "blogid", 1));
            site.setName(StringEscapeUtils.unescapeHtml4(MapUtils.getMapStr(siteMap, "blogName")));
            site.setUrl(MapUtils.getMapStr(siteMap, "url"));
            site.setXmlRpcUrl(MapUtils.getMapStr(siteMap, "xmlrpc"));
            site.setIsSelfHostedAdmin(MapUtils.getMapBool(siteMap, "isAdmin"));
            // From what we know about the host
            site.setIsWPCom(false);
            site.setUsername(username);
            site.setPassword(password);
            site.setOrigin(SiteModel.ORIGIN_XMLRPC);
            siteArray.add(site);
        }

        if (siteArray.isEmpty()) {
            return null;
        }

        return new SitesModel(siteArray);
    }

    private long string2Long(String s, long defvalue) {
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return defvalue;
        }
    }

    private void setJetpackStatus(Map<?, ?> siteOptions, SiteModel oldModel) {
        // * Jetpack not installed: field "jetpack_client_id" not included in the response
        // * Jetpack installed but not activated: field "jetpack_client_id" not included in the response
        // * Jetpack installed, activated but not connected: field "jetpack_client_id" included
        //   and is "0" (boolean)
        // * Jetpack installed, activated and connected: field "jetpack_client_id" included and is correctly
        //   set to wpcom unique id eg. "1234"

        String jetpackClientIdStr = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "jetpack_client_id", "");
        long jetpackClientId = 0;
        // jetpackClientIdStr can be a boolean "0" (false), in that case we keep the default value "0".
        if (!"false".equals(jetpackClientIdStr)) {
            jetpackClientId = string2Long(jetpackClientIdStr, -1);
        }

        // Field "jetpack_client_id" not found:
        if (jetpackClientId == -1) {
            oldModel.setIsJetpackInstalled(false);
            oldModel.setIsJetpackConnected(false);
        }

        // Field "jetpack_client_id" is "0"
        if (jetpackClientId == 0) {
            oldModel.setIsJetpackInstalled(true);
            oldModel.setIsJetpackConnected(false);
        }

        // jetpack_client_id is set then it's a Jetpack connected site
        if (jetpackClientId != 0 && jetpackClientId != -1) {
            oldModel.setIsJetpackInstalled(true);
            oldModel.setIsJetpackConnected(true);
            oldModel.setSiteId(jetpackClientId);
        } else {
            oldModel.setSiteId(0);
        }

        // * Jetpack not installed: field "jetpack_user_email" not included in the response
        // * Jetpack installed but not activated: field "jetpack_user_email" not included in the response
        // * Jetpack installed, activated but not connected: field "jetpack_user_email" not included in the response
        // * Jetpack installed, activated and connected: field "jetpack_user_email" included and is correctly
        //   set to the email of the jetpack connected user
        oldModel.setJetpackUserEmail(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "jetpack_user_email", ""));
    }

    private SiteModel updateSiteFromOptions(Object response, SiteModel oldModel) {
        if (!(response instanceof Map)) {
            reportParseError(response, oldModel.getXmlRpcUrl(), Map.class);
            return null;
        }

        Map<?, ?> siteOptions = (Map<?, ?>) response;
        String siteTitle = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "blog_title", "");
        if (!siteTitle.isEmpty()) {
            oldModel.setName(StringEscapeUtils.unescapeHtml4(siteTitle));
        }

        // TODO: set a canonical URL here
        String homeUrl = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "home_url", "");
        if (!homeUrl.isEmpty()) {
            oldModel.setUrl(homeUrl);
        }

        oldModel.setSoftwareVersion(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "software_version", ""));
        oldModel.setIsFeaturedImageSupported(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "post_thumbnail", false));
        oldModel.setDefaultCommentStatus(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "default_comment_status",
                "open"));
        oldModel.setTimezone(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "time_zone", "0"));
        oldModel.setLoginUrl(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "login_url", ""));
        oldModel.setAdminUrl(XMLRPCUtils.safeGetNestedMapValue(siteOptions, "admin_url", ""));

        setJetpackStatus(siteOptions, oldModel);
        // If the site is not public, it's private. Note: this field doesn't always exist.
        boolean isPublic = XMLRPCUtils.safeGetNestedMapValue(siteOptions, "blog_public", true);
        oldModel.setIsPrivate(!isPublic);
        return oldModel;
    }

    private @Nullable List<PostFormatModel> responseToPostFormats(Object response, SiteModel site) {
        if (!(response instanceof Map)) {
            reportParseError(response, site.getXmlRpcUrl(), Map.class);
            return null;
        }

        return SiteUtils.getValidPostFormatsOrNull((Map) response);
    }
}
