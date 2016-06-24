package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SearchView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class PostItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllPostTitles;
    private final List<String> mFilteredPostTitles;
    private PostsListPostList mAllPosts;
    private PostsListPostList mFilteredPosts;

    private RadioButtonListView mPostListView;

    public PostItemEditor(Context context) {
        this(context, null);
    }

    public PostItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mAllPostTitles = new ArrayList<>();
        mFilteredPostTitles = new ArrayList<>();
        loadPostList();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mPostListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_post_empty_list));
        mPostListView.setEmptyView(emptyTextView);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.post_menu_item_edit_view;
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
        for (int i=0; i < mFilteredPosts.size(); i++) {
            PostsListPost post = mFilteredPosts.get(i);
            String remoteId = post.getRemotePostId();
            if (remoteId != null && Long.valueOf(remoteId) == contentId){
                mPostListView.setSelection(i);
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
        mFilteredPostTitles.clear();
        mFilteredPosts.clear();
        String upperFiler = filter.toUpperCase();
        for (int i = 0; i < mAllPostTitles.size(); i++) {
            String s = mAllPostTitles.get(i);
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredPostTitles.add(s);
                mFilteredPosts.add(mAllPosts.get(i));
            }
        }
    }

    private void refreshAdapter() {
        if (mPostListView != null) {
            mPostListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mPostListView.getContext(), mFilteredPostTitles));
        }
    }

    private void loadPostList() {
        mAllPosts = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), false);
        mFilteredPosts = new PostsListPostList();
        for (PostsListPost post : mAllPosts) {
            mFilteredPosts.add(post);
            mAllPostTitles.add(post.getTitle());
            mFilteredPostTitles.add(post.getTitle());
        }
        refreshAdapter();
    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        //check selected item in array and set selected
        menuItem.type = MenuItemEditorFactory.ITEM_TYPE.POST.name().toLowerCase(); //default type: POST
        menuItem.typeFamily = "post_type";
        menuItem.typeLabel = MenuItemEditorFactory.ITEM_TYPE.POST.name();

        PostsListPost post = mFilteredPosts.get(mPostListView.getCheckedItemPosition());
        if (post != null && post.getRemotePostId() != null) {
            menuItem.contentId = Long.valueOf(post.getRemotePostId());
        }
    }

}
