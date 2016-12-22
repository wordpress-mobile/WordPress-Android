package org.wordpress.android.fluxc.network.xmlrpc.site;

import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

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
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.store.SiteStore.FetchedPostFormatsPayload;
import org.wordpress.android.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteXMLRPCClient extends BaseXMLRPCClient {
    //
    // Site fetch request keys
    //
    public static final String SOFTWARE_VERSION_KEY = "software_version";
    public static final String POST_THUMBNAIL_KEY = "post_thumbnail";
    public static final String DEFAULT_COMMENT_STATUS_KEY = "default_comment_status";
    public static final String JETPACK_CLIENT_ID_KEY = "jetpack_client_id";
    public static final String SITE_PUBLIC_KEY = "blog_public";
    public static final String HOME_URL_KEY = "home_url";
    public static final String ADMIN_URL_KEY = "admin_url";
    public static final String LOGIN_URL_KEY = "login_url";
    public static final String SITE_TITLE_KEY = "blog_title";
    public static final String TIME_ZONE_KEY = "time_zone";

    //
    // Sites response keys
    //
    public static final String SITE_ID_KEY = "blogid";
    public static final String SITE_NAME_KEY = "blogName";
    public static final String SITE_URL_KEY = "url";
    public static final String SITE_XMLRPC_URL_KEY = "xmlrpc";
    public static final String SITE_ADMIN_KEY = "isAdmin";

    public SiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void fetchSites(final String xmlrpcUrl, final String username, final String password) {
        List<Object> params = new ArrayList<>(2);
        params.add(username);
        params.add(password);
        final XMLRPCRequest request = new XMLRPCRequest(
                xmlrpcUrl, XMLRPC.GET_USERS_SITES, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SitesModel sites = sitesResponseToSitesModel(response, username, password);
                        if (sites != null) {
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(sites));
                        } else {
                            sites = new SitesModel();
                            sites.error = new BaseNetworkError(GenericErrorType.INVALID_RESPONSE);
                            mDispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(sites));
                        }
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        SitesModel sites = new SitesModel();
                        sites.error = error;
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSitesAction(sites));
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
                SOFTWARE_VERSION_KEY, POST_THUMBNAIL_KEY, DEFAULT_COMMENT_STATUS_KEY, JETPACK_CLIENT_ID_KEY,
                SITE_PUBLIC_KEY, HOME_URL_KEY, ADMIN_URL_KEY, LOGIN_URL_KEY, SITE_TITLE_KEY, TIME_ZONE_KEY });
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_OPTIONS, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SiteModel updatedSite = updateSiteFromOptions(response, site);
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(updatedSite));
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
                        List<PostFormatModel> postFormats = responseToPostFormats(response);
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

    private SitesModel sitesResponseToSitesModel(Object response, String username, String password) {
        if (!(response instanceof Object[])) {
            return null;
        }
        Object[] responseArray = (Object[]) response;
        List<SiteModel> siteArray = new ArrayList<>();
        for (Object siteObject: responseArray) {
            if (!(siteObject instanceof HashMap)) {
                continue;
            }
            HashMap<String, ?> siteMap = (HashMap<String, ?>) siteObject;
            SiteModel site = new SiteModel();
            // TODO: use MapUtils.getX(map,"", defaultValue) here
            site.setSelfHostedSiteId(Integer.parseInt((String) siteMap.get(SITE_ID_KEY)));
            site.setName((String) siteMap.get(SITE_NAME_KEY));
            site.setUrl((String) siteMap.get(SITE_URL_KEY));
            site.setXmlRpcUrl((String) siteMap.get(SITE_XMLRPC_URL_KEY));
            site.setIsSelfHostedAdmin((Boolean) siteMap.get(SITE_ADMIN_KEY));
            // Self Hosted won't be hidden
            site.setIsVisible(true);
            // From what we know about the host
            site.setIsWPCom(false);
            site.setUsername(username);
            site.setPassword(password);
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

    private SiteModel updateSiteFromOptions(Object response, SiteModel oldModel) {
        Map<?, ?> siteOptions = (Map<?, ?>) response;
        oldModel.setName(getOption(siteOptions, SITE_TITLE_KEY, String.class));
        // TODO: set a canonical URL here
        oldModel.setUrl(getOption(siteOptions, HOME_URL_KEY, String.class));
        oldModel.setSoftwareVersion(getOption(siteOptions, SOFTWARE_VERSION_KEY, String.class));
        Boolean postThumbnail = getOption(siteOptions, POST_THUMBNAIL_KEY, Boolean.class);
        oldModel.setIsFeaturedImageSupported((postThumbnail != null) && postThumbnail);
        oldModel.setDefaultCommentStatus(getOption(siteOptions, DEFAULT_COMMENT_STATUS_KEY, String.class));
        oldModel.setTimezone(getOption(siteOptions, TIME_ZONE_KEY, String.class));
        oldModel.setLoginUrl(getOption(siteOptions, LOGIN_URL_KEY, String.class));
        oldModel.setAdminUrl(getOption(siteOptions, ADMIN_URL_KEY, String.class));
        long wpComIdForJetpack = string2Long(getOption(siteOptions, JETPACK_CLIENT_ID_KEY, String.class), -1);
        oldModel.setSiteId(wpComIdForJetpack);
        // If the site is not public, it's private. Note: this field doesn't always exist.
        oldModel.setIsPrivate(false);
        if (siteOptions.containsKey(SITE_PUBLIC_KEY)) {
            Boolean isPublic = getOption(siteOptions, SITE_PUBLIC_KEY, Boolean.class);
            if (isPublic != null) {
                oldModel.setIsPrivate(!isPublic);
            }
        }
        if (wpComIdForJetpack != 0) {
            oldModel.setIsJetpack(true);
        }
        return oldModel;
    }

    private List<PostFormatModel> responseToPostFormats(Object response) {
        Map<?, ?> formatsMap = (Map<?, ?>) response;
        List<PostFormatModel> res = new ArrayList<>();
        for (Object key : formatsMap.keySet()) {
            if (!(key instanceof String)) continue;
            String skey = (String) key;
            PostFormatModel postFormat = new PostFormatModel();
            postFormat.setSlug(skey);
            postFormat.setDisplayName(MapUtils.getMapStr(formatsMap, skey));
            res.add(postFormat);
        }
        return res;
    }

    private <T> T getOption(Map<?, ?> siteOptions, String key, Class<T> type) {
        Map<?, ?> map = (HashMap<?, ?>) siteOptions.get(key);
        if (map != null) {
            if (type == String.class) {
                return (T) MapUtils.getMapStr(map, "value");
            } else if (type == Boolean.class) {
                return (T) Boolean.valueOf(MapUtils.getMapBool(map, "value"));
            }
        }
        return null;
    }
}
