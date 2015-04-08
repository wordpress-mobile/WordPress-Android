package org.wordpress.android.ui.stats;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.models.PostModel;
import org.wordpress.android.ui.stats.models.PublishedPostsAndPagesModel;
import org.wordpress.android.ui.stats.service.StatsService;

import java.util.List;


public class StatsPublishedPostsFragment extends StatsAbstractListFragment {
    public static final String TAG = StatsPublishedPostsFragment.class.getSimpleName();

    @Override
    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (isErrorResponse()) {
            showErrorUI();
            return;
        }

        if (hasPublishedPostsAndPages()) {
            List<PostModel> postViews = getPublishedPostsAndPages();
            ArrayAdapter adapter = new PublishedPostsAndPagesAdapter(getActivity(), postViews);
            StatsUIHelper.reloadLinearLayout(getActivity(), adapter, mList, getMaxNumberOfItemsToShowInList());
            showHideNoResultsUI(false);
            mModuleTitleTextView.setText(postViews.size() + " " + getTitle());
        } else {
            showHideNoResultsUI(true);
        }
    }

    private boolean hasPublishedPostsAndPages() {
        return !isDataEmpty() && ((PublishedPostsAndPagesModel) mDatamodels[0]).hasPublishedPostsAndPages();
    }

    private List<PostModel> getPublishedPostsAndPages() {
        if (!hasPublishedPostsAndPages()) {
            return null;
        }
        return ((PublishedPostsAndPagesModel) mDatamodels[0]).getPublishedPostsAndPages();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // No labels on columns for this module
        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setVisibility(View.GONE);
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setVisibility(View.GONE);
        return view;
    }

    /**
     * This module has a slightly different UI on the top of it.
     *
     * Changes are:
     * - "no-labels" on columns.
     * - Title is created at run-time by using the number of posts published in the timeframe.
     */
    @Override
    protected void showHideNoResultsUI(boolean showNoResultsUI) {
        super.showHideNoResultsUI(showNoResultsUI);
        if (showNoResultsUI) {
            // rewrite here the empty label, since it doesn't include bold text and it's 1 line only.
            String label = "<b>" + getString(R.string.stats_empty_published_posts_desc) + "</b>";
            mEmptyLabel.setText(Html.fromHtml(label));
            mEmptyLabel.setVisibility(View.VISIBLE);
            mModuleTitleTextView.setText(R.string.stats_empty_published_posts_title);
        } else {
            mModuleTitleTextView.setText(getTitle());
        }
    }

    @Override
    protected boolean isViewAllOptionAvailable() {
        return hasPublishedPostsAndPages() && getPublishedPostsAndPages().size() > MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    @Override
    protected boolean isExpandableList() {
        return false;
    }

    @Override
    protected int getEntryLabelResId() {
        return R.string.stats_entry_posts_and_pages;
    }

    @Override
    protected int getTotalsLabelResId() {
        return R.string.stats_totals_views;
    }

    @Override
    protected int getEmptyLabelTitleResId() {
        return R.string.stats_empty_published_posts_title;
    }

    @Override
    protected int getEmptyLabelDescResId() {
        return NO_STRING_ID;
    }

    @Override
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.PUBLISHED_POSTS
        };
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_published);
    }


    private class PublishedPostsAndPagesAdapter extends ArrayAdapter<PostModel> {

        private List<PostModel> list;
        private final LayoutInflater inflater;

        public PublishedPostsAndPagesAdapter(Context context, List<PostModel> list) {
            super(context, R.layout.stats_list_cell, list);
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                rowView = inflater.inflate(R.layout.stats_list_cell, parent, false);
                // configure view holder
                StatsViewHolder viewHolder = new StatsViewHolder(rowView);
                rowView.setTag(viewHolder);
            }

            final PostModel currentRowData = list.get(position);
            StatsViewHolder holder = (StatsViewHolder) rowView.getTag();

            // Entry
            holder.setEntryTextOpenDetailsPage(currentRowData);

            // No "more" button
            holder.imgMore.setVisibility(View.GONE);

            // totals
            holder.totalsTextView.setVisibility(View.GONE);

            // no icon
            holder.networkImageView.setVisibility(View.GONE);
            return rowView;
        }
    }
}
