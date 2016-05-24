package org.wordpress.android.models;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.List;

/**
 * Describes a Menu and provides convenience methods for local database (de)serialization.
 *
 * Menus contain a list of {@link MenuItemModel} ID's and a list of {@link MenuLocationModel} ID's,
 * the former being ordered.
 */

public class MenuModel {
    public long menuId;
    public String name;
    public String details;
    public List<MenuLocationModel> locations;
    public List<MenuItemModel> menuItems;

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
}
