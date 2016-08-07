package org.wordpress.android.fluxc.network.xmlrpc.site;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.xmlrpc.BaseXMLRPCClient;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPC;
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest;
import org.wordpress.android.fluxc.store.SiteStore.UpdatePostFormatsPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
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

    public void pullSites(final String xmlrpcUrl, final String username, final String password) {
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
                            // TODO: do nothing or dispatch error?
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                        // TODO: Error, dispatch network error
                    }
                }
        );
        add(request);
    }

    public static final Map<String, String> blogOptionsXMLRPCParameters = new HashMap<String, String>();

    static {
        blogOptionsXMLRPCParameters.put("software_version", "software_version");
        blogOptionsXMLRPCParameters.put("post_thumbnail", "post_thumbnail");
        blogOptionsXMLRPCParameters.put("jetpack_client_id", "jetpack_client_id");
        blogOptionsXMLRPCParameters.put("blog_public", "blog_public");
        blogOptionsXMLRPCParameters.put("home_url", "home_url");
        blogOptionsXMLRPCParameters.put("admin_url", "admin_url");
        blogOptionsXMLRPCParameters.put("login_url", "login_url");
        blogOptionsXMLRPCParameters.put("blog_title", "blog_title");
        blogOptionsXMLRPCParameters.put("time_zone", "time_zone");
    }

    public void pullSite(final SiteModel site) {
        List<Object> params = new ArrayList<>(2);
        params.add(site.getSiteId());
        params.add(site.getUsername());
        params.add(site.getPassword());
        params.add(blogOptionsXMLRPCParameters);
        final XMLRPCRequest request = new XMLRPCRequest(
                site.getXmlRpcUrl(), XMLRPC.GET_OPTIONS, params,
                new Listener<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        SiteModel updatedSite = updateSiteFromOptions(response, site);
                        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(updatedSite));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
                    }
                }
        );
        add(request);
    }

    // TODO: rename s/pull/fetch/ methods in this file
    public void pullPostFormats(final SiteModel site) {
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
                        mDispatcher.dispatch(SiteActionBuilder.newUpdatePostFormatsAction(new
                                UpdatePostFormatsPayload(site, postFormats)));
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(T.API, "Volley error", error);
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
        SitesModel sites = new SitesModel();
        for (Object siteObject: responseArray) {
            if (!(siteObject instanceof HashMap)) {
                continue;
            }
            HashMap<String, ?> siteMap = (HashMap<String, ?>) siteObject;
            SiteModel site = new SiteModel();
            // From the response
            site.setDotOrgSiteId(Integer.parseInt((String) siteMap.get("blogid")));
            site.setName((String) siteMap.get("blogName"));
            // TODO: set a canonical URL here
            site.setUrl((String) siteMap.get("url"));
            site.setLoginUrl((String) siteMap.get("login_url"));
            site.setXmlRpcUrl((String) siteMap.get("xmlrpc"));
            site.setIsAdmin((Boolean) siteMap.get("isAdmin"));
            site.setIsVisible(true);
            // TODO: siteMap.get("isPrimary")

            // From what we know about the host
            site.setIsWPCom(false);
            site.setUsername(username);
            site.setPassword(password);
            sites.add(site);
        }

        if (sites.isEmpty()) {
            return null;
        }

        return sites;
    }

    private SiteModel updateSiteFromOptions(Object response, SiteModel oldModel) {
        Map<?, ?> blogOptions = (Map<?, ?>) response;
        oldModel.setName(getOption(blogOptions, "blog_title", String.class));
        // TODO: set a canonical URL here
        oldModel.setUrl(getOption(blogOptions, "home_url", String.class));
        oldModel.setSoftwareVersion(getOption(blogOptions, "software_version", String.class));
        Boolean post_thumbnail = getOption(blogOptions, "post_thumbnail", Boolean.class);
        oldModel.setIsFeaturedImageSupported((post_thumbnail != null) && post_thumbnail);
        oldModel.setTimezone(getOption(blogOptions, "time_zone", String.class));
        long dotComIdForJetpack = Long.valueOf(getOption(blogOptions, "jetpack_client_id", String.class));
        oldModel.setSiteId(dotComIdForJetpack);
        if (dotComIdForJetpack != 0) {
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
