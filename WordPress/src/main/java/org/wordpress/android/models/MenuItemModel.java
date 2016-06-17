package org.wordpress.android.models;

import android.support.annotation.NonNull;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu Items define content that a Menu displays. They can be of type Page, Link, Post, Category,
 * or Tag and can have any number of children (also Menu Items).
 */

public class MenuItemModel implements Serializable {
    //
    // Primary key attributes (cannot be null)
    //
    public long itemId;

    //
    // Optional attributes (may be null)
    //
    public long menuId;
    public long parentId;
    public long contentId;
    public String url;
    public String name;
    public String details;
    public String linkTarget;
    public String linkTitle;
    public String type;
    public String typeFamily;
    public String typeLabel;
    public Map<String, String> data;
    public List<MenuItemModel> children;
    public int flattenedLevel; //might be 0 for root, 1 for first level, 2 for second, etc.
    public boolean editingMode;// used for visual representation

    public MenuItemModel() {
    }

    public MenuItemModel(MenuItemModel other) {
        if (other == null) return;
        menuId = other.menuId;
        parentId = other.parentId;
        contentId = other.contentId;
        url = other.url;
        name = other.name;
        details = other.details;
        linkTarget = other.linkTarget;
        type = other.type;
        typeFamily = other.typeFamily;
        typeLabel = other.typeLabel;
        if (other.data != null) {
            data = new HashMap<>(other.data);
        }
        if (other.children != null) {
            children = new ArrayList<>(other.children);
        }
        flattenedLevel = other.flattenedLevel;
        editingMode = other.editingMode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuItemModel)) return false;

        MenuItemModel otherItem = (MenuItemModel) other;
        return itemId == otherItem.itemId &&
                menuId == otherItem.menuId &&
                parentId == otherItem.parentId &&
                contentId == otherItem.contentId &&
                StringUtils.equals(url, otherItem.url) &&
                StringUtils.equals(name, otherItem.name) &&
                StringUtils.equals(details, otherItem.details) &&
                StringUtils.equals(linkTarget, otherItem.linkTarget) &&
                StringUtils.equals(linkTitle, otherItem.linkTitle) &&
                StringUtils.equals(type, otherItem.type) &&
                StringUtils.equals(typeFamily, otherItem.typeFamily) &&
                StringUtils.equals(typeLabel, otherItem.typeLabel) &&
                CollectionUtils.areListsEqual(children, otherItem.children);
    }

    public boolean hasChildren() {
        return children != null && children.size() > 0;
    }

    public void addData(@NonNull String key, String value) {
        if (data == null) data = new HashMap<>();
        data.put(key, value);
    }
}
