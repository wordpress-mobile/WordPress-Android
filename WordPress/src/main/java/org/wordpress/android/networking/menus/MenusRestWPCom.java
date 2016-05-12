package org.wordpress.android.networking.menus;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wordpress.android.util.VolleyUtils.statusCodeFromVolleyError;
import static org.wordpress.android.networking.menus.MenusDataModeler.*;

/**
 * Interface for WordPress.com Menus REST API methods.
 *
 * ref: https://developer.wordpress.com/docs/api/
 */
public class MenusRestWPCom {
    public enum REST_ERROR {
        UNKNOWN_ERROR,
        AUTHENTICATION_ERROR,
        CREATE_ERROR,
        DELETE_ERROR,
        UPDATE_ERROR,
        FETCH_ERROR
    }

    public interface MenusListener {
        Context getContext();
        long getSiteId();
        void onMenusReceived(int requestId, List<MenuModel> menus);
        void onMenuCreated(int requestId, MenuModel menu);
        void onMenuDeleted(int requestId, MenuModel menu, boolean deleted);
        void onMenuUpdated(int requestId, MenuModel menu);
        void onErrorResponse(int requestId, REST_ERROR error);
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
                if (statusCode == 403) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error);
            }
        });
        return requestId;
    }

    public int updateMenu(final MenuModel menu) {
        if (menu == null) return -1;
        final int requestId = ++mRequestCounter;
        String path = formatPath(MENU_REST_PATH, String.valueOf(menu.menuId));
        Map<String, String> params = new HashMap<>();
        params.put(MENU_NAME_KEY, menu.name);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                MenuModel menu = menuFromJson(response.optJSONObject(MENU_KEY));
                mListener.onMenuUpdated(requestId, menu);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.UPDATE_ERROR;
                if (statusCode == 403) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error);
            }
        });
        return requestId;
    }

    public int fetchMenu(long menuId) {
        if (menuId <= 0) return -1;
        final int requestId = ++mRequestCounter;
        String path = formatPath(MENU_REST_PATH, String.valueOf(menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                MenuModel result = menuFromJson(response.optJSONObject(MENU_KEY));
                if (result != null) {
                    List<MenuModel> resultList = new ArrayList<>();
                    resultList.add(result);
                    mListener.onMenusReceived(requestId, resultList);
                }
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.FETCH_ERROR;
                if (statusCode == 403) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error);
            }
        });
        return requestId;
    }

    public int fetchAllMenus() {
        final int requestId = ++mRequestCounter;
        String path = formatPath(MENUS_REST_PATH, null);
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                JSONArray menusJson = response.optJSONArray(ALL_MENUS_MENUS_KEY);
                List<MenuModel> menus = new ArrayList<>();
                try {
                    for (int i = 0; i < menusJson.length(); ++i) {
                        menus.add(menuFromJson(menusJson.getJSONObject(i)));
                    }
                } catch (JSONException exception) {
                    AppLog.w(AppLog.T.API, "Error parsing All Menus REST response");
                }
                mListener.onMenusReceived(requestId, menus);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.FETCH_ERROR;
                if (statusCode == 403) error = REST_ERROR.AUTHENTICATION_ERROR;
                mListener.onErrorResponse(requestId, error);
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
            @Override public void onResponse(JSONObject response) {
                boolean deleted = response.optBoolean(DELETED_KEY, false);
                mListener.onMenuDeleted(requestId, menu, deleted);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError volleyError) {
                int statusCode = statusCodeFromVolleyError(volleyError);
                REST_ERROR error = REST_ERROR.FETCH_ERROR;
                if (statusCode == 403) error = REST_ERROR.DELETE_ERROR;
                mListener.onErrorResponse(requestId, error);
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
}
