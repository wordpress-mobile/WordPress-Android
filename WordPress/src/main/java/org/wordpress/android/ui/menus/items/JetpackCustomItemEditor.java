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
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class JetpackCustomItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllJetpackCustomTitles;
    private final List<String> mFilteredJetpackCustomTitles;
    private ReaderPostList mAllJetpackCustomPosts;
    private ReaderPostList mFilteredJetpackCustomPosts;

    private RadioButtonListView mJetpackCustomPostsListView;

    private MenuItemEditorFactory.ITEM_TYPE mJetpackCustomType;

    public JetpackCustomItemEditor(Context context) {
        this(context, null);
    }

    public JetpackCustomItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mAllJetpackCustomTitles = new ArrayList<>();
        mFilteredJetpackCustomTitles = new ArrayList<>();
        mAllJetpackCustomPosts = new ReaderPostList();
        mFilteredJetpackCustomPosts = new ReaderPostList();
    }

    public void setJetpackCustomType(MenuItemEditorFactory.ITEM_TYPE itemType) {
        mJetpackCustomType = itemType;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        fetchJetpackCustomPosts();
    }


    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mJetpackCustomPostsListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_custom_empty_list));
        mJetpackCustomPostsListView.setEmptyView(emptyTextView);

        mJetpackCustomPostsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mOtherDataDirty = true;
                if (!mItemNameDirty) {
                    if (mItemNameEditText != null) {
                        mItemNameEditText.setText(mFilteredJetpackCustomTitles.get(i));
                    }
                }
            }
        });

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
    public void setMenuItem(MenuItemModel menuItem) {
        super.setMenuItem(menuItem);
        if (!TextUtils.isEmpty(menuItem.name)) {
            setSelection(menuItem.contentId);
        }
    }

    private void setSelection(long contentId) {
        mOtherDataDirty = false;
        if (mFilteredJetpackCustomPosts != null && mFilteredJetpackCustomPosts.size() > 0) {
            for (int i=0; i < mFilteredJetpackCustomPosts.size(); i++) {
                ReaderPost customPost = mFilteredJetpackCustomPosts.get(i);
                if (customPost.postId == contentId){
                    mJetpackCustomPostsListView.setSelection(i);
                    break;
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

    private void fetchJetpackCustomPosts() {
        final String remoteBlogId = WordPress.getCurrentRemoteBlogId();
        AppLog.d(AppLog.T.API, "JetpackCustomItemEditor > updating testimonials for siteId: " + remoteBlogId);
        String path = "/sites/" + remoteBlogId + "/posts/";
        Map<String, String> params = new HashMap<>();
        switch(mJetpackCustomType) {
            case JETPACK_PORTFOLIO:
                params.put("type", MenuItemModel.PORTFOLIO_TYPE_NAME);
                break;
            case JETPACK_COMIC:
                params.put("type", MenuItemModel.COMIC_TYPE_NAME);
                break;
            case JETPACK_TESTIMONIAL:
            default:
                params.put("type", MenuItemModel.TESTIMONIAL_TYPE_NAME);
                break;
        }

        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        if (response == null) {
                            return;
                        }

                        ReaderPostList serverPosts = ReaderPostList.fromJson(response);
                        loadJetpackCustomPosts(serverPosts);
                        MenuItemModel item = JetpackCustomItemEditor.super.getMenuItem();
                        if (item != null) {
                            //super.getMenuItem() called on purpose to avoid any processing, we just want the working item
                            setSelection(item.contentId);
                        }
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.e(AppLog.T.API, error);
                    }
                });
    }

    private void filterAdapter(String filter) {
        if (mJetpackCustomPostsListView == null) return;
        refreshFilteredTags(filter);
        refreshAdapter();
    }

    private void refreshFilteredTags(String filter) {
        mFilteredJetpackCustomTitles.clear();
        mFilteredJetpackCustomPosts.clear();
        String upperFiler = filter.toUpperCase();
        for (int i = 0; i < mAllJetpackCustomTitles.size(); i++) {
            String s = mAllJetpackCustomTitles.get(i);
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredJetpackCustomTitles.add(s);
                mFilteredJetpackCustomPosts.add(mAllJetpackCustomPosts.get(i));
            }
        }
    }

    private void refreshAdapter() {
        if (mJetpackCustomPostsListView != null) {
            mJetpackCustomPostsListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mJetpackCustomPostsListView.getContext(), mFilteredJetpackCustomTitles));
        }
    }

    private void loadJetpackCustomPosts(ReaderPostList customPosts) {
        mAllJetpackCustomPosts = customPosts;
        mFilteredJetpackCustomPosts = new ReaderPostList();
        mAllJetpackCustomTitles.clear();
        mFilteredJetpackCustomTitles.clear();
        for (ReaderPost customPost : mAllJetpackCustomPosts) {
            mFilteredJetpackCustomPosts.add(customPost);
            mAllJetpackCustomTitles.add(customPost.getTitle());
            mFilteredJetpackCustomTitles.add(customPost.getTitle());
        }
        refreshAdapter();

        if (mWorkingItem != null) {
            setSelection(mWorkingItem.contentId);
        }

    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        //check selected item in array and set selected
        switch (mJetpackCustomType) {
            case JETPACK_PORTFOLIO:
                menuItem.type = MenuItemModel.PORTFOLIO_TYPE_NAME;
                menuItem.typeFamily = MenuItemModel.POST_TYPE_NAME;
                menuItem.typeLabel = "Portfolio";
                break;
            case JETPACK_COMIC:
                menuItem.type = MenuItemModel.COMIC_TYPE_NAME;
                menuItem.typeFamily = MenuItemModel.POST_TYPE_NAME;
                menuItem.typeLabel = "Comic";
                break;
            case JETPACK_TESTIMONIAL:
            default:
                menuItem.type = MenuItemModel.TESTIMONIAL_TYPE_NAME;
                menuItem.typeFamily = MenuItemModel.POST_TYPE_NAME;
                menuItem.typeLabel = "Testimonial";
                break;
        }

        ReaderPost customPosts = mFilteredJetpackCustomPosts.get(mJetpackCustomPostsListView.getCheckedItemPosition());
        if (customPosts != null && customPosts.postId > 0) {
            menuItem.contentId = customPosts.postId;
        }
    }

}
