package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class CategoryItemEditor extends BaseMenuItemEditor {
    private final List<CategoryModel> mCategories;
    private final List<String> mCategoryNames;

    private RadioButtonListView mCategoryListView;

    public CategoryItemEditor(Context context) {
        this(context, null);
    }

    public CategoryItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mCategories = new ArrayList<>();
        mCategoryNames = new ArrayList<>();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
//        EventBus.getDefault().register(this);
        loadCategories();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        mCategoryListView = (RadioButtonListView) child.findViewById(R.id.category_list);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_category_empty_list));
        mCategoryListView.setEmptyView(emptyTextView);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.category_menu_item_edit_view;
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = super.getMenuItem();
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

    public String getSelectedCategoryName() {
        return "";
    }

    private void loadCategories() {
        Map<Integer, CategoryModel> categories = SiteSettingsTable.getAllCategories();
        mCategories.clear();
        if (categories != null) {
            mCategories.addAll(categories.values());
        }
        refreshAdapter();
    }

    private void fetchCategories() {
        // TODO
    }

    private void refreshAdapter() {
        if (mCategoryListView != null) {
            mCategoryListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(getContext(), mCategoryNames));
        }
    }
}
