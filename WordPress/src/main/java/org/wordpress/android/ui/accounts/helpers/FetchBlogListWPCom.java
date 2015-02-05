package org.wordpress.android.ui.accounts.helpers;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.VolleyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchBlogListWPCom extends FetchBlogListAbstract {
    public FetchBlogListWPCom() {
        super(null, null);
    }

    protected void fetchBlogList(final Callback callback) {
        getUsersBlogsRequestREST(callback);
    }

    private List<Map<String, Object>> convertJSONObjectToSiteList(JSONObject jsonObject, boolean keepJetpackSites) {
        List<Map<String, Object>> sites = new ArrayList<Map<String, Object>>();
        JSONArray jsonSites = jsonObject.optJSONArray("sites");
        if (jsonSites != null) {
            for (int i = 0; i < jsonSites.length(); i++) {
                JSONObject jsonSite = jsonSites.optJSONObject(i);
                Map<String, Object> site = new HashMap<String, Object>();
                try {
                    // skip if it's a jetpack site and we don't keep them
                    if (jsonSite.getBoolean("jetpack") && !keepJetpackSites) {
                        continue;
                    }
                    site.put("blogName", jsonSite.get("name"));
                    site.put("url", jsonSite.get("URL"));
                    site.put("blogid", jsonSite.get("ID"));
                    site.put("isAdmin", jsonSite.get("user_can_manage"));
                    site.put("isVisible", jsonSite.get("visible"));
                    JSONObject jsonLinks = JSONUtil.getJSONChild(jsonSite, "meta/links");
                    if (jsonLinks != null) {
                        site.put("xmlrpc", jsonLinks.getString("xmlrpc"));
                        sites.add(site);
                    } else {
                        AppLog.e(T.NUX, "xmlrpc links missing from the me/sites REST response");
                    }
                } catch (JSONException e) {
                    AppLog.e(T.NUX, e);
                }
            }
        }
        return sites;
    }

    private void getUsersBlogsRequestREST(final FetchBlogListAbstract.Callback callback) {
        WordPress.getRestClientUtils().get("me/sites", new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    List<Map<String, Object>> userBlogListReceiver = convertJSONObjectToSiteList(response, false);
                    callback.onSuccess(userBlogListReceiver);
                } else {
                    callback.onSuccess(null);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                JSONObject errorObject = VolleyUtils.volleyErrorToJSON(volleyError);
                callback.onError(LoginWPCom.restLoginErrorToMsgId(errorObject), false, false, false, "");
            }
        });
    }
}
