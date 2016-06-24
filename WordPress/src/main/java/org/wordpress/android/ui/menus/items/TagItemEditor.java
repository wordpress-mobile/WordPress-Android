package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SearchView;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class TagItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllTags;
    private final List<String> mFilteredTags;

    private RadioButtonListView mTagListView;

    public TagItemEditor(Context context) {
        this(context, null);
    }

    public TagItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mAllTags = new ArrayList<>();
        mFilteredTags = new ArrayList<>();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mTagListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_tag_empty_list));
        mTagListView.setEmptyView(emptyTextView);
        loadTags();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.tag_menu_item_edit_view;
    }

    @Override
    public int getNameEditTextRes() {
        return R.id.menu_item_title_edit;
    }

    @Override
    public void onSave() {
    }

    @Override
    public void onDelete() {
    }

    //
    // SearchView query callbacks
    //
    @Override
    public boolean onQueryTextSubmit(String query) {
        filterAdapter(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterAdapter(newText);
        return true;
    }

    private void loadTags() {
    }

    private void fetchTags() {
    }

    private void filterAdapter(String filter) {
    }

    private void refreshAdapter() {
    }
}
