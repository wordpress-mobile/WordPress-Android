package org.wordpress.android.networking.menus;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.helpshift.support.util.ListUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts Menus related objects between JSON and local database models.
 */
public class MenusDataModeler {
    //
    // Menu JSON keys
    //
    public static final String MENU_ID_KEY = "id";
    public static final String MENU_NAME_KEY = "name";
    public static final String MENU_DESCRIPTION_KEY = "description";
    public static final String MENU_ITEMS_KEY = "items";
    public static final String MENU_LOCATIONS_KEY = "locations";

    //
    // Menu Location JSON keys
    //
    public static final String LOCATION_NAME_KEY = "name";
    public static final String LOCATION_DESCRIPTION_KEY = "description";
    public static final String LOCATION_DEFAULT_STATE_KEY = "defaultState";

    //
    // Menu Item JSON keys
    //
    public static final String ITEM_ID_KEY = "id";
    public static final String ITEM_CONTENT_ID_KEY = "content_id";
    public static final String ITEM_TYPE_KEY = "type";
    public static final String ITEM_TYPE_FAMILY_KEY = "type_family";
    public static final String ITEM_TYPE_LABEL_KEY = "type_label";
    public static final String ITEM_URL_KEY = "url";
    public static final String ITEM_NAME_KEY = "name";
    public static final String ITEM_LINK_TARGET_KEY = "link_target";
    public static final String ITEM_LINK_TITLE_KEY = "link_title";
    public static final String ITEM_DESCRIPTION_KEY = "description";
    public static final String ITEM_CLASSES_KEY = "classes";// TODO: needed?
    public static final String ITEM_XFN_KEY = "xfn";// TODO: needed?
    public static final String ITEM_CHILDREN_KEY = "items";

    /**
     * To be considered a valid Menu a {@link JSONObject} must:
     * <ul>
     *     <li>not be null</li>
     *     <li>contain an ID via {@link MenusDataModeler#MENU_ID_KEY}</li>
     *     <li>contain a locations array via {@link MenusDataModeler#MENU_LOCATIONS_KEY}</li>
     * </ul>
     *
     * An ID is required to uniquely identify the Menu (essentially the primary key). A locations
     * array is required because it is unique to Menu responses.
     */
    public static boolean isValidMenuJson(JSONObject json) {
        return json != null && json.has(MENU_ID_KEY) && json.has(MENU_LOCATIONS_KEY);
    }

    public static boolean isValidMenu(MenuModel menu) {
        return menu != null;
    }

    public static MenuModel menuFromJson(JSONObject json, List<MenuLocationModel> locations, long siteId) {
        if (!isValidMenuJson(json)) return null;

        MenuModel menu = new MenuModel();
        menu.siteId = siteId;
        menu.menuId = json.optLong(MENU_ID_KEY);
        menu.name = json.optString(MENU_NAME_KEY);
        menu.details = json.optString(MENU_DESCRIPTION_KEY);
        menu.menuItems = menuItemsFromJson(json.optJSONArray(MENU_ITEMS_KEY), menu.menuId, 0);
        List<String> locationNames = new ArrayList<>();

        try {
            JSONArray locationStrings = json.optJSONArray(MENU_LOCATIONS_KEY);
            if (locationStrings != null) {
                for (int i=0; i < locationStrings.length(); i++){
                    locationNames.add(locationStrings.getString(i));
                }
            }
        } catch (JSONException exception) {
            AppLog.w(AppLog.T.API, "Error parsing All Menus REST response");
        }

        if (locations != null) {
            //now that we have the possible available Menu Location names, we populate the MenuModel.locations
            //attribute with each full blown MenuLocationModel instance for ease of use later on.
            List<MenuLocationModel> locationModels = new ArrayList<>();
            for (MenuLocationModel locationModel : locations){
                for (String locationName : locationNames){
                    if (locationModel.name != null) {
                        if (locationModel.name.equals(locationName)) {
                            locationModels.add(locationModel);
                            break;
                        }
                    }
                }
            }
            menu.locations = locationModels;
        }

        return menu;
    }

    @NonNull
    private static JSONArray serializeLocations(List<MenuLocationModel> locations) {
        JSONArray locationsArray = new JSONArray();
        if (locations != null && locations.size() > 0) {
            for (MenuLocationModel location : locations) {
                if (location != null) locationsArray.put(location.name);
            }
        }
        return locationsArray;
    }

