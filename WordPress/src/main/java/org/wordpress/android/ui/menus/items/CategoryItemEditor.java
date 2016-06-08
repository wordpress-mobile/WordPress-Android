package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

import java.util.List;
import java.util.Map;

/**
 */
public class CategoryItemEditor extends BaseMenuItemEditor {
    public static final String CATEGORY_KEY = "selected-category";

    // TODO: is RadioGroup what we want to use here?
    private RadioGroup mCategoriesGroup;
    private Map<Integer, String> mCategories;

    @Override
    public int getIconDrawable() {
        return R.drawable.stats_icon_categories;
    }

    @Override
    public View getEditView(Context context, ViewGroup root) {
        View editView = View.inflate(context, R.layout.category_menu_item_edit_view, root);

        mCategoriesGroup = (RadioGroup) editView.findViewById(R.id.category_editor_options);

        return editView;
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = new MenuItemModel();
        fillData(menuItem);
        return menuItem;
    }

    @Override
    public void onSave() {
        // TODO: save to DB
    }

    @Override
    public void onDelete() {
        // TODO: remove from DB
    }

    public void setCategories(List<String> categories) {
        if (categories == null || categories.size() <= 0) {
            mCategories.clear();
            return;
        }
    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        if (mCategoriesGroup != null) {
            menuItem.addData(CATEGORY_KEY, String.valueOf(mCategoriesGroup.getCheckedRadioButtonId()));
        }
    }
}
