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
        JETPACK_TESTIMONIAL,
        JETPACK_PORTFOLIO,
        JETPACK_COMIC,
        CUSTOM;

        public static ITEM_TYPE typeForString(String typeName) {
            if (TextUtils.isEmpty(typeName)) return NULL;

            if (typeName.equalsIgnoreCase(POST.name())) return POST;
            else if (typeName.equalsIgnoreCase(PAGE.name())) return PAGE;
            else if (typeName.equalsIgnoreCase(CATEGORY.name())) return CATEGORY;
            else if (typeName.equalsIgnoreCase(TAG.name())) return TAG;
            else if (typeName.equalsIgnoreCase(LINK.name())) return LINK;
            else if (typeName.equalsIgnoreCase(CUSTOM.name())) return CUSTOM;
            else if (typeName.equalsIgnoreCase(JETPACK_TESTIMONIAL.name())) return JETPACK_TESTIMONIAL;
            else if (typeName.equalsIgnoreCase(JETPACK_PORTFOLIO.name())) return JETPACK_PORTFOLIO;
            else if (typeName.equalsIgnoreCase(JETPACK_COMIC.name())) return JETPACK_COMIC;

            //special case for tag
            // This is a weird behavior of the API and is not documented.
            if (typeName.compareToIgnoreCase(MenuItemModel.TAG_TYPE_NAME) == 0) {
                return TAG;
            } else if (typeName.compareToIgnoreCase(MenuItemModel.POST_TYPE_NAME) == 0) {
                return POST;
            } else if (typeName.compareToIgnoreCase(MenuItemModel.TESTIMONIAL_TYPE_NAME) == 0) {
                return JETPACK_TESTIMONIAL;
            } else if (typeName.compareToIgnoreCase(MenuItemModel.PORTFOLIO_TYPE_NAME) == 0) {
                return JETPACK_PORTFOLIO;
            } else if (typeName.compareToIgnoreCase(MenuItemModel.COMIC_TYPE_NAME) == 0) {
                return JETPACK_COMIC;
            }

            return NULL;
        }

        public static String nameForItemType(Context context, ITEM_TYPE itemType) {
            String name = "";

            switch (itemType) {
                case POST:
                    name = context.getString(R.string.menu_item_type_post);
                    break;
                case PAGE:
                    name = context.getString(R.string.menu_item_type_page);
                    break;
                case CATEGORY:
                    name = context.getString(R.string.menu_item_type_category);
                    break;
                case TAG:
                    name = context.getString(R.string.menu_item_type_tag);
                    break;
                case LINK:
                    name = context.getString(R.string.menu_item_type_link);
                    break;
                case JETPACK_TESTIMONIAL:
                    name = context.getString(R.string.menu_item_type_testimonial);
                    break;
                case JETPACK_PORTFOLIO:
                    name = context.getString(R.string.menu_item_type_portfolio);
                    break;
                case JETPACK_COMIC:
                    name = context.getString(R.string.menu_item_type_comic);
                    break;
                default:
                    break;
            }

            return name;
        }
    }

    public static int getIconDrawableRes(ITEM_TYPE type) {
        switch (type) {
            case PAGE:
                return R.drawable.my_site_icon_pages;
            case CATEGORY:
                return R.drawable.stats_icon_categories;
            case TAG:
                return R.drawable.gridicon_tag;
            case LINK:
                return R.drawable.gridicon_link;
            case POST:
            case JETPACK_TESTIMONIAL:
            case JETPACK_PORTFOLIO:
            case JETPACK_COMIC:
                return R.drawable.my_site_icon_posts;
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
            case JETPACK_TESTIMONIAL:
            case JETPACK_PORTFOLIO:
            case JETPACK_COMIC:
                JetpackCustomItemEditor editor = new JetpackCustomItemEditor(context);
                editor.setJetpackCustomType(type);
                return editor;
            default:
                return null;
        }
    }
}
