package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.text.TextUtils;

import org.wordpress.android.R;

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
        TESTIMONIAL,
        CUSTOM;

        public static ITEM_TYPE typeForString(String typeName) {
            if (TextUtils.isEmpty(typeName)) return NULL;

            if (typeName.equalsIgnoreCase(POST.name())) return POST;
            else if (typeName.equalsIgnoreCase(PAGE.name())) return PAGE;
            else if (typeName.equalsIgnoreCase(CATEGORY.name())) return CATEGORY;
            else if (typeName.equalsIgnoreCase(TAG.name())) return TAG;
            else if (typeName.equalsIgnoreCase(LINK.name())) return LINK;
            else if (typeName.equalsIgnoreCase(TESTIMONIAL.name())) return TESTIMONIAL;
            else if (typeName.equalsIgnoreCase(CUSTOM.name())) return CUSTOM;
            else return NULL;
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
            case TESTIMONIAL:
                return R.drawable.media_image_placeholder;
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
            case TESTIMONIAL:
                return new TestimonialItemEditor(context);
            default:
                return null;
        }
    }
}
