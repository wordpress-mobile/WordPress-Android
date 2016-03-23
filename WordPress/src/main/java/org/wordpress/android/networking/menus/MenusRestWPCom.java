package org.wordpress.android.networking.menus;

import android.content.Context;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.MenuLocationTable;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.util.AppLog;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wordpress.android.util.VolleyUtils.statusCodeFromVolleyError;
import static org.wordpress.android.networking.menus.MenusDataModeler.*;

/**
 */
public class MenusRestWPCom {
    public interface IMenusDelegate {
        Context getContext();
        long getSiteId();
        void onAllMenusReceived(int statusCode, String siteId, List<MenuModel> menus, Map<Long, MenuItemModel> items, Map<String, MenuLocationModel> locations);
    }

    private static final String MENUS_REST_PATH = "/sites/%s/menus";

    public static void fetchAllMenus(final IMenusDelegate delegate) {
        if (delegate == null) return;
        String path = String.format(MENUS_REST_PATH, String.valueOf(delegate.getSiteId()));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                handleAllMenusResponse(delegate, response);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                delegate.onAllMenusReceived(statusCodeFromVolleyError(error), String.valueOf(delegate.getSiteId()), null, null, null);
            }
        });
    }

    private static void handleAllMenusResponse(IMenusDelegate delegate, JSONObject response) {
        if (delegate == null || delegate.getContext() == null || response == null) return;

        try {
            // Get locations from response and update database
            if (response.has(ALL_MENUS_LOCATIONS_KEY)) {
                JSONArray locationsArray = response.getJSONArray(ALL_MENUS_LOCATIONS_KEY);
                List<MenuLocationModel> allLocations = menuLocationsFromJson(locationsArray);

                if (allLocations != null && allLocations.size() > 0) {
                    MenuLocationTable.deleteAllLocations();
                    for (MenuLocationModel location : allLocations) {
                        MenuLocationTable.saveMenuLocation(location);
                    }
                }
            }

            List<MenuLocationModel> dbLocations = MenuLocationTable.getAllMenuLocations();
            if (dbLocations != null) {
                delegate.onAllMenusReceived(HttpURLConnection.HTTP_OK, String.valueOf(delegate.getSiteId()), null, null, null);
            }
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error reading All Menus JSON response: " + exception);
        }
    }
}