    public static JSONObject menuToJson(MenuModel menu) {
        if (!isValidMenu(menu)) return null;

        try {
            JSONObject json = new JSONObject();
            json.put(MENU_ID_KEY, menu.menuId);
            json.put(MENU_NAME_KEY, menu.name);
            json.put(MENU_DESCRIPTION_KEY, menu.details);
            json.put(MENU_LOCATIONS_KEY, serializeLocations(menu.locations));
            if (!ListUtils.isEmpty(menu.menuItems)) {
                JSONArray itemArray = new JSONArray();
                for (MenuItemModel item : menu.menuItems) {
                    itemArray.put(menuItemToJson(item));
                }
                json.put(MENU_ITEMS_KEY, itemArray);
            }
            return json;
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error serializing MenuModel to JSON: " + exception);
        }

        return null;
    }

    /**
     * To be considered a valid Menu Location a {@link JSONObject} must:
     * <ul>
     *     <li>not be null</li>
     *     <li>contain a name via {@link MenusDataModeler#LOCATION_NAME_KEY}</li>
     *     <li>contain a default state via {@link MenusDataModeler#LOCATION_DEFAULT_STATE_KEY}</li>
     * </ul>
     *
     * A name is required to uniquely identify the Menu Location (essentially the primary key). A
     * default state is required because the default state key is unique to Menu Location responses.
     */
    public static boolean isValidMenuLocationJson(JSONObject json) {
        return json != null && json.has(LOCATION_NAME_KEY) && json.has(LOCATION_DEFAULT_STATE_KEY);
    }

    public static boolean isValidMenuLocation(MenuLocationModel location) {
        return location != null && !TextUtils.isEmpty(location.name);
    }

    public static MenuLocationModel menuLocationFromJson(JSONObject json, long siteId) {
        if (!isValidMenuLocationJson(json)) return null;

        try {
            MenuLocationModel location = new MenuLocationModel();
            location.name = json.getString(LOCATION_NAME_KEY);
            location.details = json.getString(LOCATION_DESCRIPTION_KEY);
            location.defaultState = json.getString(LOCATION_DEFAULT_STATE_KEY);
            location.siteId = siteId;
            return location;
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error parsing Menu Location JSON: " + exception);
        }

        return null;
    }

    public static List<MenuLocationModel> menuLocationsFromJson(JSONArray json, long siteId) {
        if (json == null || json.length() <= 0) return null;

        try {
            List<MenuLocationModel> locations = new ArrayList<>();

            for (int i = 0; i < json.length(); ++i) {
                JSONObject locationObj = json.getJSONObject(i);
                MenuLocationModel location = menuLocationFromJson(locationObj, siteId);
                if (location != null) locations.add(location);
            }

            return locations;
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error parsing Menu Locations JSON: " + exception);
        }

        return null;
    }

    public static JSONObject menuLocationToJson(MenuLocationModel location) {
        if (!isValidMenuLocation(location)) return null;

        try {
            JSONObject json = new JSONObject();
            json.put(LOCATION_NAME_KEY, location.name);
            json.put(LOCATION_DESCRIPTION_KEY, location.details);
            json.put(LOCATION_DEFAULT_STATE_KEY, location.defaultState);
            return json;
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error serializing MenuLocationModel to JSON: " + exception);
        }

        return null;
    }

    /**
     * To be considered a valid Menu Item a {@link JSONObject} must:
     * <ul>
     *     <li>not be null</li>
     *     <li>contain an ID via {@link MenusDataModeler#ITEM_ID_KEY}</li>
     *     <li>contain a content ID via {@link MenusDataModeler#ITEM_CONTENT_ID_KEY}</li>
     * </ul>
     *
     * An ID is required to uniquely identify the Menu Item (essentially the primary key). The
     * content ID is required because it is unique to Menu Item responses.
     */
    public static boolean isValidMenuItemJson(JSONObject json) {
        return json != null && json.has(ITEM_ID_KEY) && json.has(ITEM_CONTENT_ID_KEY);
    }

    public static boolean isValidMenuItem(MenuItemModel item) {
        return item != null;
    }

    public static MenuItemModel menuItemFromJson(JSONObject json, long menuId, long parentId) {
        if (!isValidMenuItemJson(json)) return null;

        MenuItemModel item = new MenuItemModel();
        item.parentId = parentId;
        item.menuId = menuId;
        item.itemId = json.optLong(ITEM_ID_KEY);
        item.contentId = json.optLong(ITEM_CONTENT_ID_KEY);
        item.type = json.optString(ITEM_TYPE_KEY);
        item.typeFamily = json.optString(ITEM_TYPE_FAMILY_KEY);
        item.typeLabel = json.optString(ITEM_TYPE_LABEL_KEY);
        item.url = json.optString(ITEM_URL_KEY);
        item.name = json.optString(ITEM_NAME_KEY);
        item.linkTarget = json.optString(ITEM_LINK_TARGET_KEY);
        item.linkTitle = json.optString(ITEM_LINK_TITLE_KEY);
        item.details = json.optString(ITEM_DESCRIPTION_KEY);

        if (json.has(ITEM_CHILDREN_KEY)) {
            item.children = new ArrayList<>();
            JSONArray children = json.optJSONArray(ITEM_CHILDREN_KEY);
            for (int i = 0; i < children.length(); ++i) {
                MenuItemModel child = menuItemFromJson(children.optJSONObject(i), menuId, item.itemId);
                if (child == null) continue;
                item.children.add(child);
            }
        }

        return item;
    }

