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
public class PostItemEditor extends BaseMenuItemEditor
        implements SearchView.OnQueryTextListener {
    private final List<String> mPosts;

    private SearchView mSearchView;
    private RadioButtonListView mPageListView;

    public PostItemEditor(Context context) {
        super(context);
        mPosts = new ArrayList<>();
        loadPostList();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        if (child.getId() == R.id.page_editor_search_view) {
            mSearchView = (SearchView) child;
            mSearchView.setOnQueryTextListener(this);
        } else if (child.getId() == R.id.page_editor_page_list) {
            mPageListView = (RadioButtonListView) child;
            refreshAdapter();
        }
    }

    @Override
    public int getLayoutRes() {
        return R.layout.post_menu_item_edit_view;
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
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = new MenuItemModel();
        return menuItem;
    }

    @Override
    public void onSave() {
    }

    @Override
    public void onDelete() {
    }

    private void loadPostList() {
        PostsListPostList posts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), false);
        for (PostsListPost post : posts) {
            mPosts.add(post.getTitle());
        }
    }

    private void refreshAdapter() {
        if (mPageListView != null) {
            mPageListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mPageListView.getContext(), mPosts));
        }
    }
}
