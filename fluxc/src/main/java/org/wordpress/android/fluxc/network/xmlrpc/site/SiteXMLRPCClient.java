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
    public SiteXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        super(dispatcher, requestQueue, accessToken, userAgent, httpAuthManager);
    }

    public void fetchSites(final String xmlrpcUrl, final String username, final String password) {
        List<Object> params = new ArrayList<>(2);
        params.add(username);
        params.add(password);
        final XMLRPCRequest request = new XMLRPCRequest(
                xmlrpcUrl, XMLRPC.GET_USERS_BLOGS, params,
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

    public static final Map<String, String> XMLRPC_BLOG_OPTIONS = new HashMap<String, String>();

    static {
        XMLRPC_BLOG_OPTIONS.put("software_version", "software_version");
        XMLRPC_BLOG_OPTIONS.put("post_thumbnail", "post_thumbnail");
        XMLRPC_BLOG_OPTIONS.put("jetpack_client_id", "jetpack_client_id");
        XMLRPC_BLOG_OPTIONS.put("blog_public", "blog_public");
        XMLRPC_BLOG_OPTIONS.put("home_url", "home_url");
        XMLRPC_BLOG_OPTIONS.put("admin_url", "admin_url");
        XMLRPC_BLOG_OPTIONS.put("login_url", "login_url");
        XMLRPC_BLOG_OPTIONS.put("blog_title", "blog_title");
        XMLRPC_BLOG_OPTIONS.put("time_zone", "time_zone");
    }

    public void fetchSite(final SiteModel site) {
        List<Object> params = new ArrayList<>(2);
        params.add(site.getSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(XMLRPC_BLOG_OPTIONS);
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
        params.add(site.getSiteId());
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
            site.setSelfHostedSiteId(Integer.parseInt((String) siteMap.get("blogid")));
            site.setName((String) siteMap.get("blogName"));
            site.setUrl((String) siteMap.get("url"));
            site.setXmlRpcUrl((String) siteMap.get("xmlrpc"));
            site.setIsAdmin((Boolean) siteMap.get("isAdmin"));
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
        Map<?, ?> blogOptions = (Map<?, ?>) response;
        oldModel.setName(getOption(blogOptions, "blog_title", String.class));
        // TODO: set a canonical URL here
        oldModel.setUrl(getOption(blogOptions, "home_url", String.class));
        oldModel.setSoftwareVersion(getOption(blogOptions, "software_version", String.class));
        Boolean postThumbnail = getOption(blogOptions, "post_thumbnail", Boolean.class);
        oldModel.setIsFeaturedImageSupported((postThumbnail != null) && postThumbnail);
        oldModel.setTimezone(getOption(blogOptions, "time_zone", String.class));
        oldModel.setLoginUrl(getOption(blogOptions, "login_url", String.class));
        oldModel.setAdminUrl(getOption(blogOptions, "admin_url", String.class));
        long wpComIdForJetpack = string2Long(getOption(blogOptions, "jetpack_client_id", String.class), -1);
        oldModel.setSiteId(wpComIdForJetpack);
        // If the blog is not public, it's private. Note: this field doesn't always exist.
        oldModel.setIsPrivate(false);
        if (blogOptions.containsKey("blog_public")) {
            Boolean isPublic = getOption(blogOptions, "blog_public", Boolean.class);
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

    private <T> T getOption(Map<?, ?> blogOptions, String key, Class<T> type) {
        Map<?, ?> map = (HashMap<?, ?>) blogOptions.get(key);
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
