package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.view.View;
import android.widget.SearchView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.widgets.RadioButtonListView;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class PageItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllPages;
    private final List<String> mFilteredPages;

    private RadioButtonListView mPageListView;

    public PageItemEditor(Context context) {
        super(context);
        mAllPages = new ArrayList<>();
        mFilteredPages = new ArrayList<>();
        loadPageList();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mPageListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.page_menu_item_edit_view;
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = new MenuItemModel();
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

    private void loadPageList() {
        PostsListPostList posts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), true);
        for (PostsListPost post : posts) {
            mAllPages.add(post.getTitle());
            mFilteredPages.add(post.getTitle());
        }
        refreshAdapter();
    }

    private void filterAdapter(String filter) {
        if (mPageListView == null) return;
        refreshFilteredPages(filter);
        refreshAdapter();
    }

    private void refreshAdapter() {
        if (mPageListView != null) {
            mPageListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mPageListView.getContext(), mFilteredPages));
        }
    }

    private void refreshFilteredPages(String filter) {
        mFilteredPages.clear();
        String upperFiler = filter.toUpperCase();
        for (String s : mAllPages) {
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredPages.add(s);
            }
        }
    }
}
