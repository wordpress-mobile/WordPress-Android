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

import java.net.HttpURLConnection;
import java.util.ArrayList;
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
        void onMenuReceived(int statusCode, MenuModel menu);
        void onMenusReceived(int statusCode, List<MenuModel> menus);
        void onMenuCreated(int statusCode, MenuModel menu);
        void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted);
        void onMenuUpdated(int statusCode, MenuModel menu);
    }

    private static final String MENU_REST_PATH = "/sites/%s/menus/%s";
    private static final String MENUS_REST_PATH = "/sites/%s/menus";
    private static final String CREATE_MENU_REST_PATH = "/sites/%s/menus/new";
    private static final String DELETE_MENU_REST_PATH = "/sites/%s/menus/%s/delete";

    //
    // JSON keys for fetchAllMenus response object
    //
    public static final String ALL_MENUS_MENUS_KEY = "menus";
    public static final String ALL_MENUS_LOCATIONS_KEY = "locations";

    private static final String DELETED_KEY = "deleted";

    private final IMenusDelegate mDelegate;

    public MenusRestWPCom(IMenusDelegate delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("IMenusDelegate cannot be null");
        }
        mDelegate = delegate;
    }

    public boolean fetchMenu(long menuId) {
        String siteId = String.valueOf(mDelegate.getSiteId());
        String path = String.format(MENU_REST_PATH, siteId, String.valueOf(menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                mDelegate.onMenuReceived(HttpURLConnection.HTTP_OK, menuFromJson(response));
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenuReceived(statusCodeFromVolleyError(error), null);
            }
        });
        return true;
    }

    public boolean fetchAllMenus() {
        String path = String.format(MENUS_REST_PATH, String.valueOf(mDelegate.getSiteId()));
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
                }
                mDelegate.onMenusReceived(HttpURLConnection.HTTP_OK, menus);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenusReceived(statusCodeFromVolleyError(error), null);
            }
        });
        return true;
    }

    public boolean createMenu(final MenuModel menu) {
        if (menu == null || TextUtils.isEmpty(menu.name)) return false;
        String path = String.format(CREATE_MENU_REST_PATH, String.valueOf(mDelegate.getSiteId()));
        Map<String, String> params = new HashMap<>();
        params.put(MENU_NAME_KEY, menu.name);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                menu.menuId = response.optLong(MENU_ID_KEY);
                mDelegate.onMenuCreated(HttpURLConnection.HTTP_OK, menu);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenuCreated(statusCodeFromVolleyError(error), menu);
            }
        });
        return true;
    }

    public boolean updateMenu(final MenuModel menu) {
        if (menu == null) return false;
        String siteId = String.valueOf(mDelegate.getSiteId());
        String path = String.format(MENU_REST_PATH, siteId, String.valueOf(menu.menuId));
        Map<String, String> params = new HashMap<>();
        params.put(MENU_NAME_KEY, menu.name);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                mDelegate.onMenuUpdated(HttpURLConnection.HTTP_OK, menuFromJson(response));
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenuUpdated(statusCodeFromVolleyError(error), menu);
            }
        });
        return true;
    }

    public boolean deleteMenu(final MenuModel menu) {
        if (menu == null) return false;
        String siteId = String.valueOf(mDelegate.getSiteId());
        String path = String.format(DELETE_MENU_REST_PATH, siteId, String.valueOf(menu.menuId));
        Map<String, String> params = new HashMap<>();
        WordPress.getRestClientUtilsV1_1().post(path, params, null, new RestRequest.Listener() {
            @Override public void onResponse(JSONObject response) {
                boolean deleted = response.optBoolean(DELETED_KEY, false);
                mDelegate.onMenuDeleted(HttpURLConnection.HTTP_OK, menu, deleted);
            }
        }, new RestRequest.ErrorListener() {
            @Override public void onErrorResponse(VolleyError error) {
                mDelegate.onMenuDeleted(statusCodeFromVolleyError(error), menu, false);
            }
        });
        return true;
    }
}
