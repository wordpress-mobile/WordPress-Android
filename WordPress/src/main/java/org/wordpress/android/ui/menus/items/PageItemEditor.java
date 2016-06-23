package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SearchView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.MenuItemTable;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostUpdateService;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.RadioButtonListView.RadioButtonListAdapter;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 */
public class PageItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final int mLocalBlogId;
    private final List<String> mAllPages;
    private final List<String> mFilteredPages;

    private RadioButtonListView mPageListView;

    public PageItemEditor(Context context) {
        this(context, null);
    }

    public PageItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mLocalBlogId = WordPress.getCurrentLocalTableBlogId();
        mAllPages = new ArrayList<>();
        mFilteredPages = new ArrayList<>();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        loadPages();
        fetchPages();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mPageListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_page_empty_list));
        mPageListView.setEmptyView(emptyTextView);

        mPageListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mWorkingItem.name = mPageListView.getSelectedItem().toString();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public int getLayoutRes() {
        return R.layout.page_menu_item_edit_view;
    }

    @Override
    public void setMenuItem(MenuItemModel menuItem) {
        super.setMenuItem(menuItem);
        if (!TextUtils.isEmpty(menuItem.name)) {
            setSelection(menuItem.name);
        }
    }

    private void setSelection(String name) {
        int selectedPosition = mFilteredPages.indexOf(name);
        if (selectedPosition != -1 && selectedPosition < mPageListView.getCount()) {
            mPageListView.setSelection(selectedPosition);
        }
    }

    public boolean shouldEdit() {
        return false;
    }

    @Override
    public void onSave() {
        if (getMenuItem() != null && shouldEdit()) MenuItemTable.saveMenuItem(getMenuItem());
    }

    @Override
    public void onDelete() {
        if (getMenuItem() != null && shouldEdit()) MenuItemTable.deleteMenuItem(getMenuItem().itemId);
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

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.RequestPosts event) {
        // update page list with changes from remote
        if (event.getBlogId() == mLocalBlogId) {
            loadPages();
        }
    }

    /**
     * Loads locally stored pages
     */
    private void loadPages() {
        PostsListPostList posts = WordPress.wpDB.getPostsListPosts(mLocalBlogId, true);
        mAllPages.clear();
        mFilteredPages.clear();
        for (PostsListPost post : posts) {
            mAllPages.add(post.getTitle());
            mFilteredPages.add(post.getTitle());
        }
        refreshAdapter();
    }

    /**
     * Fetch remote pages for blog
     */
    private void fetchPages() {
        PostUpdateService.startServiceForBlog(getContext(), mLocalBlogId, true, false);
    }

    private void filterAdapter(String filter) {
        if (mPageListView == null) return;
        refreshFilteredPages(filter);
        refreshAdapter();
    }

    private void refreshAdapter() {
        if (mPageListView != null) {
            mPageListView.setAdapter(new RadioButtonListAdapter(getContext(), mFilteredPages));
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
