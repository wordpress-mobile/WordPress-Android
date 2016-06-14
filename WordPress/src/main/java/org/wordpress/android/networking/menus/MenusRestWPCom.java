package org.wordpress.android.networking.menus;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wordpress.android.util.VolleyUtils.statusCodeFromVolleyError;
import static org.wordpress.android.util.VolleyUtils.messageStringFromVolleyError;
import static org.wordpress.android.networking.menus.MenusDataModeler.*;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;

/**
 * Interface for WordPress.com Menus REST API methods.
 *
 * ref: https://developer.wordpress.com/docs/api/
 */
public class MenusRestWPCom {
    public enum REST_ERROR {
        UNKNOWN_ERROR,
        AUTHENTICATION_ERROR,
        RESERVED_ID_ERROR,
        CREATE_ERROR,
        DELETE_ERROR,
        UPDATE_ERROR,
        FETCH_ERROR
    }

    public interface MenusListener {
        Context getContext();
        long getSiteId();
        void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations);
        void onMenuCreated(int requestId, MenuModel menu);
        void onMenuDeleted(int requestId, MenuModel menu, boolean deleted);
        void onMenuUpdated(int requestId, MenuModel menu);
        void onErrorResponse(int requestId, REST_ERROR error, String errorMessage);
    }

    private static final String MENU_REST_PATH = "/sites/%s/menus/%s";
    private static final String MENUS_REST_PATH = "/sites/%s/menus";
    private static final String CREATE_MENU_REST_PATH = "/sites/%s/menus/new";
    private static final String DELETE_MENU_REST_PATH = "/sites/%s/menus/%s/delete";

    //
    // JSON keys for fetchAllMenus response object
    //
    public static final String MENU_KEY = "menu";
    public static final String ALL_MENUS_MENUS_KEY = "menus";
    public static final String ALL_MENUS_LOCATIONS_KEY = "locations";

    private static final String DELETED_KEY = "deleted";

    private final MenusListener mListener;

    private int mRequestCounter = 0;

    public MenusRestWPCom(MenusListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("MenusListener cannot be null");
        }
        mListener = listener;
    }

    public int createMenu(final MenuModel menu) {
        // a name string is required for the REST call
        if (menu == null || TextUtils.isEmpty(menu.name)) return -1;
        final int requestId = ++mRequestCounter;
        String path = formatPath(CREATE_MENU_REST_PATH, null);
        Map<String, String> params = new HashMap<>();
        params.put(MENU_NAME_KEY, menu.name);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                menu.menuId = response.optLong(MENU_ID_KEY);
                mListener.onMenuCreated(requestId, menu);
                // TODO: call updateMenu if the menu has any non-default fields
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.CREATE_ERROR;
                if (statusCode == HTTP_FORBIDDEN) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error, messageStringFromVolleyError(volleyError));
            }
        });
        return requestId;
    }

    public int updateMenu(final MenuModel menu) {
        if (menu == null || menu.isSpecialMenu()) return -1;
        final int requestId = ++mRequestCounter;
        String path = formatPath(MENU_REST_PATH, String.valueOf(menu.menuId));
        JSONObject params = new JSONObject();
        try {
            params.put(MENU_NAME_KEY, menu.name);
            params.put(MENU_DESCRIPTION_KEY, menu.details);
            params.put(MENU_LOCATIONS_KEY, serializeLocations(menu.locations));
        } catch (JSONException e) {
            AppLog.d(AppLog.T.MENUS, "failed to serialize menus update params, aborting REST call");
            return -1;
        }
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                MenuModel result = menuFromJson(response.optJSONObject(MENU_KEY), menu.locations, mListener.getSiteId());
                mListener.onMenuUpdated(requestId, result);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.UPDATE_ERROR;
                if (statusCode == HTTP_FORBIDDEN) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error, messageStringFromVolleyError(volleyError));
            }
        });
        return requestId;
    }

    public int fetchMenu(long menuId) {
        if (menuId < 0) return -1;
        final int requestId = ++mRequestCounter;
        String path = formatPath(MENU_REST_PATH, String.valueOf(menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                MenuModel result = menuFromJson(response.optJSONObject(MENU_KEY), null, mListener.getSiteId());
                List<MenuModel> resultList = new ArrayList<>();
                if (result != null) resultList.add(result);
                mListener.onMenusReceived(requestId, resultList, null);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.FETCH_ERROR;
                if (statusCode == HTTP_FORBIDDEN) error = REST_ERROR.AUTHENTICATION_ERROR;
                else if (statusCode == HTTP_BAD_REQUEST) error = REST_ERROR.RESERVED_ID_ERROR;
                mListener.onErrorResponse(requestId, error, messageStringFromVolleyError(volleyError));
            }
        });
        return requestId;
    }

    public int fetchAllMenus() {
        final int requestId = ++mRequestCounter;
        String path = formatPath(MENUS_REST_PATH, null);
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                /* first we get all locations */
                JSONArray locationsJson = response.optJSONArray(ALL_MENUS_LOCATIONS_KEY);
                List<MenuLocationModel> locations = menuLocationsFromJson(locationsJson, mListener.getSiteId());

                /* now we get all menus */
                JSONArray menusJson = response.optJSONArray(ALL_MENUS_MENUS_KEY);
                List<MenuModel> menus = new ArrayList<>();
                try {
                    for (int i = 0; i < menusJson.length(); ++i) {
                        menus.add(menuFromJson(menusJson.getJSONObject(i), locations, mListener.getSiteId()));
                    }
                } catch (JSONException exception) {
                    AppLog.w(AppLog.T.API, "Error parsing All Menus REST response");
                }

                mListener.onMenusReceived(requestId, menus, locations);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.FETCH_ERROR;
                if (statusCode == HTTP_FORBIDDEN) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error, messageStringFromVolleyError(volleyError));
            }
        });
        return requestId;
    }

    public int deleteMenu(final MenuModel menu) {
        if (menu == null) return -1;
        final int requestId = ++mRequestCounter;
        String path = formatPath(DELETE_MENU_REST_PATH, String.valueOf(menu.menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                boolean deleted = response.optBoolean(DELETED_KEY, false);
                mListener.onMenuDeleted(requestId, menu, deleted);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.DELETE_ERROR;
                if (statusCode == HTTP_FORBIDDEN) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error, messageStringFromVolleyError(volleyError));
            }
        });
        return requestId;
    }

    private String formatPath(String base, String menuId) {
        if (!TextUtils.isEmpty(menuId)) {
            return String.format(base, String.valueOf(mListener.getSiteId()), menuId);
        }
        return String.format(base, String.valueOf(mListener.getSiteId()));
    }

    @NonNull
    private JSONArray serializeLocations(List<MenuLocationModel> locations) {
        JSONArray locationsArray = new JSONArray();
        if (locations != null && locations.size() > 0) {
            for (MenuLocationModel location : locations) {
                if (location != null) locationsArray.put(location.name);
            }
        }
        return locationsArray;
    }
}
