package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SearchView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.Post;
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
    private final List<String> mAllPageTitles;
    private final List<String> mFilteredPageTitles;
    private PostsListPostList mAllPages;
    private PostsListPostList mFilteredPages;

    private RadioButtonListView mPageListView;

    public PageItemEditor(Context context) {
        this(context, null);
    }

    public PageItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mLocalBlogId = WordPress.getCurrentLocalTableBlogId();
        mAllPageTitles = new ArrayList<>();
        mFilteredPageTitles = new ArrayList<>();
        loadPages();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
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

        mPageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mOtherDataDirty = true;
                if (!mItemNameDirty) {
                    if (mItemNameEditText != null) {
                        mItemNameEditText.setText(mFilteredPages.get(i).getTitle());
                    }
                }
            }
        });
    }

    @Override
    public int getLayoutRes() {
        return R.layout.page_menu_item_edit_view;
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
        boolean selectionMarked = false;
        for (int i=0; i < mFilteredPages.size(); i++) {
            PostsListPost post = mFilteredPages.get(i);
            String remoteId = post.getRemotePostId();
            if (remoteId != null && (!TextUtils.isEmpty(remoteId)) && Long.valueOf(remoteId) == contentId){
                mPageListView.setSelection(i);
                selectionMarked = true;
                break;
            }
        }

        if (!selectionMarked) {
            //check if home
            MenuItemModel item = super.getMenuItem();
            if (item != null && item.url != null) {
                if (item.url.compareToIgnoreCase(WordPress.getCurrentBlog().getHomeURL() + "/") == 0) {
                    mPageListView.setSelection(0);
                }
            }
        }
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = super.getMenuItem();
        fillData(menuItem);
        return menuItem;
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

            MenuItemModel item = super.getMenuItem();
            if (item != null) {
                //super.getMenuItem() called on purpose to avoid any processing, we just want the working item
                setSelection(item.contentId);
            }
        }
    }

    /**
     * Loads locally stored pages
     */
    private void loadPages() {
        mAllPages = WordPress.wpDB.getPostsListPosts(WordPress.getCurrentLocalTableBlogId(), true);
        insertHomePage();
        mFilteredPages = new PostsListPostList();
        mAllPageTitles.clear();
        mFilteredPageTitles.clear();
        for (PostsListPost post : mAllPages) {
            mFilteredPages.add(post);
            mAllPageTitles.add(post.getTitle());
            mFilteredPageTitles.add(post.getTitle());
        }
        refreshAdapter();
    }

    private void insertHomePage(){
        Post homePost = new Post(WordPress.getCurrentLocalTableBlogId(), true);
        homePost.setTitle(WordPress.getContext().getString(R.string.menu_item_special_home));
        PostsListPost homePage = new PostsListPost(homePost);
        mAllPages.add(0, homePage);
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
            mPageListView.setAdapter(new RadioButtonListAdapter(getContext(), mFilteredPageTitles));
        }
    }

    private void refreshFilteredPages(String filter) {
        mFilteredPageTitles.clear();
        mFilteredPages.clear();
        String upperFiler = filter.toUpperCase();
        for (int i = 0; i < mAllPageTitles.size(); i++) {
            String s = mAllPageTitles.get(i);
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredPageTitles.add(s);
                mFilteredPages.add(mAllPages.get(i));
            }
        }
    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        //check selected item in array and set selected
        PostsListPost post = mFilteredPages.get(mPageListView.getCheckedItemPosition());
        if (post != null) {

            if (post.getRemotePostId() != null && !TextUtils.isEmpty(post.getRemotePostId())) {
                menuItem.contentId = Long.valueOf(post.getRemotePostId());

                menuItem.type = MenuItemEditorFactory.ITEM_TYPE.PAGE.name().toLowerCase(); //default type: PAGE
                menuItem.typeFamily = MenuItemModel.POST_TYPE_NAME;
                menuItem.typeLabel = MenuItemEditorFactory.ITEM_TYPE.PAGE.name();
            } else {
                // this is the HOME item
                /*
                    "type": "custom",
                    "type_family": "custom",
                    "type_label": "Custom Link",
                    * */
                menuItem.type = MenuItemEditorFactory.ITEM_TYPE.CUSTOM.name().toLowerCase(); //default type: CUSTOM
                menuItem.typeFamily = "custom";
                menuItem.typeLabel = "Custom Link";
                menuItem.url = WordPress.getCurrentBlog().getHomeURL() + "/";
            }
        }
    }

}
