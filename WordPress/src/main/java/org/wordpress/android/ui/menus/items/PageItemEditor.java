package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.view.View;
import android.view.ViewStub;
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
    public static final String PAGE_LIST_KEY = "page-list";

    private final List<String> mPages;

    private SearchView mSearchView;
    private RadioButtonListView mPageListView;

    public PageItemEditor(Context context) {
        super(context);
        mPages = new ArrayList<>();
        loadPageList();
    }

    @Override
    public void onInflate(ViewStub stub, View inflated) {
        mSearchView = (SearchView) inflated.findViewById(R.id.page_editor_search_view);
        mPageListView = (RadioButtonListView) inflated.findViewById(R.id.page_editor_page_list);

        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(this);
        }
        if (mPageListView != null) {
            mPageListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mPageListView.getContext(), mPages));
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.page_menu_item_edit_view;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public int getIconDrawable() {
        return R.drawable.my_site_icon_pages;
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

    private void loadPageList() {
        PostsListPostList posts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), true);
        for (PostsListPost post : posts) {
            mPages.add(post.getTitle());
        }
    }
}
