package org.wordpress.android.networking.menus;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.MenuLocationTable;
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
        void onMenuReceived(int statusCode, long menuId);
        void onMenusReceived(int statusCode, List<Long> menuIds);
        void onMenuCreated(int statusCode, long menuId);
        void onMenuDeleted(int statusCode, long menuId, boolean deleted);
        void onMenuUpdated(int statusCode, MenuModel menu);
    }

    private static final String MENU_REST_PATH = "/sites/%s/menus/%s";
    private static final String MENUS_REST_PATH = "/sites/%s/menus";
    private static final String CREATE_MENU_REST_PATH = "/sites/%s/menus/new";
    private static final String DELETE_MENU_REST_PATH = "/sites/%s/menus/%s/delete";

    private static final String DELETED_KEY = "deleted";

    private final IMenusDelegate mDelegate;

    public MenusRestWPCom(IMenusDelegate delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("IMenusDelegate cannot be null");
        }
        mDelegate = delegate;
    }

    public void fetchMenu(long menuId) {
        String siteId = String.valueOf(mDelegate.getSiteId());
        String path = String.format(MENU_REST_PATH, siteId, String.valueOf(menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                handleGetMenuResponse(response);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
            }
        });
    }

    public void fetchAllMenus() {
        String path = String.format(MENUS_REST_PATH, String.valueOf(mDelegate.getSiteId()));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                handleAllMenusResponse(response);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenusReceived(statusCodeFromVolleyError(error), null);
            }
        });
    }

    public void createMenu(String menuName) {
        if (TextUtils.isEmpty(menuName)) return;
        String path = String.format(CREATE_MENU_REST_PATH, String.valueOf(mDelegate.getSiteId()));
        Map<String, String> params = new HashMap<>();
        params.put(MENU_NAME_KEY, menuName);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                handleCreateMenuResponse(response);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
            }
        });
    }

    public void updateMenu(MenuModel menu) {
        if (menu == null) return;
        String siteId = String.valueOf(mDelegate.getSiteId());
        String path = String.format(MENU_REST_PATH, siteId, String.valueOf(menu.menuId));
        Map<String, String> params = new HashMap<>();
        params.put(MENU_NAME_KEY, menu.name);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
            }
        });
    }

    public void deleteMenu(final long menuId) {
        String siteId = String.valueOf(mDelegate.getSiteId());
        String path = String.format(DELETE_MENU_REST_PATH, siteId, String.valueOf(menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                boolean deleted = response.optBoolean(DELETED_KEY, false);
                mDelegate.onMenuDeleted(HttpURLConnection.HTTP_OK, menuId, deleted);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenuDeleted(statusCodeFromVolleyError(error), menuId, false);
            }
        });
    }

    private void handleCreateMenuResponse(JSONObject response) {
        if (response == null) return;

        long menuId = response.optLong(MENU_ID_KEY);
        if (menuId > 0) {
        }
    }

    private void handleDeleteMenuResponse(JSONObject response) {
    }

    private void handleGetMenuResponse(JSONObject response) {
    }

    private void handleAllMenusResponse(JSONObject response) {
        if (mDelegate.getContext() == null || response == null) return;

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
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error reading All Menus JSON response: " + exception);
        }
    }
}
