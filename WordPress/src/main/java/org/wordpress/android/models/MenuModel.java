package org.wordpress.android.models;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a Menu and provides convenience methods for local database (de)serialization.
 *
 * Menus contain a list of {@link MenuItemModel} ID's and a list of {@link MenuLocationModel} ID's,
 * the former being ordered.
 */

public class MenuModel implements NameInterface {
    public long siteId;
    public long menuId;
    public String name;
    public String details;
    public List<MenuLocationModel> locations;
    public List<MenuItemModel> menuItems;

    public static final long ADD_MENU_ID = -1;
    public static final long DEFAULT_MENU_ID = -2;
    public static final long NO_MENU_ID = -3;

    public MenuModel () {
    }

    public MenuModel (MenuModel orig) {
        siteId = orig.siteId;
        menuId = orig.menuId;
        name = orig.name;
        details = orig.details;
        if (orig.locations != null) {
            locations = new ArrayList<>();
            locations.addAll(orig.locations);
        }
        if (orig.menuItems != null) {
            menuItems = new ArrayList<>();
            menuItems.addAll(orig.menuItems);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuModel)) return false;

        MenuModel otherModel = (MenuModel) other;
        return menuId == otherModel.menuId &&
                StringUtils.equals(name, otherModel.name) &&
                StringUtils.equals(details, otherModel.details) &&
                CollectionUtils.areListsEqual(locations, otherModel.locations) &&
                CollectionUtils.areListsEqual(menuItems, otherModel.menuItems);
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isDefaultMenu() {
        if (menuId == DEFAULT_MENU_ID) {
            return true;
        }
        return false;
    }

    public boolean isNoMenu() {
        if (menuId == NO_MENU_ID) {
            return true;
        }
        return false;
    }

    public boolean isSpecialMenu() {
        return isNoMenu() || isDefaultMenu();
    }

    public void stripLocationFromMenu(MenuLocationModel location){
        if (locations != null && location != null && location.name != null) {
            for (MenuLocationModel loc : locations) {
                if (loc.name != null && loc.name.equals(location.name)) {
                    locations.remove(loc);
                    break;
                }
            }
        }
    }

}
