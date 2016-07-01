package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 * Provides appropriate {@link BaseMenuItemEditor} subclasses.
 */
public class MenuItemEditorFactory {
    public enum ITEM_TYPE {
        NULL,
        POST,
        PAGE,
        CATEGORY,
        TAG,
        LINK,
        CUSTOM;

        public static ITEM_TYPE typeForString(String typeName) {
            if (TextUtils.isEmpty(typeName)) return NULL;

            if (typeName.equalsIgnoreCase(POST.name())) return POST;
            else if (typeName.equalsIgnoreCase(PAGE.name())) return PAGE;
            else if (typeName.equalsIgnoreCase(CATEGORY.name())) return CATEGORY;
            else if (typeName.equalsIgnoreCase(TAG.name())) return TAG;
            else if (typeName.equalsIgnoreCase(LINK.name())) return LINK;
            else if (typeName.equalsIgnoreCase(CUSTOM.name())) return CUSTOM;

            //special case for tag
            // This is a weird behavior of the API and is not documented.
            if (typeName.compareToIgnoreCase(MenuItemModel.TAG_TYPE_NAME) == 0) {
                return TAG;
            }
            else if (typeName.compareToIgnoreCase("post_type") == 0) {
                return POST;
            }

            else return NULL;
        }

        public static ITEM_TYPE typeForIndex(int index){
            if (NULL.ordinal() == index) return NULL;
            if (POST.ordinal() == index) return POST;
            if (PAGE.ordinal() == index) return PAGE;
            if (CATEGORY.ordinal() == index) return CATEGORY;
            if (TAG.ordinal() == index) return TAG;
            if (LINK.ordinal() == index) return LINK;
            if (CUSTOM.ordinal() == index) return CUSTOM;

            return NULL;
        }
    }

    public static int getIconDrawableRes(ITEM_TYPE type) {
        switch (type) {
            case POST:
                return R.drawable.my_site_icon_posts;
            case PAGE:
                return R.drawable.my_site_icon_pages;
            case CATEGORY:
                return R.drawable.stats_icon_categories;
            case TAG:
                return R.drawable.gridicon_tag;
            case LINK:
                return R.drawable.gridicon_link;
            default:
                return -1;
        }
    }

    public static BaseMenuItemEditor getEditor(Context context, ITEM_TYPE type) {
        switch (type) {
            case PAGE:
                return new PageItemEditor(context);
            case POST:
                return new PostItemEditor(context);
            case CATEGORY:
                return new CategoryItemEditor(context);
            case TAG:
                return new TagItemEditor(context);
            case LINK:
                return new LinkItemEditor(context);
            default:
                return null;
        }
    }
}
