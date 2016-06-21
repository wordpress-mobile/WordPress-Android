package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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
public class PostItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllPosts;
    private final List<String> mFilteredPosts;

    private RadioButtonListView mPostListView;

    public PostItemEditor(Context context) {
        this(context, null);
    }

    public PostItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mAllPosts = new ArrayList<>();
        mFilteredPosts = new ArrayList<>();
        loadPostList();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mPostListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.post_menu_item_edit_view;
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

    private void filterAdapter(String filter) {
        if (mPostListView == null) return;
        refreshFilteredPosts(filter);
        refreshAdapter();
    }

    private void refreshFilteredPosts(String filter) {
        mFilteredPosts.clear();
        String upperFiler = filter.toUpperCase();
        for (String s : mAllPosts) {
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredPosts.add(s);
            }
        }
    }

    private void refreshAdapter() {
        if (mPostListView != null) {
            mPostListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mPostListView.getContext(), mFilteredPosts));
        }
    }

    private void loadPostList() {
        PostsListPostList posts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), false);
        for (PostsListPost post : posts) {
            mAllPosts.add(post.getTitle());
            mFilteredPosts.add(post.getTitle());
        }
        refreshAdapter();
    }
}
