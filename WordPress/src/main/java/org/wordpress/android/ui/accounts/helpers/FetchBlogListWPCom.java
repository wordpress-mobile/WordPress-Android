package org.wordpress.android.ui.accounts.helpers;

import android.content.Context;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.VolleyUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchBlogListWPCom extends FetchBlogListAbstract {

    private Context mContext;

    public FetchBlogListWPCom(Context context) {
        super(null, null);
        mContext = context;
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

                    // store capabilities as a json string
                    site.put("capabilities", jsonSite.getString("capabilities"));

                    JSONObject plan = jsonSite.getJSONObject("plan");
                    site.put("planID", plan.get("product_id"));
                    site.put("plan_product_name_short", plan.get("product_name_short"));

                    JSONObject jsonLinks = JSONUtils.getJSONChild(jsonSite, "meta/links");
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
        WordPress.getRestClientUtils().get("me/sites", RestClientUtils.getRestLocaleParams(mContext), null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    List<Map<String, Object>> userBlogListReceiver = convertJSONObjectToSiteList(response, true);
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
