package org.wordpress.android.ui.menus.views;

import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

/**
 */
public class MenuItemFactory {
    public enum ITEM_TYPE {
        NULL,
        POST,
        PAGE,
        CATEGORY,
        TAG,
        LINK,
        TESTIMONIAL,
        MISC
    }

    public static int getMenuItemIcon(ITEM_TYPE type) {
        switch (type) {
            case NULL:
                return 0;
            case PAGE:
                return R.drawable.gridicon_pages;
            default:
                return 0;
        }
    }

    public static View getEditView(ITEM_TYPE type, ViewGroup root) {
        if (root == null) return root;

        switch (type) {
            case PAGE:
                return View.inflate(root.getContext(), R.layout.page_menu_item_edit_view, root);
        }

        return null;
    }

    public static BaseMenuItemView getMenuItemView(ITEM_TYPE type) {
        switch (type) {
            case POST:
                return null;
            case PAGE:
                return null;
            case CATEGORY:
                return null;
            case TAG:
                return null;
            case LINK:
                return null;
            case TESTIMONIAL:
                return null;
            case MISC:
                return null;
            case NULL:
            default:
                return null;
        }
    }
}
