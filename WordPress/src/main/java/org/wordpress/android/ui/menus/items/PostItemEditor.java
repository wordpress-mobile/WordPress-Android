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
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostUpdateService;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 */
public class PostItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final int mLocalBlogId;
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
        mLocalBlogId = WordPress.getCurrentLocalTableBlogId();
        mAllPostTitles = new ArrayList<>();
        mFilteredPostTitles = new ArrayList<>();
        loadPostList();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        fetchPosts();
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
        mPostListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_post_empty_list));
        mPostListView.setEmptyView(emptyTextView);

        if (mPostListView.getCount() > 0)
            mPostListView.setSelection(0);

        mPostListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mOtherDataDirty = true;
                if (!mItemNameDirty) {
                    if (mItemNameEditText != null) {
                        mItemNameEditText.setText(mFilteredPosts.get(i).getTitle());
                    }
                }
            }
        });

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
        mOtherDataDirty = false;
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
            loadPostList();

            MenuItemModel item = super.getMenuItem();
            if (item != null) {
                //super.getMenuItem() called on purpose to avoid any processing, we just want the working item
                setSelection(item.contentId);
            }
        }
    }

    /**
     * Fetch remote posts for blog
     */
    private void fetchPosts() {
        PostUpdateService.startServiceForBlog(getContext(), mLocalBlogId, false, false);
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
        mAllPostTitles.clear();
        mFilteredPostTitles.clear();
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
        menuItem.typeFamily = MenuItemModel.POST_TYPE_NAME;
        menuItem.typeLabel = MenuItemEditorFactory.ITEM_TYPE.POST.name();

        if (mPostListView.getCount() > 0) {
            int selPos = mPostListView.getCheckedItemPosition();
            if (selPos == -1 ) {
                selPos = 0;
            }
            PostsListPost post = mFilteredPosts.get(selPos);
            if (post != null && post.getRemotePostId() != null) {
                menuItem.contentId = Long.valueOf(post.getRemotePostId());
            }
        }
    }

}
