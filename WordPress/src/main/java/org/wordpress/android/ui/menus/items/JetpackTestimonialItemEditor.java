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
import org.wordpress.android.models.Tag;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.RadioButtonListView;
import org.wordpress.android.widgets.WPTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class JetpackTestimonialItemEditor extends BaseMenuItemEditor implements SearchView.OnQueryTextListener {
    private final List<String> mAllTestimonialTitles;
    private final List<String> mFilteredTestimonialTitles;
    private ReaderPostList mAllTestimonials;
    private ReaderPostList mFilteredTestimonials;
    private boolean mSetSelectionOnceDataLoaded = true;

    private RadioButtonListView mTestimonialListView;

    public JetpackTestimonialItemEditor(Context context) {
        this(context, null);
    }

    public JetpackTestimonialItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mAllTestimonialTitles = new ArrayList<>();
        mFilteredTestimonialTitles = new ArrayList<>();
        mAllTestimonials = new ReaderPostList();
        mFilteredTestimonials = new ReaderPostList();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        fetchTestimonials();
    }


    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        ((SearchView) child.findViewById(R.id.single_select_search_view)).setOnQueryTextListener(this);
        mTestimonialListView = (RadioButtonListView) child.findViewById(R.id.single_select_list_view);

        WPTextView emptyTextView = (WPTextView) child.findViewById(R.id.empty_list_view);
        emptyTextView.setText(getContext().getString(R.string.menu_item_type_testimonial_empty_list));
        mTestimonialListView.setEmptyView(emptyTextView);

        mTestimonialListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mOtherDataDirty = true;
                if (!mItemNameDirty) {
                    if (mItemNameEditText != null) {
                        mItemNameEditText.setText(mFilteredTestimonialTitles.get(i));
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
        if (mFilteredTestimonials != null && mFilteredTestimonials.size() > 0) {
            for (int i=0; i < mFilteredTestimonials.size(); i++) {
                ReaderPost testimonial = mFilteredTestimonials.get(i);
                long testimonialId = testimonial.postId;
                if (testimonialId == contentId){
                    mTestimonialListView.setSelection(i);
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

    private void fetchTestimonials() {
        final String remoteBlogId = WordPress.getCurrentRemoteBlogId();
        AppLog.d(AppLog.T.API, "JetpackTestimonialsItemEditor > updating testimonials for siteId: " + remoteBlogId);
        String path = "/sites/" + remoteBlogId + "/posts/";
        Map<String, String> params = new HashMap<>();
        params.put("type", "jetpack-testimonial");

        WordPress.getRestClientUtilsV1_1().get(path, params, null, new RestRequest.Listener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        if (response == null) {
                            return;
                        }

                        ReaderPostList serverPosts = ReaderPostList.fromJson(response);
                        loadTestimonials(serverPosts);
                        MenuItemModel item = JetpackTestimonialItemEditor.super.getMenuItem();
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
        if (mTestimonialListView == null) return;
        refreshFilteredTags(filter);
        refreshAdapter();
    }

    private void refreshFilteredTags(String filter) {
        mFilteredTestimonialTitles.clear();
        mFilteredTestimonials.clear();
        String upperFiler = filter.toUpperCase();
        for (int i = 0; i < mAllTestimonialTitles.size(); i++) {
            String s = mAllTestimonialTitles.get(i);
            if (s.toUpperCase().contains(upperFiler)) {
                mFilteredTestimonialTitles.add(s);
                mFilteredTestimonials.add(mAllTestimonials.get(i));
            }
        }
    }

    private void refreshAdapter() {
        if (mTestimonialListView != null) {
            mTestimonialListView.setAdapter(new RadioButtonListView.RadioButtonListAdapter(mTestimonialListView.getContext(), mFilteredTestimonialTitles));
        }
    }

    private void loadTestimonials(ReaderPostList testimonials) {
        mAllTestimonials = testimonials;
        mFilteredTestimonials = new ReaderPostList();
        mAllTestimonialTitles.clear();
        mFilteredTestimonialTitles.clear();
        for (ReaderPost testimonial : mAllTestimonials) {
            mFilteredTestimonials.add(testimonial);
            mAllTestimonialTitles.add(testimonial.getTitle());
            mFilteredTestimonialTitles.add(testimonial.getTitle());
        }
        refreshAdapter();

        if (mSetSelectionOnceDataLoaded && mWorkingItem != null) {
            setSelection(mWorkingItem.contentId);
        }

    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        //check selected item in array and set selected
        menuItem.type = "jetpack-testimonial";
        menuItem.typeFamily = "post_type";
        menuItem.typeLabel = "Testimonial";

        ReaderPost testimonial = mFilteredTestimonials.get(mTestimonialListView.getCheckedItemPosition());
        if (testimonial != null && testimonial.postId > 0) {
            menuItem.contentId = testimonial.postId;
        }
    }

}
