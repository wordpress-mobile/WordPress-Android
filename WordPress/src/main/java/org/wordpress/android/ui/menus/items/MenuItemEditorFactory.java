package org.wordpress.android.ui.menus.items;

import android.text.TextUtils;

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

    public static BaseMenuItemEditor getEditor(ITEM_TYPE type) {
        switch (type) {
            case PAGE:
                return new PageItemEditor();
            case POST:
                return new PostItemEditor();
            case CATEGORY:
                return new CategoryItemEditor();
            case TAG:
                return new TagItemEditor();
            case LINK:
                return new LinkItemEditor();
            case TESTIMONIAL:
                return new TestimonialItemEditor();
            case NULL:
            default:
                return null;
        }
    }
}
