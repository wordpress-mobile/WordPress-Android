package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory;
import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Menu Items define content that a Menu displays. They can be of type Page, Link, Post, Category,
 * or Tag and can have any number of children (also Menu Items).
 */

public class MenuItemModel implements Serializable {
    public static final String POST_TYPE_NAME = "post_type";
    public static final String TAG_TYPE_NAME = "post_tag";
    public static final String TESTIMONIAL_TYPE_NAME = "jetpack-testimonial";
    public static final String PORTFOLIO_TYPE_NAME = "jetpack-portfolio";
    public static final String COMIC_TYPE_NAME = "jetpack-comic";

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
    public boolean editingMode;// used for visual representation
    public String calculatedType; //used for visual representation

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
        if (other.children != null) {
            children = new ArrayList<>(other.children);
        }
        flattenedLevel = other.flattenedLevel;
        editingMode = other.editingMode;
        calculatedType = other.calculatedType;
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

    public MenuItemEditorFactory.ITEM_TYPE calculateCustomType(){

        if (type != null ) {
            if (type.compareToIgnoreCase(TAG_TYPE_NAME) == 0) {
                calculatedType = MenuItemEditorFactory.ITEM_TYPE.TAG.name();
                return MenuItemEditorFactory.ITEM_TYPE.TAG;
            } else if (type.compareToIgnoreCase(POST_TYPE_NAME) == 0) {
                calculatedType = MenuItemEditorFactory.ITEM_TYPE.POST.name();
                return MenuItemEditorFactory.ITEM_TYPE.POST;
            } else if (type.compareToIgnoreCase(TESTIMONIAL_TYPE_NAME) == 0) {
                calculatedType = MenuItemEditorFactory.ITEM_TYPE.JETPACK_TESTIMONIAL.name();
                return MenuItemEditorFactory.ITEM_TYPE.JETPACK_TESTIMONIAL;
            } else if (type.compareToIgnoreCase(PORTFOLIO_TYPE_NAME) == 0) {
                calculatedType = MenuItemEditorFactory.ITEM_TYPE.JETPACK_PORTFOLIO.name();
                return MenuItemEditorFactory.ITEM_TYPE.JETPACK_PORTFOLIO;
            } else if (type.compareToIgnoreCase(COMIC_TYPE_NAME) == 0) {
                calculatedType = MenuItemEditorFactory.ITEM_TYPE.JETPACK_COMIC.name();
                return MenuItemEditorFactory.ITEM_TYPE.JETPACK_COMIC;
            }

            if (MenuItemEditorFactory.ITEM_TYPE.typeForString(type).equals(MenuItemEditorFactory.ITEM_TYPE.CUSTOM)) {

                String homeUrl = WordPress.getCurrentBlog().getHomeURL() + "/";
                //check special cases:
                //- custom and the url is EQUAL to the blog's home address: show the HOME PAGE icon
                if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(homeUrl) && url.equalsIgnoreCase(homeUrl)) {
                    calculatedType = MenuItemEditorFactory.ITEM_TYPE.PAGE.name();
                    return MenuItemEditorFactory.ITEM_TYPE.PAGE;
                }
                else
                    //check special cases:
                    //- custom and url different from home: show LINK ICON
                    if (!TextUtils.isEmpty(url) && !TextUtils.isEmpty(homeUrl) && !url.equalsIgnoreCase(homeUrl)) {
                        calculatedType = MenuItemEditorFactory.ITEM_TYPE.LINK.name();
                        return MenuItemEditorFactory.ITEM_TYPE.LINK;
                    }

                calculatedType = MenuItemEditorFactory.ITEM_TYPE.CUSTOM.name();
                return MenuItemEditorFactory.ITEM_TYPE.CUSTOM;
            }

            //default: calculatedtype is same as type
            calculatedType = type;
            return MenuItemEditorFactory.ITEM_TYPE.typeForString(type);
        }

        calculatedType = MenuItemEditorFactory.ITEM_TYPE.NULL.name();
        return MenuItemEditorFactory.ITEM_TYPE.NULL;
    }
}