    public static List<MenuItemModel> menuItemsFromJson(JSONArray json, long menuId, long parentId) {
        if (json == null) return null;

        List<MenuItemModel> items = new ArrayList<>();
        for (int i = 0; i < json.length(); ++i) {
            items.add(menuItemFromJson(json.optJSONObject(i), menuId, parentId));
        }
        return items;
    }

    public static JSONObject menuItemToJson(MenuItemModel item) {
        if (!isValidMenuItem(item)) return null;

        try {
            JSONObject json = new JSONObject();
            if (item.itemId > 0) {
                json.put(ITEM_ID_KEY, item.itemId);
            }
            json.put(ITEM_CONTENT_ID_KEY, item.contentId);
            json.put(ITEM_TYPE_KEY, item.type);
            json.put(ITEM_TYPE_FAMILY_KEY, item.typeFamily);
            json.put(ITEM_TYPE_LABEL_KEY, item.typeLabel);
            json.put(ITEM_URL_KEY, item.url);
            json.put(ITEM_NAME_KEY, item.name);
            json.put(ITEM_LINK_TARGET_KEY, item.linkTarget);
            json.put(ITEM_LINK_TITLE_KEY, item.linkTitle);
            json.put(ITEM_DESCRIPTION_KEY, item.details);

            // Add child Menu Items
            if (item.hasChildren()) {
                JSONArray childArray = new JSONArray();
                for (int i = 0; i < item.children.size(); ++i) {
                    MenuItemModel childItem = item.children.get(i);
                    if (isValidMenuItem(childItem)) {
                        childArray.put(i, menuItemToJson(childItem));
                    }
                }
                json.put(ITEM_CHILDREN_KEY, childArray);
            }

            return json;
        } catch (JSONException exception) {
            AppLog.e(AppLog.T.API, "Error serializing MenuItemModel to JSON: " + exception);
        }

        return null;
    }

    public static List<MenuItemModel> inflateMenuItemModelList(List<MenuItemModel> flattenedList) {
        List<MenuItemModel> inflatedList = new ArrayList<>();

        if (flattenedList != null && flattenedList.size() > 0) {
            MenuItemModel parent = flattenedList.get(0);
            int lastLevel = parent.flattenedLevel;
            int itemsProcessedInThisBranch = 0;
            for (int i=0; i < flattenedList.size(); i += itemsProcessedInThisBranch){
                MenuItemModel item = flattenedList.get(i);
                if (item != null) {
                    if (item.flattenedLevel == lastLevel) {
                        //same hierarchy
                        parent = item;
                        parent.children = null;
                        inflatedList.add(item);
                        itemsProcessedInThisBranch = 1;
                    }
                    else if (item.flattenedLevel > lastLevel) {
                        //further down the hierarchy
                        parent.children = inflateMenuItemModelList(flattenedList.subList(flattenedList.indexOf(item), flattenedList.size()));
                        itemsProcessedInThisBranch = getElementCountInHierarchy(parent.children);
                    } else {
                        //one or more levels up the hierarchy
                        return inflatedList;
                    }
                }
            }
        }

        return inflatedList;
    }

    /**
     * Receives a hierarchized MenuItemModel list and counts all elements in this hierarchy.
     * Please note it *MUST NOT* be a flattened list.
     * @param itemList
     * @return element count in hierarchy
     */
    public static int getElementCountInHierarchy(List<MenuItemModel> itemList) {
        int total = 0;
        if (itemList != null) {
            for (MenuItemModel item : itemList) {
                total++;
                if (item.hasChildren()) {
                    total += getElementCountInHierarchy(item.children);
                }
            }
        }
        return total;
    }

    public static List<MenuItemModel> flattenMenuItemModelList(List<MenuItemModel> hierarchyList, int currentLevel) {
        ArrayList<MenuItemModel> flattenedList = new ArrayList<>();
        if (hierarchyList != null) {
            for (MenuItemModel item : hierarchyList) {
                item.flattenedLevel = currentLevel;
                item.calculateCustomType();
                flattenedList.add(item);
                if (item.hasChildren()) {
                    List<MenuItemModel> tmpList = flattenMenuItemModelList(item.children, currentLevel+1);
                    flattenedList.addAll(tmpList);
                }
            }
        }
        return flattenedList;
    }
}
