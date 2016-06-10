package org.wordpress.android.models;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.List;

/**
 * Menu Items define content that a Menu displays. They can be of type Page, Link, Post, Category,
 * or Tag and can have any number of children (also Menu Items).
 */

public class MenuItemModel {
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
    public List<MenuItemModel> children;
    public int flattenedLevel; //might be 0 for root, 1 for first level, 2 for second, etc.

    public MenuItemModel() {
    }

    public MenuItemModel(MenuItemModel item) {
        this.itemId = item.itemId;
        this.menuId = item.menuId;
        this.parentId = item.parentId;
        this.contentId = item.contentId;
        this.url = item.url;
        this.name = item.name;
        this.details = item.details;
        this.linkTarget = item.linkTarget;
        this.linkTitle = item.linkTitle;
        this.type = item.type;
        this.typeFamily = item.typeFamily;
        this.typeLabel = item.typeLabel;
        this.children = item.children;
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
}
