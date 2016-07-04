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

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.Tag;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class TagItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllTagTitles;
    private final List<String> mFilteredTagTitles;
    private List<Tag> mAllTags;
    private List<Tag> mFilteredTags;

    private RadioButtonListView mTagListView;

    public TagItemEditor(Context context) {
        this(context, null);
    }

    public TagItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mAllTagTitles = new ArrayList<>();
        mFilteredTagTitles = new ArrayList<>();
        loadTags();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        fetchTags();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mTagListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_tag_empty_list));
        mTagListView.setEmptyView(emptyTextView);

        mTagListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mOtherDataDirty = true;
                if (!mItemNameDirty) {
                    if (mItemNameEditText != null) {
                        mItemNameEditText.setText(mFilteredTagTitles.get(i));
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
        for (int i=0; i < mFilteredTags.size(); i++) {
            Tag tagModel = mFilteredTags.get(i);
            long tagId = tagModel.id;
            if (tagId == contentId){
                mTagListView.setSelection(i);
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

    private void fetchTags() {
        final String remoteBlogId = WordPress.getCurrentRemoteBlogId();
        AppLog.d(AppLog.T.API, "TagItemEditor > updating tags for siteId: " + remoteBlogId);
        String path = "/sites/" + remoteBlogId + "/tags";
        WordPress.getRestClientUtils().get(path, new RestRequest.Listener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        if (response == null) {
                            return;
                        }

                        JSONArray jsonTags = response.optJSONArray("tags");
                        List<Tag> tags = Tag.tagListFromJSON(jsonTags, Long.valueOf(remoteBlogId));
                        if (tags != null) {
                            SuggestionTable.insertTagsForSite(Integer.valueOf(remoteBlogId), tags);

                            loadTags();

                            MenuItemModel item = TagItemEditor.super.getMenuItem();
                            if (item != null) {
                                //super.getMenuItem() called on purpose to avoid any processing, we just want the working item
                                setSelection(item.contentId);
                            }

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
        if (mTagListView == null) return;
        refreshFilteredTags(filter);
        refreshAdapter();
    }

    private void refreshFilteredTags(String filter) {
        mFilteredTagTitles.clear();
        mFilteredTags.clear();
        String upperFiler = filter.toUpperCase();
        for (int i = 0; i < mAllTagTitles.size(); i++) {
            String s = mAllTagTitles.get(i);
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredTagTitles.add(s);
                mFilteredTags.add(mAllTags.get(i));
            }
        }
    }

    private void refreshAdapter() {
        if (mTagListView != null) {
            mTagListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mTagListView.getContext(), mFilteredTagTitles));
        }
    }

    private void loadTags() {
        mAllTags = SuggestionTable.getTagsForSite(Integer.valueOf(WordPress.getCurrentRemoteBlogId()));
        mFilteredTags = new ArrayList<>();
        mAllTagTitles.clear();
        mFilteredTagTitles.clear();
        for (Tag tag : mAllTags) {
            mFilteredTags.add(tag);
            mAllTagTitles.add(tag.getTag());
            mFilteredTagTitles.add(tag.getTag());
        }
        refreshAdapter();
    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        //check selected item in array and set selected
        menuItem.type = MenuItemModel.TAG_TYPE_NAME;
        menuItem.typeFamily = "taxonomy";
        menuItem.typeLabel = "Tag";

        // no list item is checked when creating a new tag item
        int selectedIndex = mTagListView.getCheckedItemPosition();
        if (selectedIndex < 0) return;

        Tag tag = mFilteredTags.get(mTagListView.getCheckedItemPosition());
        if (tag != null && tag.id > 0) {
            menuItem.contentId = tag.id;
        }
    }
}
