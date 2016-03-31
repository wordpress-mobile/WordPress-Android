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

public class MenuModel {
    public long menuId;
    public String name;
    public String details;
    public List<Long> locations;
    public List<Long> menuItems;

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

    /**
     * Removes any existing children before adding children from given string list.
     *
     * @param locationList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void setLocationsFromStringList(String locationList) {
        if (locations != null) locations.clear();
        addLocationFromStringList(locationList);
    }

    /**
     * Adds children from given string list, maintaining existing children.
     *
     * @param locationList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void addLocationFromStringList(String locationList) {
        if (locations == null) locations = new ArrayList<>();
        CollectionUtils.addLongsFromStringListToArrayList(locations, locationList);
    }

    /**
     * Removes any existing children before adding children from given string list.
     *
     * @param itemList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void setItemsFromStringList(String itemList) {
        if (menuItems != null) menuItems.clear();
        addItemsFromStringList(itemList);
    }

    /**
     * Adds children from given string list, maintaining existing children.
     *
     * @param itemList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void addItemsFromStringList(String itemList) {
        if (menuItems == null) menuItems = new ArrayList<>();
        CollectionUtils.addLongsFromStringListToArrayList(menuItems, itemList);
    }
}
