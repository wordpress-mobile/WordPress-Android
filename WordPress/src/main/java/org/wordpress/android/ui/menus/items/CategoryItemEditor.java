package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SearchView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.util.AppLog;
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
        loadCategories();
        fetchCategories();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        mCategoryListView = (RadioButtonListView) child.findViewById(R.id.category_list);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_category_empty_list));
        mCategoryListView.setEmptyView(emptyTextView);

        mCategoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mOtherDataDirty = true;
                if (!mItemNameDirty) {
                    if (mItemNameEditText != null) {
                        mItemNameEditText.setText(mCategories.get(i).name);
                    }
                }
            }
        });

    }

    @Override
    public int getLayoutRes() {
        return R.layout.category_menu_item_edit_view;
    }

    @Override
    public int getNameEditTextRes() {
        return R.id.menu_item_title_edit;
    }

    @Override
    public void setMenuItem(MenuItemModel menuItem) {
        super.setMenuItem(menuItem);
        if (!TextUtils.isEmpty(menuItem.name)) {
            setSelection(menuItem.contentId);
        }
    }

    private void setSelection(long contentId) {
        mOtherDataDirty = false;
        for (int i=0; i < mCategories.size(); i++) {
            CategoryModel categ = mCategories.get(i);
            int id = categ.id;
            if (id == contentId){
                mCategoryListView.setSelection(i);
                break;
            }
        }
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = super.getMenuItem();
        fillData(menuItem);
        return menuItem;
    }

    private void loadCategories() {
        Map<Integer, CategoryModel> categories = SiteSettingsTable.getAllCategories();
        mCategories.clear();
        mCategoryNames.clear();
        if (categories != null) {
            mCategories.addAll(categories.values());
            for (CategoryModel categ : categories.values()) {
                mCategoryNames.add(categ.name);
            }
        }
        refreshAdapter();
    }

    /**
     * Request a list of post categories for a site via the WordPress REST API.
     */
    private void fetchCategories() {
        WordPress.getRestClientUtilsV1_1().getCategories(String.valueOf(WordPress.getCurrentRemoteBlogId()),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        AppLog.d(AppLog.T.API, "Received response to Categories REST request.");

                        CategoryModel[] models = CategoryModel.deserializeJsonRestResponse(response);
                        if (models == null) return;

                        SiteSettingsTable.saveCategories(models);
                        loadCategories();

                        MenuItemModel item = CategoryItemEditor.super.getMenuItem();
                        if (item != null) {
                            //super.getMenuItem() called on purpose to avoid any processing, we just want the working item
                            setSelection(item.contentId);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(AppLog.T.API, "Error fetching WP.com categories:" + error);
                    }
                });
    }

    private void refreshAdapter() {
        if (mCategoryListView != null) {
            mCategoryListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(getContext(), mCategoryNames));
        }
    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        //check selected item in array and set selected
        menuItem.type = MenuItemEditorFactory.ITEM_TYPE.CATEGORY.name().toLowerCase(); //default type: CATEGORY
        menuItem.typeFamily = "taxonomy";
        menuItem.typeLabel = MenuItemEditorFactory.ITEM_TYPE.CATEGORY.name();

        CategoryModel categ = mCategories.get(mCategoryListView.getCheckedItemPosition());
        if (categ != null) {
            menuItem.contentId = categ.id;
        }
    }


}
